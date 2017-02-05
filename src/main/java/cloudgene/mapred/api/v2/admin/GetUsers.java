package cloudgene.mapred.api.v2.admin;

import java.util.Date;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonBeanProcessor;
import net.sf.json.processors.JsonValueProcessor;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.util.BaseResource;

public class GetUsers extends BaseResource {

	@Get
	public Representation get() {

		User user = getAuthUser();

		if (user == null) {

			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation(
					"The request requires user authentication.");

		}

		if (!user.isAdmin()) {
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation(
					"The request requires administration rights.");
		}

		UserDao dao = new UserDao(getDatabase());
		List<User> users = dao.findAll();

		JsonConfig config = new JsonConfig();
		config.setExcludes(new String[] { "password","apiToken" });
		config.registerJsonValueProcessor("lastLogin", new JsonValueProcessor() {
			
			@Override
			public Object processObjectValue(String arg0, Object arg1, JsonConfig arg2) {
				if (arg1 == null){
					return null;
				}
				return (((Date)arg1).toString());

			}
			
			@Override
			public Object processArrayValue(Object arg0, JsonConfig arg1) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		config.registerJsonValueProcessor("lockedUntil", new JsonValueProcessor() {
			
			@Override
			public Object processObjectValue(String arg0, Object arg1, JsonConfig arg2) {
				if (arg1 == null){
					return null;
				}
				return (((Date)arg1).toString());

			}
			
			@Override
			public Object processArrayValue(Object arg0, JsonConfig arg1) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		JSONArray jsonArray = JSONArray.fromObject(users, config);

		return new StringRepresentation(jsonArray.toString());

	}

}
