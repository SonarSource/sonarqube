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

import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.server.search.action.IndexAction;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IndexQueueWorker extends ThreadPoolExecutor
  implements ServerComponent, Startable {

  private static final Logger LOG = LoggerFactory.getLogger(IndexQueueWorker.class);

  private final IndexClient indexes;

  public IndexQueueWorker(IndexQueue queue, IndexClient indexes) {
    super(1,1, 0L, TimeUnit.MILLISECONDS, queue);
    this.indexes = indexes;
  }

  protected void beforeExecute(Thread t, Runnable r) {
    LOG.debug("Starting task: {}", r);
    super.beforeExecute(t, r);
    if (IndexAction.class.isAssignableFrom(r.getClass())) {
      IndexAction ia = (IndexAction) r;
      LOG.debug("Task is an IndexAction for {}", ia.getIndexType());
      ia.setIndex(indexes.getByType(ia.getIndexType()));
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
    this.prestartAllCoreThreads();
  }

  @Override
  public void stop() {
    this.shutdown();
  }
}
