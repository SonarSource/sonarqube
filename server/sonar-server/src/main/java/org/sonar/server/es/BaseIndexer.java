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
import org.sonar.api.ServerSide;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ServerSide
public abstract class BaseIndexer implements Startable {

  private final ThreadPoolExecutor executor;
  private final String indexName, typeName, dateFieldName;
  protected final EsClient esClient;
  private volatile long lastUpdatedAt = 0L;

  /**
   * Indexers are disabled during server startup, to avoid too many consecutive refreshes of the same index
   * An example is RegisterQualityProfiles. If {@link org.sonar.server.activity.index.ActivityIndexer} is enabled by
   * default during startup, then each new activated rule generates a bulk request with a single document and then
   * asks for index refresh -> big performance hit.
   *
   * Indices are populated and refreshed when all startup components have been executed. See
   * {@link org.sonar.server.search.IndexSynchronizer}
   */
  private boolean enabled = false;

  protected BaseIndexer(EsClient client, long threadKeepAliveSeconds, String indexName, String typeName,
    String dateFieldName) {
    this.indexName = indexName;
    this.typeName = typeName;
    this.dateFieldName = dateFieldName;
    this.esClient = client;
    this.executor = new ThreadPoolExecutor(0, 1,
      threadKeepAliveSeconds, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
  }

  public void index(final IndexerTask task) {
    if (enabled) {
      final long requestedAt = System.currentTimeMillis();
      Future submit = executor.submit(new Runnable() {
        @Override
        public void run() {
          if (requestedAt > lastUpdatedAt) {
            long l = task.index(lastUpdatedAt);
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
  }

  public void index() {
    index(new IndexerTask() {
      @Override
      public long index(long lastUpdatedAt) {
        return doIndex(lastUpdatedAt);
      }
    });
  }

  protected abstract long doIndex(long lastUpdatedAt);

  public BaseIndexer setEnabled(boolean b) {
    this.enabled = b;
    return this;
  }

  @Override
  public void start() {
    lastUpdatedAt = esClient.getMaxFieldValue(indexName, typeName, dateFieldName);
  }

  @Override
  public void stop() {
    executor.shutdown();
  }

  public interface IndexerTask {
    long index(long lastUpdatedAt);
  }

}
