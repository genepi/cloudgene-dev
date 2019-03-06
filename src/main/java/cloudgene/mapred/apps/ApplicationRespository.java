package cloudgene.mapred.apps;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.util.GitHubUtil;
import cloudgene.mapred.util.GitHubUtil.Repository;
import cloudgene.mapred.wdl.WdlApp;
import genepi.io.FileUtil;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

public class ApplicationRespository {

	private List<Application> apps;

	private Map<String, Application> indexApps;

	private String appsFolder = "apps";

	private static ApplicationRespository instance;

	private static final Logger log = LoggerFactory.getLogger(ApplicationRespository.class);

	public static ApplicationRespository getInstance() {
		if (instance == null) {
			instance = new ApplicationRespository("apps");
		}
		return instance;
	}

	private ApplicationRespository(String appsFolder) {
		this.appsFolder = appsFolder;
		apps = new Vector<Application>();
		reload();
	}

	public List<Application> getAll() {
		return apps;
	}

	public void init(List<Application> apps) {
		this.apps = apps;
		reload();
	}

	public void reload() {
		indexApps = new HashMap<String, Application>();
		for (Application app : apps) {
			log.info("Register application " + app.getId());
			// load application
			try {
				log.info("Load workflow file " + app.getFilename());
				app.loadWdlApp();
				WdlApp wdlApp = app.getWdlApp();
				// update wdl id with id from application
				if (wdlApp != null) {
					wdlApp.setId(app.getId());
				}
			} catch (IOException e) {
				log.error("Application " + app.getId() + " has syntax errors.", e);
			}
			indexApps.put(app.getId(), app);

		}
	}

	public Application getByIdAndUser(String id, User user) {

		Application app = getById(id);

		if (app != null && app.isEnabled() && app.isLoaded() && !app.hasSyntaxError()) {

			if (user == null) {
				if (app.getPermission().toLowerCase().equals("public")) {
					if (app.getWdlApp().getWorkflow() != null) {
						return app;
					} else {
						return app;
					}
				} else {
					return null;
				}
			}

			if (user.isAdmin() || user.hasRole(app.getPermission())
					|| app.getPermission().toLowerCase().equals("public")) {
				if (app.getWdlApp().getWorkflow() != null) {
					return app;
				} else {
					return app;
				}

			} else {
				return null;
			}

		}

		return null;

	}

	public Application getById(String id) {

		Application app = indexApps.get(id);
		return app;

	}

	public List<WdlApp> getAllByUser(User user) {
		return getAllByUser(user, true);
	}

	public List<WdlApp> getAllByUser(User user, boolean appsOnly) {

		List<WdlApp> listApps = new Vector<WdlApp>();

		for (Application application : getAll()) {

			boolean using = true;

			if (user == null) {
				if (application.getPermission().toLowerCase().equals("public")) {
					using = true;
				} else {
					using = false;
				}
			} else {

				if (!user.isAdmin() && !application.getPermission().toLowerCase().equals("public")) {

					if (!user.hasRole(application.getPermission())) {
						using = false;
					}
				}
			}

			if (using) {

				if (application.isEnabled() && application.isLoaded() && !application.hasSyntaxError()) {
					if (appsOnly) {
						if (application.getWdlApp().getWorkflow() != null) {
							WdlApp app = application.getWdlApp();
							listApps.add(app);
						}
					} else {
						WdlApp app = application.getWdlApp();
						listApps.add(app);
					}
				}

			}

		}

		Collections.sort(listApps);
		return listApps;

	}

	public void remove(Application application) throws IOException {

		// delete application in app folder
		String id = application.getId();
		// String appPath = FileUtil.path(config.getApps(), id);
		// FileUtil.deleteDirectory(appPath);

		// remove from app list
		apps.remove(application);
		reload();

	}

	public void removeById(String id) throws IOException {
		for (Application app : new Vector<Application>(getAll())) {
			if (app.getId().startsWith(id)) {
				remove(app);
			}
		}
	}

	public Application installFromUrl(String url) throws IOException {
		// download file from url
		if (url.endsWith(".zip")) {
			File zipFile = File.createTempFile("download", ".zip");
			FileUtils.copyURLToFile(new URL(url), zipFile);
			return installFromZipFile(zipFile.getAbsolutePath());
		} /*
			 * else { try { String appPath = FileUtil.path(appsFolder, "new-app");
			 * FileUtil.createDirectory(appPath);
			 * 
			 * String yamlFilename = FileUtil.path(appPath, "cloudgene.yaml");
			 * 
			 * FileUtils.copyURLToFile(new URL(url), new File(yamlFilename)); Application
			 * application = installFromYaml(yamlFilename);
			 * 
			 * return application;
			 * 
			 * } catch (IOException e) { e.printStackTrace(); throw e; } }
			 */
		return null;

	}

	public Application installFromGitHub(Repository repository)
			throws MalformedURLException, IOException {

		String url = GitHubUtil.buildUrlFromRepository(repository);
		File zipFile = File.createTempFile("github", ".zip");
		FileUtils.copyURLToFile(new URL(url), zipFile);

		String zipFilename = zipFile.getAbsolutePath();
		if (repository.getDirectory() != null) {
			// extract only sub dir
			Application application = installFromZipFile(zipFilename, "^.*/" + repository.getDirectory() + ".*");
			return application;

		} else {
			Application application = installFromZipFile(zipFilename);
			return application;
		}

	}

	public Application installFromZipFile(String zipFilename) throws IOException {

		// extract in apps folder

		String appPath = FileUtil.path(appsFolder, "archive");
		FileUtil.deleteDirectory(appPath);
		FileUtil.createDirectory(appPath);
		try {
			ZipFile file = new ZipFile(zipFilename);
			file.extractAll(appPath);
		} catch (ZipException e) {
			throw new IOException(e);
		}

		try {
			Application application = installFromDirectory(appPath, true);
			return application;
		} finally {
			FileUtil.deleteDirectory(appPath);
		}

	}

	public Application installFromZipFile(String zipFilename, String subFolder) throws IOException {

		String appPath = FileUtil.path(appsFolder, "archive");
		FileUtil.deleteDirectory(appPath);
		FileUtil.createDirectory(appPath);
		try {
			ZipFile file = new ZipFile(zipFilename);
			for (Object header : file.getFileHeaders()) {
				FileHeader fileHeader = (FileHeader) header;
				String name = fileHeader.getFileName();
				if (name.matches(subFolder)) {
					file.extractFile(fileHeader, appPath);
				}
			}
		} catch (ZipException e) {
			throw new IOException(e);
		}

		try {
			Application application = installFromDirectory(appPath, true);
			return application;
		} finally {
			FileUtil.deleteDirectory(appPath);
		}

	}

	public Application installFromDirectory(String path, boolean moveToApps) throws IOException {

		String cloudgeneFilename = FileUtil.path(path, "cloudgene.yaml");
		if (new File(cloudgeneFilename).exists()) {
			Application application = installFromYaml(cloudgeneFilename, moveToApps);
			if (application != null) {
				return application;
			}
		}

		// find all cloudgene workflows (use filename as id)
		String[] files = FileUtil.getFiles(path, "*.yaml");

		for (String filename : files) {
			Application application = installFromYaml(filename, moveToApps);
			if (application != null) {
				return application;
			}
		}

		// search in subfolders
		for (String directory : getDirectories(path)) {
			Application application = installFromDirectory(directory, moveToApps);
			if (application != null) {
				return application;
			}
		}
		return null;

	}

	public Application installFromYaml(String filename, boolean moveToApps) throws IOException {

		Application application = new Application();
		application.setFilename(filename);
		application.setPermission("user");
		try {
			application.loadWdlApp();
		} catch (IOException e) {
			log.warn("Ignore file " + filename + ". Not a valid cloudgene.yaml file.", e);
			return null;
		}

		String id = application.getWdlApp().getId() + ":" + application.getWdlApp().getVersion();

		//application with same version is already installed.
		if (indexApps.get(id) != null) {
			throw new IOException("Application " + id + " is already installed");
		}
		
		//TODO: check if its an update an remove old version. atm both versions are installed
		

		if (moveToApps) {

			File file = new File(filename);
			File folder = file.getParentFile();

			String targetPath = FileUtil.path(appsFolder, application.getWdlApp().getId(),
					application.getWdlApp().getVersion());
			FileUtil.createDirectory(targetPath);
			File target = new File(targetPath);

			Files.move(folder.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

			application.setFilename(FileUtil.path(targetPath, file.getName()));
			try {
				application.loadWdlApp();
			} catch (IOException e) {
				log.warn("Ignore file " + filename + ". Not a valid cloudgene.yaml file.", e);
				return null;
			}

		}

		application.setId(id);
		apps.add(application);

		WdlApp wdlApp = application.getWdlApp();
		// TODO: check if it is needed!
		if (wdlApp != null) {
			wdlApp.setId(id);
		}

		indexApps.put(application.getId(), application);

		return application;

	}

	private String[] getDirectories(String path) {
		File dir = new File(path);
		File[] files = dir.listFiles();

		int count = 0;
		for (File file : files) {
			if (file.isDirectory()) {
				count++;
			}
		}

		String[] names = new String[count];

		count = 0;
		for (File file : files) {
			if (file.isDirectory()) {
				names[count] = file.getAbsolutePath();
				count++;
			}
		}

		return names;
	}
}
