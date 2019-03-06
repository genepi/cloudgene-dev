package cloudgene.mapred.api.v2.server;

import java.io.File;
import java.util.List;

import org.restlet.data.LocalReference;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import cloudgene.mapred.WebApp;
import cloudgene.mapred.apps.ApplicationRespository;
import cloudgene.mapred.core.User;
import cloudgene.mapred.util.BaseResource;
import cloudgene.mapred.util.Template;
import cloudgene.mapred.wdl.WdlApp;
import freemarker.template.Configuration;
import net.sf.json.JSONObject;

public class Server extends BaseResource {

	private ApplicationRespository repository = ApplicationRespository.getInstance();

	@Get
	public Representation getServer() {
		User user = getAuthUser(false);

		WebApp app = getWebApp();

		Configuration cfg = new Configuration();

		ContextTemplateLoader loader = new ContextTemplateLoader(getContext(),
				LocalReference.createFileReference(new File(app.getRootFolder())));

		cfg.setTemplateLoader(loader);

		JSONObject data = new JSONObject();
		data.put("name", getSettings().getName());
		data.put("background", getSettings().getColors().get("background"));
		data.put("foreground", getSettings().getColors().get("foreground"));
		data.put("footer", getWebApp().getTemplate(Template.FOOTER));
		if (user != null) {
			JSONObject userJson = new JSONObject();
			userJson.put("username", user.getUsername());
			userJson.put("mail", user.getMail());
			userJson.put("admin", user.isAdmin());
			userJson.put("name", user.getFullName());
			data.put("user", userJson);
			List<WdlApp> apps = repository.getAllByUser(user);
			data.put("apps", apps);
			data.put("loggedIn", true);

		} else {
			// get Public apps
			List<WdlApp> apps = repository.getAllByUser(null);
			data.put("apps", apps);
			data.put("loggedIn", false);
		}

		data.put("navigation", getSettings().getNavigation());
		if (getSettings().isMaintenance()) {
			data.put("maintenace", true);
			data.put("maintenaceMessage", getWebApp().getTemplate(Template.MAINTENANCE_MESSAGE));
		} else {
			data.put("maintenace", false);
		}
		
		return new StringRepresentation(data.toString());

	}

}
