package cloudgene.mapred.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.queue.PriorityRunnable;
import cloudgene.mapred.jobs.queue.Queue;

public class WorkflowEngine implements Runnable {

	private Thread threadLongTimeQueue;

	private Thread threadShortTimeQueue;

	private Queue longTimeQueue;

	private Queue shortTimeQueue;

	private boolean running = false;

	private AtomicLong priorityCounter = new AtomicLong();

	private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

	public WorkflowEngine(int ltqThreads, int stqThreads) {

		shortTimeQueue = new Queue("ShortTimeQueue", stqThreads, false, false) {

			@Override
			public PriorityRunnable createRunnable(AbstractJob job) {
				return new SetupThread(job);
			}

			@Override
			public void onComplete(AbstractJob job) {

				if (job.isSetupComplete()) {

					if (job.hasSteps()) {
						statusUpdated(job);
						longTimeQueue.submit(job);
					} else {
						job.setFinishedOn(System.currentTimeMillis());
						jobCompleted(job);
						job.setComplete(true);
					}

				} else {
					log.info("Setup failed for Job " + job.getId() + ". Not added to Long Time Queue.");
					job.setFinishedOn(System.currentTimeMillis());
					jobCompleted(job);
					job.setComplete(true);
				}

			}

		};

		longTimeQueue = new Queue("LongTimeQueue", ltqThreads, true, true) {

			@Override
			public PriorityRunnable createRunnable(AbstractJob job) {
				return job;
			}

			@Override
			public void onComplete(AbstractJob job) {
				job.setFinishedOn(System.currentTimeMillis());
				jobCompleted(job);
				job.setComplete(true);
			}

		};

	}

	public void submit(AbstractJob job) {
		submit(job, priorityCounter.incrementAndGet());
	}

	public void submit(AbstractJob job, long priority) {

		job.setPriority(priority);
		job.setSubmittedOn(System.currentTimeMillis());
		jobSubmitted(job);

		boolean okey = job.afterSubmission();
		if (okey) {
			shortTimeQueue.submit(job);
		} else {
			job.setFinishedOn(System.currentTimeMillis());
			statusUpdated(job);
			job.setComplete(true);			

		}
	}

	public void restart(AbstractJob job) {
		restart(job, priorityCounter.incrementAndGet());
	}

	public void restart(AbstractJob job, long priority) {

		job.setPriority(priority);
		job.setSubmittedOn(System.currentTimeMillis());
		job.setStartTime(0);
		job.setEndTime(0);
		job.setSetupStartTime(0);
		job.setSetupEndTime(0);
		job.setFinishedOn(0);
		job.setState(AbstractJob.STATE_WAITING);
		statusUpdated(job);

		boolean okey = job.afterSubmission();
		if (okey) {
			shortTimeQueue.submit(job);
		} else {
			job.setFinishedOn(System.currentTimeMillis());
			statusUpdated(job);
			job.setComplete(true);

		}

	}

	public void updatePriority(AbstractJob job, long priority) {
		longTimeQueue.updatePriority(job, priority);
	}

	public void cancel(AbstractJob job) {

		if (shortTimeQueue.isInQueue(job)) {
			shortTimeQueue.cancel(job);
		}

		if (longTimeQueue.isInQueue(job)) {
			longTimeQueue.cancel(job);
		}

	}

	@Override
	public void run() {
		threadShortTimeQueue = new Thread(shortTimeQueue);
		threadShortTimeQueue.start();
		threadLongTimeQueue = new Thread(longTimeQueue);
		threadLongTimeQueue.start();
		running = true;

	}

	public void stop() {
		threadShortTimeQueue.stop();
		threadLongTimeQueue.stop();
	}

	public void block() {
		shortTimeQueue.pause();
		longTimeQueue.pause();
		running = false;
	}

	public void resume() {
		shortTimeQueue.resume();
		longTimeQueue.resume();
		running = true;
	}

	public boolean isRunning() {
		return running && shortTimeQueue.isRunning() && longTimeQueue.isRunning();
	}

	public int getActiveCount() {
		return shortTimeQueue.getActiveCount() + longTimeQueue.getActiveCount();
	}

	public AbstractJob getJobById(String id) {
		AbstractJob job = longTimeQueue.getJobById(id);
		if (job == null) {
			job = shortTimeQueue.getJobById(id);
		}
		return job;
	}

	public Map<String, Long> getCounters(int state) {

		Map<String, Long> result = new HashMap<String, Long>();
		List<AbstractJob> jobs = longTimeQueue.getAllJobs();
		for (AbstractJob job : jobs) {
			if (job.getState() == state) {
				Map<String, Integer> counters = job.getContext().getCounters();
				for (String name : counters.keySet()) {
					Integer value = counters.get(name);
					Long oldvalue = result.get(name);
					if (oldvalue == null) {
						oldvalue = new Long(0);
					}
					result.put(name, oldvalue + value);
				}
			}
		}
		return result;

	}

	public List<AbstractJob> getJobsByUser(User user) {

		List<AbstractJob> jobs = shortTimeQueue.getJobsByUser(user);
		jobs.addAll(longTimeQueue.getJobsByUser(user));

		for (AbstractJob job : jobs) {

			if (job instanceof CloudgeneJob) {

				((CloudgeneJob) job).updateProgress();

			}

		}

		return jobs;
	}

	public List<AbstractJob> getAllJobsInShortTimeQueue() {

		List<AbstractJob> jobs = shortTimeQueue.getAllJobs();

		for (AbstractJob job : jobs) {

			if (job instanceof CloudgeneJob) {

				((CloudgeneJob) job).updateProgress();

			}

		}

		return shortTimeQueue.getAllJobs();
	}

	public List<AbstractJob> getAllJobsInLongTimeQueue() {

		List<AbstractJob> jobs = longTimeQueue.getAllJobs();

		for (AbstractJob job : jobs) {

			if (job instanceof CloudgeneJob) {

				((CloudgeneJob) job).updateProgress();

			}

		}

		return jobs;
	}

	class SetupThread extends PriorityRunnable {

		private AbstractJob job;

		public SetupThread(AbstractJob job) {
			this.job = job;
		}

		@Override
		public void run() {
			log.info("Start input validation for job " + job.getId() + "...");
			job.setSetupStartTime(System.currentTimeMillis());
			job.setSetupRunning(true);
			job.runSetupSteps();
			job.setSetupEndTime(System.currentTimeMillis());
			job.setSetupRunning(false);
			log.info("Input Validation for job " + job.getId() + " finished. Result: " + job.isSetupComplete());
		}

	}

	public boolean isInQueue(AbstractJob job) {
		if (!shortTimeQueue.isInQueue(job)) {
			return longTimeQueue.isInQueue(job);
		} else {
			return true;
		}
	}

	protected void statusUpdated(AbstractJob job) {

	}

	protected void jobCompleted(AbstractJob job) {

	}

	protected void jobSubmitted(AbstractJob job) {

	}

}