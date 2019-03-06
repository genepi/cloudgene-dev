package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.jobs.Message;
import genepi.db.Database;
import genepi.db.IRowMapper;
import genepi.db.JdbcDataAccessObject;

public class MessageDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(MessageDao.class);

	public MessageDao(Database database) {
		super(database);
	}

	public boolean insert(Message logMessage) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into log_messages (time, type, message, step_id) ");
		sql.append("values (?,?,?,?)");

		try {

			Object[] params = new Object[4];
			params[0] = System.currentTimeMillis();
			params[1] = logMessage.getType();
			params[2] = logMessage.getMessage().substring(0,
					Math.min(logMessage.getMessage().length(), 1000));
			params[3] = logMessage.getStep().getId();
			update(sql.toString(), params);

			log.debug("insert log messages successful.");

		} catch (SQLException e) {
			log.error("insert log messages failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<Message> findAllByStep(CloudgeneStep step) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from log_messages ");
		sql.append("where step_id = ? ");
		sql.append("order by time ");

		Object[] params = new Object[1];
		params[0] = step.getId();

		List<Message> result = new Vector<Message>();

		try {

			result = query(sql.toString(), params, new MessageMapper(step));

			log.debug("find all log messages successful. results: "
					+ result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all log messages failed", e);
			return null;
		}
	}

	class MessageMapper implements IRowMapper {

		private CloudgeneStep step;

		public MessageMapper(CloudgeneStep step) {
			this.step = step;
		}

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			Message message = new Message();
			message.setTime(rs.getLong("time"));
			message.setStep(step);
			message.setType(rs.getInt("type"));
			message.setMessage(rs.getString("message"));
			return message;
		}

	}

}
