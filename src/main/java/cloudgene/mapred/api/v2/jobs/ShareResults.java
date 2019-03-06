package cloudgene.mapred.api.v2.jobs;

import org.restlet.data.MediaType;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.DownloadDao;
import cloudgene.mapred.jobs.Download;
import cloudgene.mapred.util.BaseResource;
import genepi.io.FileUtil;

public class ShareResults extends BaseResource {

	private static final Logger log = LoggerFactory.getLogger(ShareResults.class);

	@Get
	public Representation get() {

		String username = (String) getRequest().getAttributes().get("username");
		String hash = (String) getRequest().getAttributes().get("hash");
		String filename = (String) getRequest().getAttributes().get("filename");

		DownloadDao dao = new DownloadDao(getDatabase());
		Download download = dao.findByHash(hash);

		if (download == null) {
			return error404("download not found.");
		}

		if (!download.getName().equals(filename)) {
			return error404("download not found.");
		}

		if (download.getCount() == 0) {
			return error400("number of max downloads exceeded.");
		}

		MediaType mediaType = MediaType.ALL;
		if (filename.endsWith(".zip")) {
			mediaType = MediaType.APPLICATION_ZIP;
		} else if (filename.endsWith(".txt") || filename.endsWith(".csv")) {
			mediaType = MediaType.TEXT_PLAIN;
		} else if (filename.endsWith(".pdf")) {
			mediaType = MediaType.APPLICATION_PDF;
		} else if (filename.endsWith(".html")) {
			mediaType = MediaType.TEXT_HTML;
		}

		String resultFile = FileUtil.path(getSettings().getLocalWorkspace(), download.getPath());

		log.debug("Downloading file " + resultFile);

		download.decCount();
		dao.update(download);

		return new FileRepresentation(resultFile, mediaType);

	}

}
