package cloudgene.mapred.util;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import cloudgene.mapred.WebApp;
import cloudgene.mapred.core.JWTUtil;
import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.WorkflowEngine;
import genepi.db.Database;

public class BaseResource extends ServerResource {

	private WebApp application;

	private Database database;

	@Override
	protected void doInit() throws ResourceException {
		super.doInit();

		application = (WebApp) getApplication();
		database = application.getDatabase();

	}

	public Database getDatabase() {
		return database;
	}

	public WebApp getWebApp() {
		return application;
	}

	public User getAuthUser() {
		return getAuthUser(true);
	}
	
	public User getAuthUser(boolean checkCsrf) {

		User user = JWTUtil.getUserByRequest(getDatabase(), getRequest(), getSettings().getSecretKey(), checkCsrf);
		if (user != null) {
			
			getRequest().getClientInfo().setUser(new org.restlet.security.User(user.getUsername()));
			
			return user;
		} else {
			return null;
		}

	}

	public Settings getSettings() {
		return application.getSettings();
	}

	public WorkflowEngine getWorkflowEngine() {
		return application.getWorkflowEngine();
	}

	public Representation error(Status status, String message) {

		setStatus(status, message);

		JSONObject jsonObject = new JSONObject();

		try {

			jsonObject.put("success", false);
			jsonObject.put("message", message);

		} catch (JSONException e) {

			setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			return new EmptyRepresentation();

		}
		return new JsonRepresentation(jsonObject);

	}

	public Representation ok(String message) {

		setStatus(Status.SUCCESS_OK, message);

		JSONObject jsonObject = new JSONObject();

		try {

			jsonObject.put("success", true);
			jsonObject.put("message", message);

		} catch (JSONException e) {

			setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			return new EmptyRepresentation();

		}
		return new JsonRepresentation(jsonObject);

	}

	public Representation ok(String message, Map<String, Object> params) {

		setStatus(Status.SUCCESS_OK, message);

		JSONObject jsonObject = new JSONObject();

		try {

			jsonObject.put("success", true);
			jsonObject.put("message", message);
			for (String key : params.keySet()) {
				jsonObject.put(key, params.get(key));
			}

		} catch (JSONException e) {

			setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			return new EmptyRepresentation();

		}
		return new JsonRepresentation(jsonObject);

	}

	public Representation error401(String message) {
		return error(Status.CLIENT_ERROR_UNAUTHORIZED, message);
	}

	public Representation error403(String message) {
		return error(Status.CLIENT_ERROR_FORBIDDEN, message);
	}

	public Representation error404(String message) {
		return error(Status.CLIENT_ERROR_NOT_FOUND, message);
	}

	public Representation error400(String message) {
		return error(Status.CLIENT_ERROR_BAD_REQUEST, message);
	}

}