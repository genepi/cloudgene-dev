package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.CloudgeneParameterOutput;
import cloudgene.mapred.jobs.Download;
import genepi.db.Database;
import genepi.db.IRowMapper;
import genepi.db.JdbcDataAccessObject;

public class DownloadDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(DownloadDao.class);

	public DownloadDao(Database database) {
		super(database);
	}

	public boolean insert(Download download) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into downloads (parameter_id, name, path, hash, count, size, job_id) ");
		sql.append("values (?,?,?,?,?,?,?)");

		try {

			Object[] params = new Object[7];
			params[0] = download.getParameterId();
			params[1] = download.getName();
			params[2] = download.getPath();
			params[3] = download.getHash();
			params[4] = download.getCount();
			params[5] = download.getSize();
			params[6] = download.getParameter().getId();

			update(sql.toString(), params);

			log.debug("insert download successful.");

		} catch (SQLException e) {
			log.error("insert download failed.", e);
			return false;
		}

		return true;
	}

	public boolean update(Download download) {
		StringBuilder sql = new StringBuilder();
		sql.append("update downloads set count = ? where hash = ? ");

		try {

			Object[] params = new Object[2];
			params[0] = download.getCount();
			params[1] = download.getHash();

			update(sql.toString(), params);

			log.debug("update download successful.");

		} catch (SQLException e) {
			log.error("update download failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<Download> findAllByParameter(CloudgeneParameterOutput parameter) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from downloads ");
		sql.append("where parameter_id = ? ");
		sql.append("order by path ");

		Object[] params = new Object[1];
		params[0] = parameter.getId();

		List<Download> result = new Vector<Download>();

		try {

			result = query(sql.toString(), params, new DownloadMapper());

			log.debug("find all downloads successful. results: "
					+ result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all downloads failed", e);
			return null;
		}
	}

	public Download findByHash(String hash) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from downloads ");
		sql.append("where hash = ? ");
		sql.append("order by path ");

		Object[] params = new Object[1];
		params[0] = hash;

		Download result = null;

		try {

			result = (Download) queryForObject(sql.toString(), params,
					new DownloadMapper());

			log.debug("find download by hash successful. results: " + result);

			return result;
		} catch (SQLException e) {
			log.error("find download by hash failed", e);
			return null;
		}
	}

	public Download findByJobAndPath(String job, String path) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from downloads ");
		sql.append("where path = ? ");
		sql.append("order by path ");

		Object[] params = new Object[1];
		params[0] = job + "/" + path;

		Download result = null;

		try {

			result = (Download) queryForObject(sql.toString(), params,
					new DownloadMapper());

			log.debug("find download by job " + job + " and path " + path
					+ " successful. results: " + result);

			return result;
		} catch (SQLException e) {
			log.error("find download by job and path failed.", e);
			return null;
		}
	}

	class DownloadMapper implements IRowMapper {

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			Download result = new Download();
			result.setCount(rs.getInt("count"));
			result.setHash(rs.getString("hash"));
			result.setName(rs.getString("name"));
			result.setPath(rs.getString("path"));
			result.setSize(rs.getString("size"));
			result.setParameterId(rs.getInt("parameter_id"));
			return result;
		}

	}

}
