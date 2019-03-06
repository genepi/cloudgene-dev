package cloudgene.mapred;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.resource.Directory;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;

import com.rollbar.notifier.Rollbar;

import cloudgene.mapred.api.v2.admin.ArchiveJob;
import cloudgene.mapred.api.v2.admin.ChangeGroup;
import cloudgene.mapred.api.v2.admin.ChangePriority;
import cloudgene.mapred.api.v2.admin.ChangeRetireDate;
import cloudgene.mapred.api.v2.admin.DeleteUser;
import cloudgene.mapred.api.v2.admin.GetAllJobs;
import cloudgene.mapred.api.v2.admin.GetGroups;
import cloudgene.mapred.api.v2.admin.GetStatistics;
import cloudgene.mapred.api.v2.admin.GetUsers;
import cloudgene.mapred.api.v2.admin.HotRetireJob;
import cloudgene.mapred.api.v2.admin.ResetDownloads;
import cloudgene.mapred.api.v2.admin.RetireJobs;
import cloudgene.mapred.api.v2.admin.server.BlockQueue;
import cloudgene.mapred.api.v2.admin.server.EnterMaintenance;
import cloudgene.mapred.api.v2.admin.server.ExitMaintenance;
import cloudgene.mapred.api.v2.admin.server.GetClusterDetails;
import cloudgene.mapred.api.v2.admin.server.GetServerLogs;
import cloudgene.mapred.api.v2.admin.server.GetSettings;
import cloudgene.mapred.api.v2.admin.server.GetTemplates;
import cloudgene.mapred.api.v2.admin.server.OpenQueue;
import cloudgene.mapred.api.v2.admin.server.UpdateSettings;
import cloudgene.mapred.api.v2.admin.server.UpdateTemplate;
import cloudgene.mapred.api.v2.data.ImporterFileList;
import cloudgene.mapred.api.v2.jobs.CancelJob;
import cloudgene.mapred.api.v2.jobs.DownloadResults;
import cloudgene.mapred.api.v2.jobs.GetChunk;
import cloudgene.mapred.api.v2.jobs.GetJobDetails;
import cloudgene.mapred.api.v2.jobs.GetJobStatus;
import cloudgene.mapred.api.v2.jobs.GetJobs;
import cloudgene.mapred.api.v2.jobs.GetLogs;
import cloudgene.mapred.api.v2.jobs.RestartJob;
import cloudgene.mapred.api.v2.jobs.ShareResults;
import cloudgene.mapred.api.v2.jobs.SubmitJob;
import cloudgene.mapred.api.v2.server.App;
import cloudgene.mapred.api.v2.server.Apps;
import cloudgene.mapred.api.v2.server.CloudgeneApps;
import cloudgene.mapred.api.v2.server.GetCounter;
import cloudgene.mapred.api.v2.server.GetVersion;
import cloudgene.mapred.api.v2.server.Server;
import cloudgene.mapred.api.v2.users.ActivateUser;
import cloudgene.mapred.api.v2.users.ApiTokens;
import cloudgene.mapred.api.v2.users.LoginUser;
import cloudgene.mapred.api.v2.users.LogoutUser;
import cloudgene.mapred.api.v2.users.RegisterUser;
import cloudgene.mapred.api.v2.users.ResetPassword;
import cloudgene.mapred.api.v2.users.UpdatePassword;
import cloudgene.mapred.api.v2.users.UserProfile;
import cloudgene.mapred.database.TemplateDao;
import cloudgene.mapred.jobs.WorkflowEngine;
import cloudgene.mapred.representations.CustomStatusService;
import cloudgene.mapred.resources.Admin;
import cloudgene.mapred.resources.Index;
import cloudgene.mapred.resources.Start;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.util.logging.RollbarStatusService;
import genepi.db.Database;

public class WebApp extends Application {

	private String root;

	private LocalReference webRoot;

	private LocalReference webRoot2;

	private Database database;

	private Settings settings;

	private WorkflowEngine workflowEngine;

	private Map<String, String> cacheTemplates;

	public WebApp(String root, String pages) {
		this.webRoot = LocalReference.createFileReference(new File(root));
		this.root = root;
		this.webRoot2 = LocalReference.createFileReference(new File(pages));
	}

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */

	public void setRollbar(Rollbar rollbar) {
		setStatusService(new RollbarStatusService(rollbar));
	}
	
	@Override
	public synchronized Restlet createInboundRoot() {

		String prefix = settings.getUrlPrefix();

		// Create a router Restlet that routes each call to a
		Router router = new Router(getContext());
		String target = prefix + "/index.html";
		Redirector redirector = new Redirector(getContext(), target, Redirector.MODE_CLIENT_PERMANENT);
		TemplateRoute route = router.attach(prefix, redirector);
		route.setMatchingMode(Template.MODE_EQUALS);

		route = router.attach(prefix + "/", redirector);
		route.setMatchingMode(Template.MODE_EQUALS);

		// resources
		router.attach(prefix + "/index", Index.class);
		router.attach(prefix + "/index.html", Index.class);
		router.attach(prefix + "/start.html", Start.class);
		router.attach(prefix + "/admin", Admin.class);
		router.attach(prefix + "/admin.html", Admin.class);

		// user authentication
		router.attach(prefix + "/login", LoginUser.class);
		router.attach(prefix + "/logout", LogoutUser.class);

		router.attach(prefix + "/api/v2/jobs", GetJobs.class);
		// jobs
		router.attach(prefix + "/api/v2/jobs", GetJobs.class);
		router.attach(prefix + "/api/v2/jobs/submit/{tool}", SubmitJob.class);
		router.attach(prefix + "/api/v2/jobs/{job}/status", GetJobStatus.class);
		router.attach(prefix + "/api/v2/jobs/{job}", GetJobDetails.class);
		router.attach(prefix + "/api/v2/jobs/{job}/cancel", CancelJob.class);
		router.attach(prefix + "/api/v2/jobs/{job}/restart", RestartJob.class);
		router.attach(prefix + "/api/v2/jobs/{job}/chunks/{filename}", GetChunk.class);

		// user registration
		router.attach(prefix + "/api/v2/users/register", RegisterUser.class);

		// activate user after registration
		router.attach(prefix + "/users/activate/{user}/{code}", ActivateUser.class);

		// reset password. Sends mail with link to update password
		router.attach(prefix + "/api/v2/users/reset", ResetPassword.class);

		// after reset, update password (needs activation code...)
		router.attach(prefix + "/api/v2/users/update-password", UpdatePassword.class);

		// get or update user profile
		router.attach(prefix + "/api/v2/users/{user}/profile", UserProfile.class);

		// create, delete, get api token
		router.attach(prefix + "/api/v2/users/{user}/api-token", ApiTokens.class);

		// returns all counters
		router.attach(prefix + "/api/v2/server", Server.class);		
		
		// returns all counters
		router.attach(prefix + "/api/v2/server/counters", GetCounter.class);

		// returns meta data about an app
		router.attach(prefix + "/api/v2/server/apps/{tool}", App.class);

		// returns a list of all installed apps
		router.attach(prefix + "/api/v2/server/apps", Apps.class);

		// returns a list of all apps registed on cloudgene.io
		router.attach(prefix + "/api/v2/server/cloudgene-apps", CloudgeneApps.class);

		// returns current version as svg image
		router.attach(prefix + "/api/v2/server/version.svg", GetVersion.class);

		// admin jobs
		router.attach(prefix + "/api/v2/admin/jobs", GetAllJobs.class);
		router.attach(prefix + "/api/v2/admin/jobs/retire", RetireJobs.class);
		router.attach(prefix + "/api/v2/admin/jobs/{job}/reset", ResetDownloads.class);
		router.attach(prefix + "/api/v2/admin/jobs/{job}/retire", HotRetireJob.class);
		router.attach(prefix + "/api/v2/admin/jobs/{job}/priority", ChangePriority.class);
		router.attach(prefix + "/api/v2/admin/jobs/{job}/change-retire/{days}", ChangeRetireDate.class);
		router.attach(prefix + "/api/v2/admin/jobs/{job}/archive", ArchiveJob.class);

		// admin users
		router.attach(prefix + "/api/v2/admin/users", GetUsers.class);
		router.attach(prefix + "/api/v2/admin/users/{username}/delete", DeleteUser.class);
		router.attach(prefix + "/api/v2/admin/users/changegroup", ChangeGroup.class);
		router.attach(prefix + "/api/v2/admin/groups", GetGroups.class);

		// admin server management
		router.attach(prefix + "/api/v2/admin/server/cluster", GetClusterDetails.class);
		router.attach(prefix + "/api/v2/admin/server/queue/open", OpenQueue.class);
		router.attach(prefix + "/api/v2/admin/server/queue/block", BlockQueue.class);
		router.attach(prefix + "/api/v2/admin/server/maintenance/enter", EnterMaintenance.class);
		router.attach(prefix + "/api/v2/admin/server/maintenance/exit", ExitMaintenance.class);
		router.attach(prefix + "/api/v2/admin/server/templates", GetTemplates.class);
		router.attach(prefix + "/api/v2/admin/server/templates/{id}", UpdateTemplate.class);

		router.attach(prefix + "/api/v2/admin/server/settings", GetSettings.class);
		router.attach(prefix + "/api/v2/admin/server/settings/update", UpdateSettings.class);
		router.attach(prefix + "/api/v2/admin/server/logs/{logfile}", GetServerLogs.class);
		router.attach(prefix + "/api/v2/admin/server/statistics", GetStatistics.class);

		// download resources
		router.attach(prefix + "/results/{job}/{id}", DownloadResults.class);
		
		TemplateRoute route2 = router.attach(prefix + "/results/{job}/{id}/{filename}", DownloadResults.class);
        Map routeVariables = route2.getTemplate().getVariables();
        routeVariables.put("filename", new Variable(Variable.TYPE_URI_PATH));
		
		router.attach(prefix + "/share/{username}/{hash}/{filename}", ShareResults.class);
		router.attach(prefix + "/logs/{id}", GetLogs.class);

		// ------------------

		router.attach(prefix + "/api/v2/importer/files", ImporterFileList.class);

		setStatusService(new CustomStatusService());

		Directory dir = new Directory(getContext(), webRoot2);
		dir.setListingAllowed(false);

		route = router.attach(prefix + "/static", dir);
		route.setMatchingMode(Template.MODE_STARTS_WITH);

		dir = new Directory(getContext(), webRoot);
		dir.setListingAllowed(false);

		route = router.attach(prefix + "/", dir);
		route.setMatchingMode(Template.MODE_STARTS_WITH);

		return router;
	}

	public String getRootFolder() {
		return root;
	}

	public Database getDatabase() {
		return database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public WorkflowEngine getWorkflowEngine() {
		return workflowEngine;
	}

	public void setWorkflowEngine(WorkflowEngine workflowEngine) {
		this.workflowEngine = workflowEngine;
	}

	public void reloadTemplates() {
		TemplateDao dao = new TemplateDao(database);
		List<cloudgene.mapred.util.Template> templates = dao.findAll();

		cacheTemplates = new HashMap<String, String>();
		for (cloudgene.mapred.util.Template snippet : templates) {
			cacheTemplates.put(snippet.getKey(), snippet.getText());
		}
	}

	public String getTemplate(String key) {

		String template = cacheTemplates.get(key);

		if (template != null) {
			return template;
		} else {
			return "!" + key;
		}

	}

	public String getTemplate(String key, Object... strings) {

		String template = cacheTemplates.get(key);

		if (template != null) {
			return String.format(template, strings);
		} else {
			return "!" + key;
		}

	}

}