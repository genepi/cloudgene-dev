package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.AbstractJob;
import genepi.db.Database;
import genepi.db.IRowMapMapper;
import genepi.db.JdbcDataAccessObject;

public class CounterDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(CounterDao.class);

	public CounterDao(Database database) {
		super(database);
	}

	public boolean insert(String name, int value, AbstractJob job) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into counters (name, job_id, value) ");
		sql.append("values (?,?,?)");

		try {

			Object[] params = new Object[3];
			params[0] = name;
			params[1] = job.getId();
			params[2] = value;

			update(sql.toString(), params);

			log.debug("insert counter successful.");

		} catch (SQLException e) {
			log.error("insert counter failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Long> getAll() {

		StringBuilder sql = new StringBuilder();
		sql.append("select name, sum(value) ");
		sql.append("from counters ");
		sql.append("group by name");

		Map<String, Long> result = new HashMap<String, Long>();

		try {

			result = queryForMap(sql.toString(), new CounterMapper());

			log.debug("find counters successful. results: " + result);

			return result;
		} catch (SQLException e) {
			log.error("find all counters failed", e);
		}

		return result;
	}

	class CounterMapper implements IRowMapMapper {

		@Override
		public Object getRowKey(ResultSet rs, int row) throws SQLException {
			return rs.getString(1);
		}

		@Override
		public Object getRowValue(ResultSet rs, int row) throws SQLException {
			return rs.getLong(2);
		}

	}

}
