package cloudgene.mapred.util;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;
import org.restlet.engine.Engine;

import cloudgene.mapred.Main;
import cloudgene.mapred.WebServer;
import cloudgene.mapred.apps.Application;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.TemplateDao;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.jobs.PersistentWorkflowEngine;
import cloudgene.mapred.jobs.WorkflowEngine;
import cloudgene.mapred.util.Config;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.util.Template;
import genepi.db.Database;
import genepi.db.DatabaseUpdater;
import genepi.db.h2.H2Connector;
import genepi.io.FileUtil;
import junit.framework.Test;

public class TestServer {

	public static int PORT = 8085;

	public static final String HOSTNAME = "http://localhost:" + PORT;

	protected WebServer server;

	protected User adminUser;

	protected User user;

	protected Settings settings = new Settings(new Config());

	private Database database;

	private WorkflowEngine engine;

	private List<Application> applications;

	private Thread engineThread;

	private static TestServer instance;

	private TestServer() {
		HashMap<String, String> mail = new HashMap<String, String>();
		mail.put("smtp", "localhost");
		mail.put("port", TestMailServer.PORT + "");
		mail.put("user", "");
		mail.put("password", "");
		mail.put("name", "noreply@cloudgene");
		settings.setMail(mail);
	}

	public List<Application> registerApplications() {

		if (applications == null) {

			List<Application> applications = new Vector<Application>();

			Application app = new Application();
			app.setId("return-true-step-public");
			app.setFilename("test-data/return-true.yaml");
			app.setPermission("public");
			applications.add(app);

			Application app2 = new Application();
			app2.setId("return-false-step-public");
			app2.setFilename("test-data/return-false.yaml");
			app2.setPermission("public");
			applications.add(app2);

			Application app3 = new Application();
			app3.setId("return-exception-step-public");
			app3.setFilename("test-data/return-exception.yaml");
			app3.setPermission("public");
			applications.add(app3);

			Application app4 = new Application();
			app4.setId("write-text-to-file");
			app4.setFilename("test-data/write-text-to-file.yaml");
			app4.setPermission("public");
			applications.add(app4);

			Application app5 = new Application();
			app5.setId("return-true-in-setup");
			app5.setFilename("test-data/return-true-in-setup.yaml");
			app5.setPermission("public");
			applications.add(app5);

			Application app6 = new Application();
			app6.setId("return-false-in-setup");
			app6.setFilename("test-data/return-false-in-setup.yaml");
			app6.setPermission("public");
			applications.add(app6);

			Application app7 = new Application();
			app7.setId("all-possible-inputs");
			app7.setFilename("test-data/all-possible-inputs.yaml");
			app7.setPermission("public");
			applications.add(app7);

			Application app71 = new Application();
			app71.setId("all-possible-inputs-private");
			app71.setFilename("test-data/all-possible-inputs.yaml");
			app71.setPermission("private");
			applications.add(app71);

			Application app8 = new Application();
			app8.setId("long-sleep");
			app8.setFilename("test-data/long-sleep.yaml");
			app8.setPermission("public");
			applications.add(app8);

			Application app9 = new Application();
			app9.setId("write-files-to-folder");
			app9.setFilename("test-data/write-files-to-folder.yaml");
			app9.setPermission("public");
			applications.add(app9);

			Application app13 = new Application();
			app13.setId("three-tasks");
			app13.setFilename("test-data/three-tasks.yaml");
			app13.setPermission("public");
			applications.add(app13);

			Application app14 = new Application();
			app14.setId("write-text-to-std-out");
			app14.setFilename("test-data/write-text-to-std-out.yaml");
			app14.setPermission("public");
			applications.add(app14);

			// hdfs

			Application app10 = new Application();
			app10.setId("all-possible-inputs-hdfs");
			app10.setFilename("test-data/all-possible-inputs-hdfs.yaml");
			app10.setPermission("public");
			applications.add(app10);

			Application app11 = new Application();
			app11.setId("write-files-to-hdfs-folder");
			app11.setFilename("test-data/write-files-to-hdfs-folder.yaml");
			app11.setPermission("public");
			applications.add(app11);

			Application app12 = new Application();
			app12.setId("write-text-to-hdfs-file");
			app12.setFilename("test-data/write-text-to-hdfs-file.yaml");
			app12.setPermission("public");
			applications.add(app12);

			Application app16 = new Application();
			app16.setId("sftp-import");
			app16.setFilename("test-data/sftp-import.yaml");
			app16.setPermission("public");
			applications.add(app16);

			Application app17 = new Application();
			app17.setId("app-links");
			app17.setFilename("test-data/app-links.yaml");
			app17.setPermission("public");
			applications.add(app17);

			Application app18 = new Application();
			app18.setId("app-links-child");
			app18.setFilename("test-data/app-links-child.yaml");
			app18.setPermission("public");
			applications.add(app18);

			Application app19 = new Application();
			app19.setId("app-links-child-protected");
			app19.setFilename("test-data/app-links-child.yaml");
			app19.setPermission("protected");
			applications.add(app19);

			Application app20 = new Application();
			app20.setId("app-installation2");
			app20.setFilename("test-data/app-installation2.yaml");
			app20.setPermission("public");
			applications.add(app20);

			
			Application app21 = new Application();
			app21.setId("app-installation-child");
			app21.setFilename("test-data/app-installation-child.yaml");
			app21.setPermission("public");
			applications.add(app21);

			Application app22 = new Application();
			app22.setId("print-hidden-inputs");
			app22.setFilename("test-data/print-hidden-inputs.yaml");
			app22.setPermission("public");
			applications.add(app22);
			
			
			
			settings.setApps(applications);

		}

		return applications;

	}

	public static TestServer getInstance() {
		if (instance == null) {
			instance = new TestServer();
		}
		return instance;

	}

	public Database createDatabase(boolean newDatabase) throws SQLException {

		if (database != null) {
			return database;
		}

		// delete old database
		if (newDatabase) {
			FileUtil.deleteDirectory("test-database");
			// FileUtil.createDirectory("test-database");
		}

		H2Connector connector = new H2Connector("./test-database/mapred", "mapred", "mapred", false);
		// DatabaseConnector connector = new MySqlConnector("localhost", "3306",
		// "cloudgene",
		// "root", "lukas");
		database = new Database();

		try {

			database.connect(connector);

			if (connector.isNewDatabase()) {

				// init schema
				InputStream is = Main.class.getResourceAsStream("/create-tables.sql");
				connector.executeSQL(is);

				File versionFile = new File("version.txt");
				if (versionFile.exists()) {
					versionFile.delete();
				}
			}

			InputStream is = Main.class.getResourceAsStream("/updates.sql");
			DatabaseUpdater askimedUpdater = new DatabaseUpdater(connector, "version.txt", is, Main.VERSION);
			if (askimedUpdater.needUpdate()) {
				if (!askimedUpdater.update()) {
					database.disconnect();
					System.exit(1);
				}
			}

			String username = "admin";
			String password = "admin1978";

			// insert user admin
			UserDao dao = new UserDao(database);
			adminUser = dao.findByUsername(username);
			if (adminUser == null) {
				adminUser = new User();
				adminUser.setUsername(username);
				password = HashUtil.getMD5(password);
				adminUser.setPassword(password);
				adminUser.makeAdmin();
				dao.insert(adminUser);
			}

			String usernameUser = "user";
			String passwordUser = "admin1978";

			// insert user admin
			user = dao.findByUsername(usernameUser);
			if (user == null) {
				user = new User();
				user.setUsername(usernameUser);
				password = HashUtil.getMD5(passwordUser);
				user.setPassword(passwordUser);
				user.setRoles(new String[] { "public" });
				dao.insert(user);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);

		}
		return database;

	}

	public WorkflowEngine startWorkflowEngineWithoutServer() throws SQLException {
		if (engine == null) {

			registerApplications();

			database = createDatabase(true);
			// start workflow engine
			engine = new PersistentWorkflowEngine(database, 1, 1);
			engineThread = new Thread(engine);
			engineThread.start();
		}
		return engine;
	}

	public void start() throws SQLException {
		start(true);
	}

	public void start(boolean newDatabase) throws SQLException {

		if (server == null) {

			registerApplications();

			database = createDatabase(newDatabase);

			// inser messages
			TemplateDao htmlSnippetDao = new TemplateDao(database);

			for (Template defaultSnippet : Template.SNIPPETS) {

				Template snippet = htmlSnippetDao.findByKey(defaultSnippet.getKey());
				if (snippet == null) {
					htmlSnippetDao.insert(defaultSnippet);
				} else {
				}

			}

			try {

				// start workflow engine
				WorkflowEngine engine = startWorkflowEngineWithoutServer();

				PropertyConfigurator.configure("config/log4j.properties");


				String webAppFolder = "";

				if (new File("webapp").exists()) {
					webAppFolder = "webapp";
				} else {
					webAppFolder = FileUtil
							.path(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
									.getParent(), "html", "webapp");
					System.out.println(webAppFolder);
				}

				String pagesFolder = "";
				if (new File("pages").exists()) {
					pagesFolder = "pages";
				} else {
					pagesFolder = "sample/pages";
				}

				server = new WebServer();
				server.setPort(PORT);
				server.setRootDirectory(webAppFolder);
				server.setPagesDirectory(pagesFolder);

				server.setDatabase(database);
				server.setSettings(settings);
				server.setWorkflowEngine(engine);

				server.start();

			} catch (Exception e) {

				database.disconnect();
				System.exit(1);

			}
		}

	}

	public void reStartWebServer() throws Exception {
		engine.stop();
		engineThread.stop();
		engine = null;
		database.disconnect();

		server.stop();
		server = null;
		database = null;
		start(false);

	}

	public User getAdminUser() {
		return adminUser;
	}

	public User getUser() {
		return user;
	}

	public Settings getSettings() {
		return settings;
	}

	public Database getDatabase() {
		return database;
	}

}
