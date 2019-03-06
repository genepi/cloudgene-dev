package cloudgene.mapred.util.logging;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.service.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rollbar.notifier.Rollbar;

public class RollbarStatusService extends StatusService {

	protected static final Logger log = LoggerFactory.getLogger(RollbarStatusService.class);

	private Rollbar rollbar;

	public RollbarStatusService(Rollbar rollbar) {
		super();
		this.rollbar = rollbar;
	}

	@Override
	public Representation toRepresentation(Status status, Request request, Response response) {
		Representation representation = super.toRepresentation(status, request, response);

		if (status.isServerError()) {
			rollbar.error(status.getThrowable());
		}

		return representation;

	}

}