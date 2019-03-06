package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.CloudgeneParameterInput;
import cloudgene.mapred.jobs.CloudgeneParameterOutput;
import cloudgene.mapred.jobs.Download;
import cloudgene.mapred.wdl.WdlParameterInputType;
import cloudgene.mapred.wdl.WdlParameterOutputType;
import genepi.db.Database;
import genepi.db.IRowMapper;
import genepi.db.JdbcDataAccessObject;

public class ParameterDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(ParameterDao.class);

	public ParameterDao(Database database) {
		super(database);
	}

	public boolean insert(CloudgeneParameterInput parameter) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into parameter (name, value, input, job_id, type, variable, download, format, admin_only) ");
		sql.append("values (?,?,?,?,?,?,?,?,?)");

		try {

			Object[] params = new Object[9];
			if (parameter.getDescription() != null) {
				params[0] = parameter.getDescription().substring(0, Math.min(parameter.getDescription().length(), 100));
			} else {
				params[0] = "";
			}
			params[1] = parameter.getValue();
			params[2] = true;
			params[3] = parameter.getJob().getId();
			params[4] = parameter.getType().toString();
			params[5] = parameter.getName();
			params[6] = false;
			params[7] = "";
			params[8] = parameter.isAdminOnly();

			int paramId = insert(sql.toString(), params);
			parameter.setId(paramId);

			log.debug("insert parameter '" + parameter.getId() + "' successful.");

		} catch (SQLException e) {
			log.error("insert parameter '" + parameter.getId() + "' failed.", e);
			return false;
		}

		return true;
	}

	public boolean insert(CloudgeneParameterOutput parameter) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into parameter (name, value, input, job_id, type, variable, download, format, admin_only) ");
		sql.append("values (?,?,?,?,?,?,?,?,?)");

		try {

			Object[] params = new Object[9];
			params[0] = parameter.getDescription().substring(0, Math.min(parameter.getDescription().length(), 100));
			params[1] = parameter.getValue();
			params[2] = false;
			params[3] = parameter.getJob().getId();
			params[4] = parameter.getType().toString();
			params[5] = parameter.getName();
			params[6] = parameter.isDownload();
			params[7] = "";
			params[8] = parameter.isAdminOnly();

			int paramId = insert(sql.toString(), params);
			parameter.setId(paramId);

			log.debug("insert parameter '" + parameter.getId() + "' successful.");

		} catch (SQLException e) {
			log.error("insert parameter '" + parameter.getId() + "' failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<CloudgeneParameterInput> findAllInputByJob(AbstractJob job) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from parameter ");
		sql.append("where job_id = ? and input = true");

		Object[] params = new Object[1];
		params[0] = job.getId();

		List<CloudgeneParameterInput> result = new Vector<CloudgeneParameterInput>();

		try {

			result = query(sql.toString(), params, new ParameterInputMapper());

			log.debug("find all input parameters for job '" + job.getId() + "' successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all input parameters for job '" + job.getId() + "' failed.", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<CloudgeneParameterOutput> findAllOutputByJob(AbstractJob job) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from parameter ");
		sql.append("where job_id = ? and input = false");

		Object[] params = new Object[1];
		params[0] = job.getId();

		List<CloudgeneParameterOutput> result = new Vector<CloudgeneParameterOutput>();

		try {

			result = query(sql.toString(), params, new ParameterOutputMapper());

			DownloadDao downloadDao = new DownloadDao(database);
			for (CloudgeneParameterOutput parameter : result) {
				List<Download> downloads = downloadDao.findAllByParameter(parameter);
				for (Download download : downloads) {
					download.setUsername(job.getUser().getUsername());
				}
				parameter.setFiles(downloads);
			}

			log.debug("find all output parameters for job '" + job.getId() + "' successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all output parameters for job '" + job.getId() + "' failed.", e);
			return null;
		}
	}

	class ParameterInputMapper implements IRowMapper {

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			CloudgeneParameterInput parameter = new CloudgeneParameterInput();
			parameter.setDescription(rs.getString("name"));
			parameter.setValue(rs.getString("value"));
			parameter.setName(rs.getString("variable"));
			parameter.setJobId(rs.getString("job_id"));
			parameter.setType(WdlParameterInputType.getEnum(rs.getString("type")));
			parameter.setId(rs.getInt("id"));
			parameter.setAdminOnly(rs.getBoolean("admin_only"));
			return parameter;

		}

	}

	class ParameterOutputMapper implements IRowMapper {

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			CloudgeneParameterOutput parameter = new CloudgeneParameterOutput();
			parameter.setDescription(rs.getString("name"));
			parameter.setValue(rs.getString("value"));
			parameter.setName(rs.getString("variable"));
			parameter.setJobId(rs.getString("job_id"));
			parameter.setType(WdlParameterOutputType.getEnum(rs.getString("type")));
			parameter.setDownload(rs.getBoolean("download"));
			parameter.setId(rs.getInt("id"));
			parameter.setAdminOnly(rs.getBoolean("admin_only"));
			return parameter;

		}

	}

}
