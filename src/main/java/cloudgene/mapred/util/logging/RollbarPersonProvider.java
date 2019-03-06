package cloudgene.mapred.util.logging;

import org.restlet.Request;

import com.rollbar.api.payload.data.Person;
import com.rollbar.notifier.provider.Provider;

public class RollbarPersonProvider implements Provider<Person> {

	@Override
	public Person provide() {

		Request request = Request.getCurrent();

		if (request != null) {
			if (request.getClientInfo().getUser() != null) {
				return new Person.Builder().id(request.getClientInfo().getUser().getIdentifier()).build();
			}
		}
		return null;
	}
}