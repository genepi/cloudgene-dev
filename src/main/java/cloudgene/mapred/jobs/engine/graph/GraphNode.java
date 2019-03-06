package cloudgene.mapred.jobs.engine.graph;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.jobs.CloudgeneStepFactory;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.steps.ErrorStep;
import cloudgene.mapred.steps.JavaInternalStep;
import cloudgene.mapred.steps.JavaInternalStepDeprecrated;
import cloudgene.mapred.wdl.WdlStep;
import cloudgene.sdk.internal.WorkflowStep;
import genepi.io.FileUtil;

public class GraphNode implements Runnable {
	private WdlStep step;

	private CloudgeneContext context;

	private AbstractJob job;

	private CloudgeneStep instance;

	private static final Logger log = LoggerFactory.getLogger(CloudgeneJob.class);

	private boolean successful = false;

	private boolean finish = false;

	private List<String> inputs;

	private List<String> outputs;

	private long time;

	private String id = "";

	public GraphNode(WdlStep step, CloudgeneContext context)
			throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		this.step = step;
		this.context = context;
		this.job = context.getJob();
		inputs = new Vector<String>();
		outputs = new Vector<String>();
		instance();
	}

	public WdlStep getStep() {
		return step;
	}

	public void setStep(WdlStep step) {
		this.step = step;
	}

	private void instance()
			throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		id = step.getName().toLowerCase().replace(" ", "_");

		// find step implementation

		CloudgeneStepFactory factory = CloudgeneStepFactory.getInstance();
		String classname = factory.getClassname(step);
		step.setClassname(classname);

		// create instance

		String path = new File(context.getWorkingDirectory()).getAbsolutePath();
		final String jar = FileUtil.path(path, step.getJar());

		try {

			File file = new File(jar);

			if (file.exists()) {

				URL url = file.toURL();

				URLClassLoader urlCl = new URLClassLoader(new URL[] { url }, CloudgeneJob.class.getClassLoader());
				Class myClass = urlCl.loadClass(step.getClassname());

				Object object = myClass.newInstance();

				if (object instanceof CloudgeneStep) {
					instance = (CloudgeneStep) object;
				} else if (object instanceof WorkflowStep) {
					instance = new JavaInternalStep((WorkflowStep) object);
					// old genepi-hadoop support! this is deprecreated!
				} else if (object instanceof genepi.hadoop.common.WorkflowStep) {
					instance = new JavaInternalStepDeprecrated((genepi.hadoop.common.WorkflowStep) object);
				} else {
					instance = new ErrorStep("Error during initialization: class " + step.getClassname() + " ( "
							+ object.getClass().getSuperclass().getCanonicalName() + ") "
							+ " has to extend CloudgeneStep or WorkflowStep. ");

				}

				// check requirements
				PluginManager pluginManager = PluginManager.getInstance();
				for (String plugin : instance.getRequirements()) {
					if (!pluginManager.isEnabled(plugin)) {
						instance = new ErrorStep(
								"Requirements not fullfilled. This steps needs plugin '" + plugin + "'");
					}
				}

			} else {

				instance = new ErrorStep("Error during initialization: Jar file '" + jar + "' not found.");

			}

		} catch (Exception e) {
			Writer writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			e.printStackTrace(printWriter);
			String s = writer.toString();
			instance = new ErrorStep("Error during initialization: " + s);

		}

		instance.setName(step.getName());
		instance.setJob(job);
	}

	@Override
	public void run() {

		context.setCurrentStep(instance);
		job.getSteps().add(instance);

		job.onStepStarted(instance);

		job.writeLog("------------------------------------------------------");
		job.writeLog(step.getName());
		job.writeLog("------------------------------------------------------");

		long start = System.currentTimeMillis();

		try {

			instance.setup(context);
			boolean successful = instance.run(step, context);

			if (!successful) {
				job.writeLog("  " + step.getName() + " [ERROR]");
				successful = false;
				finish = true;

				context.incCounter("steps.failure." + id, 1);
				context.submitCounter("steps.failure." + id);
				job.onStepFinished(instance);

				return;
			} else {
				long end = System.currentTimeMillis();
				long time = end - start;

				long h = (long) (Math.floor((time / 1000) / 60 / 60));
				long m = (long) ((Math.floor((time / 1000) / 60)) % 60);

				String t = (h > 0 ? h + " h " : "") + (m > 0 ? m + " min " : "")
						+ (int) ((Math.floor(time / 1000)) % 60) + " sec";

				job.writeLog("  " + step.getName() + " [" + t + "]");
				setTime(time);

			}
		} catch (Exception e) {
			log.error("Running extern job failed!", e);

			context.incCounter("steps.failure." + id, 1);
			context.submitCounter("steps.failure." + id);

			successful = false;
			finish = true;
			job.onStepFinished(instance);
			return;
		}

		context.incCounter("steps.success." + id, 1);
		context.submitCounter("steps.success." + id);

		finish = true;
		successful = true;
		job.onStepFinished(instance);

	}

	public boolean isSuccessful() {
		return successful;
	}

	public void kill() {
		if (instance != null) {
			instance.kill();
		}
	}

	public void updateProgress() {
		if (instance != null) {
			instance.updateProgress();
		}
	}

	public int getProgress() {
		if (instance != null) {
			return instance.getProgress();
		} else {
			return 0;
		}
	}

	public boolean isFinish() {
		return finish;
	}

	public void addInput(String input) {
		inputs.add(input);
	}

	public void addOutput(String output) {
		outputs.add(output);
	}

	public List<String> getInputs() {
		return inputs;
	}

	public List<String> getOutputs() {
		return outputs;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getExecutionTime() {
		return time;
	}

}
