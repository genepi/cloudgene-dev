package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao.UserMapper;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.CloudgeneParameterInput;
import cloudgene.mapred.jobs.CloudgeneParameterOutput;
import cloudgene.mapred.jobs.CloudgeneStep;
import genepi.db.Database;
import genepi.db.IRowMapper;
import genepi.db.JdbcDataAccessObject;

public class JobDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(JobDao.class);

	public JobDao(Database database) {
		super(database);
	}

	public boolean insert(AbstractJob job) {
		StringBuilder sql = new StringBuilder();
		sql.append(
				"insert into job (id, name, state, start_time, end_time, user_id, s3_url, type, application, application_id, submitted_on, finished_on, setup_start_time, setup_end_time) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		try {

			Object[] params = new Object[14];
			params[0] = job.getId();
			params[1] = job.getName();
			params[2] = job.getState();
			params[3] = job.getStartTime();
			params[4] = job.getEndTime();
			params[5] = job.getUser().getId();
			params[6] = "";
			params[7] = -1;
			params[8] = job.getApplication();
			params[9] = job.getApplicationId();
			params[10] = job.getSubmittedOn();
			params[11] = job.getFinishedOn();
			params[12] = job.getSetupStartTime();
			params[13] = job.getSetupEndTime();

			update(sql.toString(), params);

			log.debug("insert job '" + job.getId() + "' successful.");

		} catch (SQLException e) {
			log.error("insert job '" + job.getId() + "' failed.", e);
			return false;
		}

		return true;
	}

	public boolean update(AbstractJob job) {
		StringBuilder sql = new StringBuilder();
		sql.append("update job ");
		sql.append("  set name = ?, state = ?, ");
		sql.append("  start_time = ?, end_time = ?, ");
		sql.append(
				"  user_id = ?, s3_url = ?, type = ?, deleted_on = ?, application = ?, application_id = ?, submitted_on = ?, finished_on = ?, setup_start_time = ?, setup_end_time = ? ");
		sql.append("where id = ? ");
		try {

			Object[] params = new Object[15];
			params[0] = job.getName();
			params[1] = job.getState();
			params[2] = job.getStartTime();
			params[3] = job.getEndTime();
			params[4] = job.getUser().getId();
			params[5] = "";
			params[6] = -1;
			params[7] = job.getDeletedOn();
			params[8] = job.getApplication();
			params[9] = job.getApplicationId();
			params[10] = job.getSubmittedOn();
			params[11] = job.getFinishedOn();
			params[12] = job.getSetupStartTime();
			params[13] = job.getSetupEndTime();
			params[14] = job.getId();

			update(sql.toString(), params);

			log.debug("update job successful.");

		} catch (SQLException e) {
			log.error("update job '" + job.getId() + "' failed", e);
			return false;
		}

		return true;
	}

	public boolean updateUser(User oldUser, User newUser) {
		StringBuilder sql = new StringBuilder();
		sql.append("update job ");
		sql.append("set user_id = ?, name = ? ");
		sql.append("where  user_id = ?");
		try {

			Object[] params = new Object[3];
			params[0] = newUser.getId();
			params[1] = "";
			params[2] = oldUser.getId();

			update(sql.toString(), params);

			log.error("move all jobs from '" + oldUser.getUsername() + "' to '" + newUser.getUsername()
					+ "' successful.");

		} catch (SQLException e) {
			log.error("move all jobs from '" + oldUser.getUsername() + "' to '" + newUser.getUsername() + "' failed",
					e);
			return false;
		}

		return true;
	}

	public boolean delete(AbstractJob job) {
		StringBuilder sql = new StringBuilder();
		sql.append("delete from job ");
		sql.append("where id = ? ");
		try {

			Object[] params = new Object[1];
			params[0] = job.getId();

			update(sql.toString(), params);

			log.debug("delete job successful.");

		} catch (SQLException e) {
			log.error("delete job failed", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAllByUser(User user) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("where user_id = ? and state != ? ");
		sql.append("order by id desc ");

		Object[] params = new Object[2];
		params[0] = user.getId();
		params[1] = AbstractJob.STATE_DELETED;

		List<AbstractJob> result = new Vector<AbstractJob>();

		try {

			result = query(sql.toString(), params, new JobMapper());

			log.debug("find all jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all jobs failed", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAllByUser(User user, int offset, int limit) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("where user_id = ? and state != ? ");
		sql.append("order by id desc ");
		sql.append("limit ?,?");

		Object[] params = new Object[4];
		params[0] = user.getId();
		params[1] = AbstractJob.STATE_DELETED;
		params[2] = offset;
		params[3] = limit;

		List<AbstractJob> result = new Vector<AbstractJob>();

		try {

			result = query(sql.toString(), params, new JobMapper());

			log.debug("find all jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all jobs failed", e);
			return null;
		}
	}

	public int countAllByUser(User user) {

		StringBuilder sql = new StringBuilder();
		sql.append("select count(*) ");
		sql.append("from job ");
		sql.append("where user_id = ? and state != ? ");

		Object[] params = new Object[2];
		params[0] = user.getId();
		params[1] = AbstractJob.STATE_DELETED;

		int result = 0;

		try {

			result = (Integer) queryForObject(sql.toString(), params, new IntegerMapper());

			log.debug("count all jobs successful. results: " + result);

			return result;
		} catch (SQLException e) {
			log.error("count all jobs failed", e);
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAll() {
		// log.info("finding all jobs");

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("join user on job.user_id = user.id ");
		sql.append("order by job.id asc ");

		List<AbstractJob> result = new Vector<AbstractJob>();

		try {

			result = query(sql.toString(), new JobAndUserMapper());

			log.debug("find all jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all jobs failed", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAllNotRetiredJobs() {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("join user on job.user_id = user.id ");
		sql.append("where state != ? AND state != ? ");
		sql.append("order by job.id desc ");

		List<AbstractJob> result = new Vector<AbstractJob>();

		Object[] params = new Object[2];
		params[0] = AbstractJob.STATE_RETIRED;
		params[1] = AbstractJob.STATE_DELETED;

		try {

			result = query(sql.toString(), params, new JobAndUserMapper());

			log.debug("find all jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all jobs failed", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAllNotNotifiedJobs() {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("join user on job.user_id = user.id ");
		sql.append("where state != ? AND state != ? AND state != ? AND state != ? ");
		sql.append("order by job.id desc ");

		List<AbstractJob> result = new Vector<AbstractJob>();

		Object[] params = new Object[4];
		params[0] = AbstractJob.STATE_RETIRED;
		params[1] = AbstractJob.STATE_SUCESS_AND_NOTIFICATION_SEND;
		params[2] = AbstractJob.STATE_FAILED_AND_NOTIFICATION_SEND;
		params[3] = AbstractJob.STATE_DELETED;

		try {

			result = query(sql.toString(), params, new JobAndUserMapper());

			log.debug("find all jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all jobs failed", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAllNotifiedJobs() {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("join user on job.user_id = user.id ");
		sql.append("where  state = ? or state = ? ");
		sql.append("order by job.id desc ");

		Object[] params = new Object[2];
		params[0] = AbstractJob.STATE_SUCESS_AND_NOTIFICATION_SEND;
		params[1] = AbstractJob.STATE_FAILED_AND_NOTIFICATION_SEND;

		List<AbstractJob> result = new Vector<AbstractJob>();

		try {

			result = query(sql.toString(), params, new JobAndUserMapper());

			log.debug("find all old jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all old jobs failed", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAllOlderThan(long time, int state) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("join user on job.user_id = user.id ");
		sql.append("where  state = ? AND finished_on != 0 AND finished_on < ? ");
		sql.append("order by job.id desc ");

		Object[] params = new Object[2];
		params[0] = state;
		params[1] = time;

		List<AbstractJob> result = new Vector<AbstractJob>();

		try {

			result = query(sql.toString(), params, new JobAndUserMapper());

			log.debug("find all old jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all old jobs failed", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<AbstractJob> findAllByState(int state) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("join user on job.user_id = user.id ");
		sql.append("where state = ? ");
		sql.append("order by job.id desc ");

		Object[] params = new Object[1];
		params[0] = state;

		List<AbstractJob> result = new Vector<AbstractJob>();

		try {

			result = query(sql.toString(), params, new JobAndUserMapper());

			log.debug("find all old jobs successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all old jobs failed", e);
			return null;
		}
	}

	public AbstractJob findById(String id) {

		return findById(id, true);

	}

	public AbstractJob findById(String id, boolean loadParams) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from job ");
		sql.append("join user on job.user_id = user.id ");
		sql.append("where job.id = ? and state != ? ");

		Object[] params = new Object[2];
		params[0] = id;
		params[1] = AbstractJob.STATE_DELETED;

		AbstractJob job = null;

		try {

			job = (AbstractJob) queryForObject(sql.toString(), params, new JobAndUserMapper());

			if (loadParams && job != null) {

				ParameterDao parameterDao = new ParameterDao(database);
				List<CloudgeneParameterInput> inputParams = parameterDao.findAllInputByJob(job);
				List<CloudgeneParameterOutput> outputParams = parameterDao.findAllOutputByJob(job);
				job.setInputParams(inputParams);
				job.setOutputParams(outputParams);

				if (job instanceof CloudgeneJob) {

					StepDao stepDao = new StepDao(database);
					List<CloudgeneStep> steps = stepDao.findAllByJob((CloudgeneJob) job);
					job.setSteps(steps);

				}

			}

			if (job instanceof CloudgeneJob) {
				((CloudgeneJob) job).updateProgress();
			}

			if (job != null) {
				log.debug("find job by id '" + id + "' successful.");
			} else {
				log.debug("job '" + id + "' not found");
			}

			return job;
		} catch (SQLException e) {
			log.error("find job by id '" + id + "' failed", e);
			return null;
		}
	}

	class JobMapper implements IRowMapper {

		@Override
		public AbstractJob mapRow(ResultSet rs, int row) throws SQLException {

			AbstractJob job = new CloudgeneJob();
			job.setId(rs.getString("job.id"));
			job.setName(rs.getString("job.name"));
			job.setState(rs.getInt("job.state"));
			job.setStartTime(rs.getLong("job.start_time"));
			job.setEndTime(rs.getLong("job.end_time"));
			job.setDeletedOn(rs.getLong("job.deleted_on"));
			job.setApplication(rs.getString("job.application"));
			job.setApplicationId(rs.getString("job.application_id"));
			job.setSubmittedOn(rs.getLong("job.submitted_on"));
			job.setFinishedOn(rs.getLong("job.finished_on"));
			job.setSetupStartTime(rs.getLong("job.setup_start_time"));
			job.setSetupEndTime(rs.getLong("job.setup_end_time"));

			return job;
		}

	}

	class JobAndUserMapper implements IRowMapper {

		private JobMapper jobMaper = new JobMapper();

		private UserMapper userMapper = new UserMapper();

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {

			AbstractJob job = jobMaper.mapRow(rs, row);

			User user = userMapper.mapRow(rs, row);
			job.setUser(user);

			return job;
		}

	}

}
