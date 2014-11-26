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
package org.sonar.server.es;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class BaseIndexer implements ServerComponent, Startable {

  private final ThreadPoolExecutor executor;
  private final String indexName, typeName;
  protected final EsClient esClient;
  private volatile long lastUpdatedAt = 0L;

  protected BaseIndexer(EsClient client, long threadKeepAliveMilliseconds, String indexName, String typeName) {
    this.indexName = indexName;
    this.typeName = typeName;
    this.esClient = client;
    this.executor = new ThreadPoolExecutor(0, 1,
      threadKeepAliveMilliseconds, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
  }

  public void index() {
    final long requestedAt = System.currentTimeMillis();
    Future submit = executor.submit(new Runnable() {
      @Override
      public void run() {
        if (requestedAt > lastUpdatedAt) {
          long l = doIndex(lastUpdatedAt);
          // l can be 0 if no documents were indexed
          lastUpdatedAt = Math.max(l, lastUpdatedAt);
        }
      }
    });
    try {
      Uninterruptibles.getUninterruptibly(submit);
    } catch (ExecutionException e) {
      Throwables.propagate(e);
    }
  }

  protected abstract long doIndex(long lastUpdatedAt);

  @Override
  public void start() {
    lastUpdatedAt = esClient.getLastUpdatedAt(indexName, typeName);
  }

  @Override
  public void stop() {
    executor.shutdown();
  }

}
