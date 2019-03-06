package cloudgene.mapred.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRespository;
import genepi.hadoop.HadoopCluster;
import genepi.io.FileUtil;

public class Settings {

	private String hadoopPath = "/usr/";

	private String pigPath = "/usr/";

	private String sparkPath = "/usr/bin/spark-submit";

	private String tempPath = "tmp";

	private String localWorkspace = "workspace";

	private String hdfsWorkspace = "cloudgene/data";

	private String hdfsAppWorkspace = "cloudgene/apps";

	private String streamingJar = "";

	private String version;

	private String name = "Cloudgene";

	private Map<String, String> colors;

	private String secretKey = "default-key-change-me";

	private Map<String, String> mail;

	private Map<String, String> database;

	private Map<String, String> cluster;

	private int autoRetireInterval = 5;

	private int retireAfter = 6;

	private int notificationAfter = 4;

	private int threadsSetupQueue = 5;

	private int threadsQueue = 5;

	private int maxRunningJobsPerUser = 2;

	private boolean autoRetire = false;

	private boolean streaming = true;

	private boolean removeHdfsWorkspace = true;

	private static final Logger log = LoggerFactory.getLogger(Settings.class);

	private boolean writeStatistics = true;

	private boolean https = false;

	private String httpsKeystore = "";

	private String httpsPassword = "";

	private boolean maintenance = false;

	private String adminMail = null;

	private String slack = null;

	private String urlPrefix = "";

	private List<MenuItem> navigation = new Vector<MenuItem>();

	private boolean secureCookie = false;

	private int uploadLimit = -1;

	private String googleAnalytics = "";

	private int maxDownloads = 10;

	protected Config config;

	private String port = "8082";

	private Map<String, String> rollbar = null;

	// fake!
	private List<Application> apps = new Vector<Application>();;

	private ApplicationRespository repository = ApplicationRespository.getInstance();

	public Settings() {

		// read default settings from env variables when set
		String name = System.getenv().get("CLOUDGENE_SERVICE_NAME");
		if (name != null) {
			this.name = name;
		}

		MenuItem helpMenuItem = new MenuItem();
		helpMenuItem.setId("help");
		helpMenuItem.setName("Help");
		String helpLink = System.getenv().get("CLOUDGENE_HELP_PAGE");
		if (helpLink != null) {
			helpMenuItem.setLink(helpLink);
		} else {
			helpMenuItem.setLink("http://docs.cloudgene.io");
		}
		navigation.add(helpMenuItem);

		database = new HashMap<String, String>();
		initDefaultDatabase(database, "data/mapred");

		colors = getDefaultColors();

	}

	public Settings(Config config) {

		this();
		this.config = config;

		// workspace in config has higher priority
		if (config.getWorkspace() != null) {
			setLocalWorkspace(config.getWorkspace());
		}

		if (config.getPort() != null && !config.getPort().trim().isEmpty()) {
			setPort(config.getPort());
		}

		// database in config has higher priority
		if (config.getDatabase() != null) {
			Map<String, String> database = getDatabase();
			if (database != null) {
				if (database.get("driver") != null && database.get("driver").equals("h2")) {
					database.put("database", config.getDatabase());
					// check file and create parent folders
					File databaseFile = new File(config.getDatabase());
					if (!databaseFile.exists()) {
						databaseFile.getParentFile().mkdirs();
					}
				}
			} else {
				database = new HashMap<String, String>();
				initDefaultDatabase(database, config.getDatabase());
			}
		}

	}

	public static Settings load(Config config) throws FileNotFoundException, YamlException {

		YamlConfig yamlConfig = new YamlConfig();
		yamlConfig.setPropertyElementType(Settings.class, "apps", Application.class);
		yamlConfig.setClassTag("cloudgene.mapred.util.Application", Application.class);
		YamlReader reader = new YamlReader(new FileReader(config.getSettings()), yamlConfig);
		Settings settings = reader.read(Settings.class);

		log.info("Auto retire: " + settings.isAutoRetire());
		log.info("Retire jobs after " + settings.retireAfter + " days.");
		log.info("Notify user after " + settings.notificationAfter + " days.");
		log.info("Write statistics: " + settings.writeStatistics);

		if (settings.cluster != null) {
			String conf = settings.cluster.get("conf");
			String username = settings.cluster.get("user");
			String name = settings.cluster.get("name");
			if (conf != null) {
				log.info("Use Haddop configuration folder '" + conf + "'"
						+ (username != null ? " with username " + username : ""));
				try {
					HadoopCluster.setConfPath(name, conf, username);
				} catch (NoClassDefFoundError e) {
				}
			}
		}

		settings.config = config;

		// workspace in config has higher priority
		if (config.getWorkspace() != null) {
			settings.setLocalWorkspace(config.getWorkspace());
		}

		if (config.getPort() != null && !config.getPort().trim().isEmpty()) {
			settings.setPort(config.getPort());
		}

		// database in config has higher priority
		if (config.getDatabase() != null) {
			Map<String, String> database = settings.getDatabase();
			if (database != null) {
				if (database.get("driver") != null && database.get("driver").equals("h2")) {
					database.put("database", config.getDatabase());
					// check file and create parent folders
					File databaseFile = new File(config.getDatabase());
					if (!databaseFile.exists()) {
						databaseFile.getParentFile().mkdirs();
					}
				}
			} else {
				database = new HashMap<String, String>();
				initDefaultDatabase(database, config.getDatabase());
			}
		}

		return settings;

	}

	public List<Application> getApps() {
		return apps;
	}

	public void setApps(List<Application> apps) {
		repository.init(apps);
	}

	public static void initDefaultDatabase(Map<String, String> database, String defaultSchema) {
		database.put("driver", "h2");
		database.put("database", defaultSchema);
		database.put("user", "mapred");
		database.put("password", "mapred");
	}

	public static Map<String, String> getDefaultColors() {
		Map<String, String> colors = new HashMap<String, String>();
		colors.put("background", "#343a40");
		colors.put("foreground", "navbar-dark");
		return colors;
	}

	public void save() {
		String filename = config.getSettings();
		save(filename);
	}

	public void save(String filename) {
		try {

			File file = new File(filename);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			}

			log.info("Storing settings to file " + filename + " (" + getApps().size() + " apps installed)");
			apps = repository.getAll();

			YamlConfig yamlConfig = new YamlConfig();
			yamlConfig.setPropertyElementType(Settings.class, "apps", Application.class);
			yamlConfig.setClassTag("cloudgene.mapred.util.Application", Application.class);

			YamlWriter writer = new YamlWriter(new FileWriter(filename), yamlConfig);
			writer.write(this);
			writer.close();

		} catch (Exception e) {
			log.error("Storing settings failed.", e);
		}

	}

	public String getHadoopPath() {
		return hadoopPath;
	}

	public void setHadoopPath(String hadoopPath) {
		this.hadoopPath = hadoopPath;
	}

	public void setPigPath(String pigPath) {
		this.pigPath = pigPath;
	}

	public String getPigPath() {
		return pigPath;
	}

	public String getTempPath() {
		return tempPath;
	}

	public void setTempPath(String tempPath) {
		this.tempPath = tempPath;
	}

	public String getLocalWorkspace() {
		return localWorkspace;
	}

	public void setLocalWorkspace(String localWorkspace) {
		this.localWorkspace = localWorkspace;
	}

	public String getHdfsWorkspace() {
		return hdfsWorkspace;
	}

	public void setHdfsWorkspace(String hdfsWorkspace) {
		this.hdfsWorkspace = hdfsWorkspace;
	}

	public String getHdfsAppWorkspace() {
		return hdfsAppWorkspace;
	}

	public void setHdfsAppWorkspace(String hdfsAppWorkspace) {
		this.hdfsAppWorkspace = hdfsAppWorkspace;
	}

	public String getStreamingJar() {
		return streamingJar;
	}

	public void setStreamingJar(String streamingJar) {
		this.streamingJar = streamingJar;
	}

	public boolean isStreaming() {
		return streaming;
	}

	public void setStreaming(boolean streaming) {
		this.streaming = streaming;
	}

	public boolean isRemoveHdfsWorkspace() {
		return removeHdfsWorkspace;
	}

	public void setRemoveHdfsWorkspace(boolean removeHdfsWorkspace) {
		this.removeHdfsWorkspace = removeHdfsWorkspace;
	}

	public boolean testPaths() {

		String hadoop = FileUtil.path(hadoopPath, "bin", "hadoop");

		if (!new File(hadoop).exists()) {

			log.warn("hadoop '" + hadoop + "' does not exist. please change it.");

			// return false;

		}
		/*
		 * if (!new File(streamingJar).exists()) {
		 * 
		 * log.error("streamingJar '" + streamingJar + "' does not exist.");
		 * 
		 * return false;
		 * 
		 * }
		 */

		return true;

	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, String> getMail() {
		return mail;
	}

	public void setMail(Map<String, String> mail) {
		this.mail = mail;
	}

	public Map<String, String> getCluster() {
		return cluster;
	}

	public void setCluster(Map<String, String> cluster) {
		this.cluster = cluster;
	}

	public String getSlack() {
		return slack;
	}

	public void setSlack(String slack) {
		this.slack = slack;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getTempFilename(String filename) {
		String path = getTempPath();
		String name = FileUtil.getFilename(filename);
		return FileUtil.path(path, name);
	}

	public void setNotificationAfter(int notificationAfter) {
		this.notificationAfter = notificationAfter;
	}

	public int getNotificationAfter() {
		return notificationAfter;
	}

	public int getNotificationAfterInSec() {
		return notificationAfter * 24 * 60 * 60;
	}

	public void setRetireAfter(int retireAfter) {
		this.retireAfter = retireAfter;
	}

	public int getRetireAfter() {
		return retireAfter;
	}

	public int getRetireAfterInSec() {
		return retireAfter * 24 * 60 * 60;
	}

	public void setAutoRetire(boolean autoRetire) {
		this.autoRetire = autoRetire;
	}

	public boolean isAutoRetire() {
		return autoRetire;
	}

	public void setWriteStatistics(boolean writeStatistics) {
		this.writeStatistics = writeStatistics;
	}

	public boolean isWriteStatistics() {
		return writeStatistics;
	}

	public void setHttps(boolean https) {
		this.https = https;
	}

	public boolean isHttps() {
		return https;
	}

	public void setHttpsKeystore(String httpsKeystore) {
		this.httpsKeystore = httpsKeystore;
	}

	public String getHttpsKeystore() {
		return httpsKeystore;
	}

	public void setHttpsPassword(String httpsPassword) {
		this.httpsPassword = httpsPassword;
	}

	public String getHttpsPassword() {
		return httpsPassword;
	}

	public void setMaintenance(boolean maintenance) {
		this.maintenance = maintenance;
	}

	public boolean isMaintenance() {
		return maintenance;
	}

	public void setAdminMail(String adminMail) {
		this.adminMail = adminMail;
	}

	public String getAdminMail() {
		return adminMail;
	}

	public String getSparkPath() {
		return sparkPath;
	}

	public void setSparkPath(String sparkPath) {
		this.sparkPath = sparkPath;
	}

	public void setThreadsQueue(int threadsQueue) {
		this.threadsQueue = threadsQueue;
	}

	public int getThreadsQueue() {
		return threadsQueue;
	}

	public int getMaxRunningJobsPerUser() {
		return maxRunningJobsPerUser;
	}

	public void setMaxRunningJobsPerUser(int maxRunningJobsPerUser) {
		this.maxRunningJobsPerUser = maxRunningJobsPerUser;
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public String getUrlPrefix() {
		return urlPrefix;
	}

	public void setDatabase(Map<String, String> database) {
		this.database = database;
	}

	public Map<String, String> getDatabase() {
		return database;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public void setNavigation(List<MenuItem> navigation) {
		this.navigation = navigation;
	}

	public List<MenuItem> getNavigation() {
		return navigation;
	}

	public boolean isSecureCookie() {
		return secureCookie;
	}

	public void setSecureCookie(boolean secureCookie) {
		this.secureCookie = secureCookie;
	}

	public int getUploadLimit() {
		return uploadLimit;
	}

	public void setUploadLimit(int uploadLimit) {
		this.uploadLimit = uploadLimit;
	}

	public void setThreadsSetupQueue(int threadsSetupQueue) {
		this.threadsSetupQueue = threadsSetupQueue;
	}

	public int getThreadsSetupQueue() {
		return threadsSetupQueue;
	}

	public int getAutoRetireInterval() {
		return autoRetireInterval;
	}

	public void setAutoRetireInterval(int autoRetireInterval) {
		this.autoRetireInterval = autoRetireInterval;
	}

	public Map<String, String> getColors() {
		return colors;
	}

	public void setColors(Map<String, String> colors) {
		this.colors = colors;
	}

	public void setGoogleAnalytics(String googleAnalytics) {
		this.googleAnalytics = googleAnalytics;
	}

	public String getGoogleAnalytics() {
		return googleAnalytics;
	}

	public void setMaxDownloads(int maxDownloads) {
		this.maxDownloads = maxDownloads;
	}

	public int getMaxDownloads() {
		return maxDownloads;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPort() {
		return port;
	}

	public void setRollbar(Map<String, String> rollbar) {
		this.rollbar = rollbar;
	}
	
	public Map<String, String> getRollbar() {
		return rollbar;
	}
}