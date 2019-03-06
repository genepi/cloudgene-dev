package cloudgene.mapred.jobs.queue;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.AbstractJob;

public abstract class Queue implements Runnable {

	private List<AbstractJob> queue;

	private HashMap<AbstractJob, Future<?>> futures;

	private HashMap<AbstractJob, PriorityRunnable> runnables;

	private PriorityThreadPoolExecutor scheduler;

	private String name = "";

	private boolean updatePositions = false;

	private boolean priority = false;

	private static final Logger log = LoggerFactory.getLogger(Queue.class);

	public Queue(String name, int threads, boolean updatePositions, boolean priority) {
		this.name = name;
		this.updatePositions = updatePositions;
		this.priority = priority;
		futures = new HashMap<AbstractJob, Future<?>>();
		runnables = new HashMap<AbstractJob, PriorityRunnable>();
		queue = new Vector<AbstractJob>();
		scheduler = new PriorityThreadPoolExecutor(threads, priority);
	}

	public void submit(AbstractJob job) {

		synchronized (futures) {

			synchronized (queue) {

				PriorityRunnable runnable = createRunnable(job);
				runnables.put(job, runnable);

				Future<?> future = scheduler.submit(runnable);
				futures.put(job, future);
				queue.add(job);
				log.info(name + ": Submit job" + (priority ? " (P: " + job.getPriority() + ")" : "") + "...");

				if (priority) {
					// sorty by state and by priority
					Collections.sort(queue, new PriorityComparator());
				}

				if (updatePositions) {
					updatePositionInQueue();
				}

			}

		}

	}

	public void cancel(AbstractJob job) {

		if (job.getState() == AbstractJob.STATE_RUNNING || job.getState() == AbstractJob.STATE_EXPORTING) {

			log.info(name + ": Cancel Job " + job.getId() + "...");

			if (job.getSetupStartTime() > 0 && job.getSetupEndTime() == 0) {
				job.setSetupEndTime(System.currentTimeMillis());
			}

			job.kill();
			job.cancel();

			if (updatePositions) {
				updatePositionInQueue();
			}

		}

		if (job.getState() == AbstractJob.STATE_WAITING) {

			synchronized (futures) {

				synchronized (queue) {
					PriorityRunnable runnable = runnables.get(job);
					if (runnable != null) {
						scheduler.kill(runnable);
					}
					if (job.getSetupStartTime() > 0 && job.getSetupEndTime() == 0) {
						job.setSetupEndTime(System.currentTimeMillis());
					}

					job.cancel();
					queue.remove(job);
					futures.remove(job);
					onComplete(job);
					log.info(name + ": Cancel Job...");

					if (updatePositions) {
						updatePositionInQueue();
					}

				}

			}
		}

	}

	@Override
	public void run() {

		List<AbstractJob> complete = new Vector<AbstractJob>();

		while (true) {
			try {

				synchronized (futures) {

					synchronized (queue) {

						complete.clear();

						for (AbstractJob job : futures.keySet()) {
							Future<?> future = futures.get(job);
							if (future.isDone() || future.isCancelled()) {
								log.info(name + ": Job " + job.getId() + ": finished");
								queue.remove(job);
								complete.add(job);
							}

						}

						for (AbstractJob job : complete) {
							try {
								onComplete(job);
							} catch (Exception e) {
								log.warn(name + ": Job " + job.getId() + ": On complete failed. ", e);
							}
							futures.remove(job);
							if (updatePositions) {
								updatePositionInQueue();
							}
						}

					}
				}

			} catch (Exception e) {
				log.warn(name + ": Concurrency Exception!! ", e);
			}

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void pause() {
		log.info(name + ": Pause...");
		scheduler.pause();
	}

	public void resume() {
		log.info(name + ": Resume...");
		scheduler.resume();
	}

	public boolean isRunning() {
		return scheduler.isRunning();
	}

	public int getActiveCount() {
		return scheduler.getActiveCount();
	}

	public List<AbstractJob> getJobsByUser(User user) {

		List<AbstractJob> result = new Vector<AbstractJob>();

		synchronized (queue) {

			for (AbstractJob job : queue) {

				if (job.getUser().getId() == user.getId()) {
					result.add(job);
				}

			}

		}

		return result;
	}

	public List<AbstractJob> getAllJobs() {

		List<AbstractJob> result = new Vector<AbstractJob>();

		synchronized (queue) {

			for (AbstractJob job : queue) {

				result.add(job);

			}

		}

		return result;
	}

	public AbstractJob getJobById(String id) {

		synchronized (queue) {

			for (AbstractJob job : queue) {

				if (job.getId().equals(id)) {
					return job;
				}

			}

		}

		return null;
	}

	protected void updatePositionInQueue() {
		synchronized (queue) {
			int position = 0;
			for (AbstractJob job : queue) {
				job.setPositionInQueue(position);
				if (job.getState() == AbstractJob.STATE_WAITING) {
					position++;
				}
			}
		}
	}

	public boolean updatePriority(AbstractJob job, long priority) {
		log.info("Update priority");
		if (!this.priority) {
			return false;
		}

		if (!isInQueue(job)) {
			return false;
		}

		if (job.getState() != AbstractJob.STATE_WAITING) {
			return false;
		}

		synchronized (futures) {

			synchronized (queue) {
				Future<?> oldFuture = futures.get(job);
				if (oldFuture != null) {
					oldFuture.cancel(false);
				}
				job.setPriority(priority);
				Future<?> future = scheduler.resubmit(job);
				if (future != null) {
					futures.put(job, future);
					log.info(name + ": Update priority of " + job.getId()
							+ (this.priority ? " (P: " + job.getPriority() + ")" : "") + "...");

					// sorty by state and by priority
					Collections.sort(queue, new PriorityComparator());

					if (updatePositions) {
						updatePositionInQueue();
					}
					return true;
				}

			}
		}
		return false;

	}

	public boolean isInQueue(AbstractJob job) {

		synchronized (queue) {

			return queue.contains(job);

		}
	}

	protected class PriorityComparator implements Comparator<AbstractJob> {

		@Override
		public int compare(AbstractJob o1, AbstractJob o2) {

			if (o1.getState() != o2.getState()) {
				if (o1.getState() == AbstractJob.STATE_RUNNING) {
					return -1;
				} else {
					return 1;
				}
			}

			if (o1.getPriority() == o2.getPriority()) {
				return 0;
			} else {
				if (o1.getPriority() < o2.getPriority()) {
					return -1;
				} else {
					return 1;
				}
			}
		}
	}

	abstract public void onComplete(AbstractJob job);

	abstract public PriorityRunnable createRunnable(AbstractJob job);

}
