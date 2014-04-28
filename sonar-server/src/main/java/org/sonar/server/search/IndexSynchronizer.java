/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.cluster.WorkQueue;

import java.io.Serializable;

public class IndexSynchronizer<K extends Serializable> {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSynchronizer.class);

  private static final Long DEFAULT_WAIT_TIME = 5000l;

  private long wait = 0;
  private boolean continuous;

  private final Index<K> index;
  private final WorkQueue workQueue;

  public static <K extends Serializable> IndexSynchronizer<K> getContinuousSynchronizer(Index<K> index, WorkQueue workQueue) {
    return new IndexSynchronizer<K>(index, workQueue)
      .setContinuous(true)
      .setWait(DEFAULT_WAIT_TIME);
  }

  public static <K extends Serializable> IndexSynchronizer<K> getContinuousSynchronizer(Index<K> index, WorkQueue workQueue, Long wait) {
    return new IndexSynchronizer<K>(index, workQueue)
      .setContinuous(true)
      .setWait(wait);
  }

  public static <K extends Serializable> IndexSynchronizer<K> getOnetimeSynchronizer(Index<K> index, WorkQueue workQueue) {
    return new IndexSynchronizer<K>(index, workQueue)
      .setContinuous(false);
  }

  private IndexSynchronizer(Index<K> index, WorkQueue workQueue) {
    this.index = index;
    this.workQueue = workQueue;
  }

  private IndexSynchronizer<K> setWait(Long wait) {
    this.wait = wait;
    return this;
  }

  private IndexSynchronizer<K> setContinuous(Boolean continuous) {
    this.continuous = continuous;
    return this;
  }

  public IndexSynchronizer<K> start() {

    LOG.info("Starting synchronization thread for ", index.getClass().getSimpleName());

    Long since = index.getLastSynchronization();
    index.setLastSynchronization(System.currentTimeMillis());

    for (K key : index.synchronizeSince(since)) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Adding {} to workQueue for {}", key, index.getClass().getSimpleName());
      }
      workQueue.enqueInsert(index.getIndexName(), key);
    }

    return this;
  }
}
