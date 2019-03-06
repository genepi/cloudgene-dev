package cloudgene.mapred.jobs;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.CounterDao;
import cloudgene.mapred.database.DownloadDao;
import cloudgene.mapred.database.JobDao;
import cloudgene.mapred.database.MessageDao;
import cloudgene.mapred.database.ParameterDao;
import cloudgene.mapred.database.StepDao;
import genepi.db.Database;

public class PersistentWorkflowEngine extends WorkflowEngine {

	private static final Logger log = LoggerFactory.getLogger(PersistentWorkflowEngine.class);

	private Database database;

	private JobDao dao;

	private CounterDao counterDao;

	private Map<String, Long> counters;

	public PersistentWorkflowEngine(Database database, int ltqThreads, int stqThreads) {
		super(ltqThreads, stqThreads);
		this.database = database;

		log.info("Init Counters....");

		counterDao = new CounterDao(database);
		counters = counterDao.getAll();

		dao = new JobDao(database);

		List<AbstractJob> deadJobs = dao.findAllByState(AbstractJob.STATE_WAITING);
		deadJobs.addAll(dao.findAllByState(AbstractJob.STATE_RUNNING));
		deadJobs.addAll(dao.findAllByState(AbstractJob.STATE_EXPORTING));

		for (AbstractJob job : deadJobs) {
			log.info("lost control over job " + job.getId() + " -> Dead");
			job.setState(AbstractJob.STATE_DEAD);
			dao.update(job);
		}

	}

	@Override
	protected void statusUpdated(AbstractJob job) {
		dao.update(job);
	}

	@Override
	protected void jobCompleted(AbstractJob job) {

		DownloadDao downloadDao = new DownloadDao(database);

		for (CloudgeneParameterOutput parameter : job.getOutputParams()) {

			if (parameter.isDownload()) {

				if (parameter.getFiles() != null) {

					for (Download download : parameter.getFiles()) {
						download.setParameterId(parameter.getId());
						download.setParameter(parameter);
						downloadDao.insert(download);
					}

				}

			}

		}

		if (job.getSteps() != null) {
			StepDao dao2 = new StepDao(database);
			for (CloudgeneStep step : job.getSteps()) {
				dao2.insert(step);

				MessageDao messageDao = new MessageDao(database);
				if (step.getLogMessages() != null) {
					for (Message logMessage : step.getLogMessages()) {
						messageDao.insert(logMessage);
					}
				}

			}
		}

		// write all submitted counters into database
		for (String name : job.getContext().getSubmittedCounters().keySet()) {
			Integer value = job.getContext().getSubmittedCounters().get(name);

			if (value != null) {

				Long counterValue = counters.get(name);
				if (counterValue == null) {
					counterValue = 0L + value;
				} else {
					counterValue = counterValue + value;
				}
				counters.put(name, counterValue);

				counterDao.insert(name, value, job);

			}
		}
		
		//update job updates (state, endtime, ....)
		dao.update(job);

	}

	@Override
	protected void jobSubmitted(AbstractJob job) {
		dao.insert(job);

		ParameterDao dao = new ParameterDao(database);

		for (CloudgeneParameterInput parameter : job.getInputParams()) {
			parameter.setJobId(job.getId());
			dao.insert(parameter);
		}

		for (CloudgeneParameterOutput parameter : job.getOutputParams()) {
			parameter.setJobId(job.getId());
			dao.insert(parameter);
		}
	}

	@Override
	public Map<String, Long> getCounters(int state) {
		if (state == AbstractJob.STATE_SUCCESS) {
			return counters;
		} else {
			return super.getCounters(state);
		}
	}

}
