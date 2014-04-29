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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jfree.util.Log;
import org.sonar.core.cluster.IndexAction;
import org.sonar.server.search.Index;

import java.util.HashMap;
import java.util.Map;

import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.core.cluster.WorkQueue;

public class LocalQueueWorker implements ServerComponent, Startable {

  private static final Logger LOG = LoggerFactory.getLogger(LocalNonBlockingWorkQueue.class);

  private WorkQueue queue;

  private volatile Thread worker;
  private Map<String, Index<?>> indexes;

  class Worker implements Runnable {

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
      LOG.info("Starting Worker Thread");
      Thread thisThread = Thread.currentThread();
      while (worker == thisThread) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          Log.error("Oops");
        }

        @SuppressWarnings("rawtypes")
        IndexAction action = queue.dequeue();

        if (action != null && indexes.containsKey(action.getIndexName())) {
          indexes.get(action.getIndexName()).executeAction(action);
        }
      }
      LOG.info("Stoping Worker Thread");
    }
  }

  public LocalQueueWorker(WorkQueue queue, Index<?>... indexes) {
    this.queue = queue;
    this.worker = new Thread(new Worker());
    this.indexes = new HashMap<String, Index<?>>();
    for(Index<?> index:indexes){
      this.indexes.put(index.getIndexName(), index);
    }
  }


  @Override
  public void start() {
    this.worker.start();
  }

  @Override
  public void stop() {
    this.worker = null;
  }
}
