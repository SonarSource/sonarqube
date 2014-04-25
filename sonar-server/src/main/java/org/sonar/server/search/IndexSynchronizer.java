package org.sonar.server.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.cluster.WorkQueue;

import java.util.Date;

public class IndexSynchronizer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSynchronizer.class);

  private static final Long DEFAULT_WAIT_TIME = 5000l;

  private long wait = 0;
  private boolean continuous;

  private final Index<?> index;
  private final WorkQueue workQueue;

  public static IndexSynchronizer getContinuousSynchronizer(Index<?> index, WorkQueue workQueue) {
    return new IndexSynchronizer(index, workQueue)
      .setContinuous(true)
      .setWait(DEFAULT_WAIT_TIME);
  }

  public static IndexSynchronizer getContinuousSynchronizer(Index<?> index, WorkQueue workQueue, Long wait) {
    return new IndexSynchronizer(index, workQueue)
      .setContinuous(true)
      .setWait(wait);
  }

  public static IndexSynchronizer getOnetimeSynchronizer(Index<?> index, WorkQueue workQueue) {
    return new IndexSynchronizer(index, workQueue)
      .setContinuous(false);
  }

  private IndexSynchronizer(Index<?> index, WorkQueue workQueue) {
    this.index = index;
    this.workQueue = workQueue;
  }

  private IndexSynchronizer setWait(Long wait) {
    this.wait = wait;
    return this;
  }

  private IndexSynchronizer setContinuous(Boolean continuous) {
    this.continuous = continuous;
    return this;
  }

  public IndexSynchronizer start() {

    LOG.info("Starting synchronization thread for ", index.getClass().getSimpleName());

    Long since = index.getLastSynchronization();
    index.setLastSynchronization(System.currentTimeMillis());

    for (Object key : index.synchronizeSince(since)) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Adding {} to workQueue for {}", key, index.getClass().getSimpleName());
      }
      workQueue.enqueInsert(index.getIndexName(), key);
    }

    return this;
  }
}
