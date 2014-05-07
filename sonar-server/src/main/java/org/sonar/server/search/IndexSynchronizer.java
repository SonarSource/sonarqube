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
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.db.Dao;

import java.io.Serializable;

public class IndexSynchronizer<K extends Serializable> {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSynchronizer.class);

  private final Index index;
  private final Dao<?,K> dao;
  private final WorkQueue workQueue;

  public IndexSynchronizer(Index index, Dao<?,K> dao,  WorkQueue workQueue) {
    this.index = index;
    this.dao = dao;
    this.workQueue = workQueue;
  }

  public IndexSynchronizer<K> start() {

//    LOG.info("Starting synchronization thread for ", index.getClass().getSimpleName());
//
//    Long since = index.getLastSynchronization();
//    index.setLastSynchronization(System.currentTimeMillis());
//
//    for (K key : dao.keysOfRowsUpdatedAfter(since)) {
//      if (LOG.isTraceEnabled()) {
//        LOG.trace("Adding {} to workQueue for {}", key, index.getClass().getSimpleName());
//      }
//      workQueue.enqueue(new KeyIndexAction<K>(index.getIndexName(), IndexAction.Method.INSERT, key));
//    }

    return this;
  }
}
