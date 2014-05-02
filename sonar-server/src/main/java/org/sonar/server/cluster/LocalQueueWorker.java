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
package org.sonar.server.cluster;

import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.server.search.Index;
import org.sonar.server.search.IndexAction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LocalQueueWorker extends ThreadPoolExecutor
  implements ServerComponent, Startable {

  private static final Logger LOG = LoggerFactory.getLogger(LocalQueueWorker.class);

  private Map<String, Index> indexes;

  public LocalQueueWorker(LocalNonBlockingWorkQueue queue, Index... allIndexes) {
    super(10, 10, 500l, TimeUnit.MILLISECONDS, queue);

    /* Save all instances of Index<?> */
    this.indexes = new HashMap<String, Index>();
    for (Index index : allIndexes) {
      this.indexes.put(index.getIndexName(), index);
    }
  }

  protected void beforeExecute(Thread t, Runnable r) {
    LOG.debug("Starting task: {}", r);
    super.beforeExecute(t, r);
    if (IndexAction.class.isAssignableFrom(r.getClass())) {
      IndexAction ia = (IndexAction) r;
      LOG.debug("Task is an IndexAction for {}", ia.getIndexName());
      ia.setIndex(indexes.get(ia.getIndexName()));
    }
  }

  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    if (t != null) {
      throw new IllegalStateException(t);
    }
  }

  @Override
  public void start() {
    this.prestartCoreThread();
  }

  @Override
  public void stop() {
    this.shutdown();
  }
}
