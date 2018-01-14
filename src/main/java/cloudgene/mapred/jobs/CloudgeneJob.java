package cloudgene.mapred.jobs;

import genepi.hadoop.HdfsUtil;
import genepi.hadoop.importer.FileItem;
import genepi.hadoop.io.HdfsLineWriter;
import genepi.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.engine.Executor;
import cloudgene.mapred.jobs.engine.Planner;
import cloudgene.mapred.jobs.engine.graph.Graph;
import cloudgene.mapred.jobs.engine.graph.GraphNode;
import cloudgene.mapred.util.Application;
import cloudgene.mapred.util.ApplicationInstaller;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlParameterInput;
import cloudgene.mapred.wdl.WdlParameterOutput;
import cloudgene.mapred.wdl.WdlParameterOutputType;
import cloudgene.mapred.wdl.WdlStep;

public class CloudgeneJob extends AbstractJob {

	private WdlApp app;

	private String workingDirectory;

	private Executor executor;

	public static final int MAX_DOWNLOAD = 10;

	private static final Log log = LogFactory.getLog(CloudgeneJob.class);

	public CloudgeneJob() {
		super();
	}

	public void loadConfig(WdlApp app) {

		this.app = app;
		workingDirectory = app.getPath();

		// set parameter properties that are not stored in database.
		// needed for restart
		for (CloudgeneParameterOutput outputParam : outputParams) {

			for (WdlParameterOutput output : app.getWorkflow().getOutputs()) {

				if (outputParam.getName().equals(output.getId())) {
					outputParam.setMakeAbsolute(output.isMakeAbsolute());
					outputParam.setAutoExport(output.isAutoExport());
					outputParam.setZip(output.isZip());
					outputParam.setMergeOutput(output.isMergeOutput());
					outputParam.setRemoveHeader(output.isRemoveHeader());
				}

			}

		}

	}

	public CloudgeneJob(User user, String id, WdlApp app, Map<String, String> params) {
		setComplete(false);
		this.app = app;
		setId(id);
		setUser(user);
		workingDirectory = app.getPath();

		// init parameters
		inputParams = new Vector<CloudgeneParameterInput>();
		for (WdlParameterInput input : app.getWorkflow().getInputs()) {
			CloudgeneParameterInput newInput = new CloudgeneParameterInput(input);
			newInput.setJob(this);

			if (params.containsKey(input.getId())) {
				newInput.setValue(params.get(input.getId()));
			}

			inputParams.add(newInput);
		}

		outputParams = new Vector<CloudgeneParameterOutput>();
		for (WdlParameterOutput output : app.getWorkflow().getOutputs()) {
			CloudgeneParameterOutput newOutput = new CloudgeneParameterOutput(output);
			newOutput.setJob(this);
			outputParams.add(newOutput);
		}

	}

	@Override
	public boolean setup() {

		context = new CloudgeneContext(this);
		// context.updateInputParameters();
		context.setupOutputParameters(app.getWorkflow().hasHdfsOutputs());

		return true;

	}

	@Override
	public boolean hasSteps() {
		try {
			// evaluate WDL derictives
			Planner planner = new Planner();
			WdlApp app = planner.evaluateWDL(this.app, context, getSettings());
			return app.getWorkflow().getSteps().size() > 0;
		} catch (Exception e) {
			// e.printStackTrace();
			writeOutput(e.getMessage());
			setError(e.getMessage());
			return false;
		}
	}

	@Override
	public boolean execute() {

		try {

			log.info("Job " + getId() + " submitted...");

			// evaluate WDL derictives
			Planner planner = new Planner();
			WdlApp app = planner.evaluateWDL(this.app, context, getSettings());

			// create dag from wdl document
			Graph graph = planner.buildDAG(app.getWorkflow().getSteps(), app.getWorkflow(), context);

			// execute optimzed dag
			executor = new Executor();
			boolean successful = executor.execute(graph);

			if (!successful) {
				setError("Job Execution failed.");
				GraphNode failedNode = executor.getCurrentNode();
				executeFailureStep(failedNode.getStep());
				return false;
			}

			setError(null);
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			writeOutput(e.getMessage());
			setError(e.getMessage());
			return false;
		}

	}

	@Override
	public boolean executeSetupSteps() {

		try {
			// evaluate WDL
			Planner planner = new Planner();
			WdlApp app = planner.evaluateWDL(this.app, context, getSettings());

			// if a single setup step is set, add it to setups
			WdlStep setup = app.getWorkflow().getSetup();
			if (setup != null) {
				app.getWorkflow().getSetups().add(0, setup);
			}

			Graph graph = planner.buildDAG(app.getWorkflow().getSetups(), app.getWorkflow(), context);

			// execute optimized DAG
			executor = new Executor();
			boolean successful = executor.execute(graph);

			if (!successful) {
				setError("Job Execution failed.");
				GraphNode failedNode = executor.getCurrentNode();
				executeFailureStep(failedNode.getStep());
				return false;
			}

			setError(null);
			return true;
		} catch (Exception e) {
			// e.printStackTrace();
			writeOutput(e.getMessage());
			setError(e.getMessage());
			return false;
		}

	}

	@Override
	public boolean executeInstallation(boolean forceInstallation) {

		try {

			Settings setttings = getSettings();

			// find dependencies
			List<WdlApp> applications = new Vector<>();

			// install application
			applications.add(app);

			for (CloudgeneParameterInput input : getInputParams()) {
				String value = input.getValue();
				if (value.startsWith("apps@")) {
					String appId = value.replaceAll("apps@", "");
					Application app2 = setttings.getAppByIdAndUser(appId, getUser());
					if (app2 != null) {
						applications.add(app2.getWdlApp());
						// update evenirnoment variables
						HashMap<String, String> env = setttings.getEnvironment(app2.getWdlApp());
						Map<String, String> properties = app2.getWdlApp().getProperties();
						for (String property : properties.keySet()) {
							String value2 = properties.get(property);
							properties.put(property, ApplicationInstaller.env(value2, env));
							System.out.println(ApplicationInstaller.env(value2, env));

						}
						getContext().setData(input.getName(), properties);
					} else {
						writeOutput("Application " + appId + " is not installed.");
						setError("Application " + appId + " is not installed.");
						return false;
					}
				}
			}

			for (WdlApp app : applications) {

				log.info("Job " + getId() + ": executing installation for " + app.getId() + "...");

				if (app.getInstallation() != null && app.getInstallation().size() > 0) {

					writeLog("  Preparing application " + app.getId() + "...");
					Map<String, String> env = setttings.getEnvironment(app);
					String target = env.get("hdfs_app_folder");

					String installationFile = HdfsUtil.path(target, "installed");
					boolean installed = HdfsUtil.exists(installationFile);

					if (!installed || forceInstallation) {
						try {

							// context.beginTask("Installing application " +
							// app.getId() + "...");

							HdfsUtil.delete(target);
							writeLog("  Installing Application...");
							ApplicationInstaller.runCommands(app.getInstallation(), env);

							HdfsLineWriter lineWriter = new HdfsLineWriter(installationFile);
							lineWriter.write(System.currentTimeMillis() + "");
							lineWriter.close();

							log.info("Installation of application " + app.getId() + " finished.");
							// context.endTask("Installation of application " +
							// app.getId() + " finished.",
							// WorkflowContext.OK);
							writeLog("  Installation finished.");
						} catch (IOException e) {
							// context.endTask("Installing application " +
							// app.getId() + "failed.", WorkflowContext.ERROR);

							writeOutput("Installation of application " + app.getId() + " failed.");
							writeOutput(e.getMessage());
							setError(e.getMessage());
							return false;
						}
					} else {
						writeLog("  Application is already installed.");
					}
				}

			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			writeOutput("Installation of application " + getApplicationId() + " failed.");
			writeOutput(e.getMessage());
			setError(e.getMessage());
			return false;
		}
	}

	@Override
	public boolean before() {

		return true;

	}

	@Override
	public boolean onFailure() {

		cleanUp();

		return true;
	}

	public boolean executeFailureStep(WdlStep failedStep) {

		WdlStep step = app.getWorkflow().getOnFailure();

		if (step != null) {
			try {
				writeLog("Executing onFailure... ");
				GraphNode node = new GraphNode(step, context);
				context.setData("cloudgene.failedStep", failedStep);
				context.setData("cloudgene.failedStep.classname", failedStep.getClassname());
				node.run();
				boolean result = node.isSuccessful();
				if (result) {

					// export parameters generated by onFailure step
					for (CloudgeneParameterOutput out : getOutputParams()) {
						if (out.isAutoExport() && step.getGenerates().contains(out.getName())) {
							log.info("Export parameter '" + out.getName() + "'...");
							context.println("Export parameter '" + out.getName() + "'...");
							exportParameter(out);
						}
					}

					writeLog("Executed onFailure successful.");
					return true;
				} else {
					writeLog("onFailure execution failed.");
					return false;
				}

			} catch (Exception e) {
				writeLog("onFailure execution failed.");
				writeLog(e.getMessage());
				setError(e.getMessage());
				return false;
			}

		}

		cleanUp();

		return true;
	}

	@Override
	public boolean cleanUp() {

		// delete local temp folders

		writeLog("Cleaning up uploaded local files...");
		FileUtil.deleteDirectory(context.getLocalInput());

		writeLog("Cleaning up temporary local files...");
		FileUtil.deleteDirectory(context.getLocalTemp());

		if (app.getWorkflow().hasHdfsOutputs()) {

			try {
				// delete hdfs temp folders
				writeLog("Cleaning up temporary hdfs files...");
				HdfsUtil.delete(context.getHdfsTemp());

				// delete hdfs workspace
				if (isRemoveHdfsWorkspace()) {
					writeLog("Cleaning up hdfs files...");
					HdfsUtil.delete(context.getHdfsOutput());
					HdfsUtil.delete(context.getHdfsInput());
				}
			} catch (Exception e) {
				System.out.println("Warning: problems during hdfs cleanup.");
			}

		}

		return true;
	}

	@Override
	public boolean after() {

		// create output zip file for hdfs folders
		for (CloudgeneParameterOutput out : getOutputParams()) {

			if (out.isDownload() && !out.isAutoExport()) {
				// export to local folder for faster download
				exportParameter(out);
			}

			writeLog("Exporting data successful.");

		}

		return true;
	}

	public boolean exportParameter(CloudgeneParameterOutput out) {

		String localOutput = context.getLocalOutput();
		String workspace = getHdfsWorkspace();

		if (out.getType() == WdlParameterOutputType.HDFS_FOLDER) {

			String localOutputDirectory = FileUtil.path(localOutput, out.getName());

			FileUtil.createDirectory(localOutputDirectory);

			String filename = context.getOutput(out.getName());
			String hdfsPath = null;
			if (filename.startsWith("hdfs://") || filename.startsWith("file:/")) {
				hdfsPath = filename;

			} else {

				hdfsPath = HdfsUtil.makeAbsolute(HdfsUtil.path(workspace, filename));
			}

			if (HdfsUtil.exists(hdfsPath)) {

				if (out.isZip()) {

					String zipName = FileUtil.path(localOutputDirectory, out.getName() + ".zip");

					if (out.isMergeOutput()) {

						HdfsUtil.compressAndMerge(zipName, hdfsPath, out.isRemoveHeader());

					} else {

						HdfsUtil.compress(zipName, hdfsPath);

					}

				} else {

					if (out.isMergeOutput()) {

						HdfsUtil.exportDirectoryAndMerge(localOutputDirectory, out.getName(), hdfsPath,
								out.isRemoveHeader());

					} else {

						HdfsUtil.exportDirectory(localOutputDirectory, out.getName(), hdfsPath);

					}

				}

			}

		}

		if (out.getType() == WdlParameterOutputType.HDFS_FILE) {

			String localOutputDirectory = FileUtil.path(localOutput, out.getName());

			FileUtil.createDirectory(localOutputDirectory);

			String filename = context.getOutput(out.getName());
			String hdfsPath = null;
			if (filename.startsWith("hdfs://") || filename.startsWith("file:/")) {

				hdfsPath = filename;

			} else {

				hdfsPath = HdfsUtil.makeAbsolute(HdfsUtil.path(workspace, filename));
			}

			if (!out.isZip()) {

				if (HdfsUtil.exists(hdfsPath)) {
					HdfsUtil.exportFile(localOutputDirectory, hdfsPath);
				}

			} else {
				if (HdfsUtil.exists(hdfsPath)) {
					HdfsUtil.compressFile(localOutputDirectory, hdfsPath);
				}
			}

		}

		out.setJobId(getId());

		String n = FileUtil.path(localOutput, out.getName());

		File f = new File(n);

		if (f.exists() && f.isDirectory()) {

			FileItem[] items = cloudgene.mapred.util.FileTree.getFileTree(localOutput, out.getName());

			List<Download> files = new Vector<Download>();

			for (FileItem item : items) {
				String hash = HashUtil
						.getMD5(item.getText() + item.getId() + item.getSize() + getId() + (Math.random() * 100000));
				Download download = new Download();
				download.setName(item.getText());
				download.setPath(FileUtil.path(getId(), item.getId()));
				download.setSize(item.getSize());
				download.setHash(hash);
				download.setParameter(out);
				download.setCount(MAX_DOWNLOAD);
				files.add(download);
			}
			Collections.sort(files);
			out.setFiles(files);
		}

		return true;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public WdlApp getApp() {
		return app;
	}

	@Override
	public void kill() {
		if (executor != null) {
			executor.kill();
		}
	}

	public void updateProgress() {

		if (executor != null) {

			executor.updateProgress();
			setProgress(executor.getProgress());

		} else {
			setProgress(-1);
		}

	}

}
