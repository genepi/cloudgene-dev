package cloudgene.mapred.util.logging;
import java.util.HashMap;
import java.util.Map;

import org.restlet.data.Header;

import com.rollbar.api.payload.data.Request;
import com.rollbar.notifier.provider.Provider;

/**
 * {@link Request} provider.
 */
public class RollbarRequestProvider implements Provider<Request> {


  @Override
  public Request provide() {
    org.restlet.Request req = org.restlet.Request.getCurrent();

    if (req != null) {
      Request request = new Request.Builder()
          .url(url(req))
          .method(method(req))
          .headers(headers(req))
          //.get(getParams(req))
          .queryString(queryString(req))
          .userIp(userIp(req))
          .build();

      return request;
    }

    return null;
  }

  private String userIp(org.restlet.Request request) {
    return request.getClientInfo().getAddress();
  }

  private static String url(org.restlet.Request request) {
    return  request.getOriginalRef().toUrl().toString();
  }

  private static String method(org.restlet.Request request) {
    return request.getMethod().toString();
  }

  private static Map<String, String> headers(org.restlet.Request request) {
    Map<String, String> headers = new HashMap<>();

    for (Header header: request.getHeaders()) {
      String headerName = header.getName();
      String headerValue = header.getValue();
      headers.put(headerName, headerValue);
    }

    return headers;
  }

  private static String queryString(org.restlet.Request request) {
    return request.getOriginalRef().getQuery();
  }

}