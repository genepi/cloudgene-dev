package cloudgene.mapred.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import genepi.db.Database;
import genepi.db.JdbcDataAccessObject;

public class CounterHistoryDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(CounterHistoryDao.class);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yy-MM-dd HH:mm");

	public CounterHistoryDao(Database database) {
		super(database);
	}

	public boolean insert(long timestamp, String name, long value) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into counters_history (time_stamp, name, value) ");
		sql.append("values (?,?,?)");

		try {

			Object[] params = new Object[3];
			params[0] = timestamp;
			params[1] = name;
			params[2] = value;

			update(sql.toString(), params);

			log.debug("insert counter history successful.");

		} catch (SQLException e) {
			log.error("insert counter history failed.", e);
			return false;
		}

		return true;
	}

	public List<Map<String, String>> getAll(int limit) {

		StringBuilder sql = new StringBuilder();
		sql.append("select time_stamp, name, value ");
		sql.append("from counters_history ");
		sql.append("order by time_stamp desc, name ");
		sql.append("limit " + limit);

		List<Map<String, String>> result = new Vector<Map<String, String>>();

		try {

			String old = "";
			Map<String, String> counters = null;

			Connection connection = database.getDataSource().getConnection();
			PreparedStatement statement = connection.prepareStatement(sql.toString());
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {

				if (!old.equals(rs.getString(1))) {
					counters = new HashMap<String, String>();
					result.add(counters);
					counters.put("timestamp",
							DATE_FORMAT.format(new Date(rs.getLong(1))));
					old = rs.getString(1);
				}
				counters.put(rs.getString(2), rs.getString(3));
			}
			rs.close();
			connection.close();

			log.debug("find counter history successful. results: "
					+ result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all counter history failed", e);
		}

		return result;
	}

	public List<Map<String, String>> getAllBeetween(long start, long end) {

		StringBuilder sql = new StringBuilder();
		sql.append("select time_stamp, name, value ");
		sql.append("from counters_history ");
		sql.append("where time_stamp > " + start + " and time_stamp < " + end
				+ " ");
		sql.append("order by time_stamp desc, name ");

		List<Map<String, String>> result = new Vector<Map<String, String>>();

		try {

			String old = "";
			Map<String, String> counters = null;
			Connection connection = database.getDataSource().getConnection();
			PreparedStatement statement = connection.prepareStatement(sql.toString());
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {

				if (!old.equals(rs.getString(1))) {
					counters = new HashMap<String, String>();
					result.add(counters);
					counters.put("timestamp",
							DATE_FORMAT.format(new Date(rs.getLong(1))));
					old = rs.getString(1);
				}
				counters.put(rs.getString(2), rs.getString(3));
			}
			rs.close();
			connection.close();

			log.debug("find counter history successful. results: "
					+ result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all counter history failed", e);
		}

		return result;
	}

}
