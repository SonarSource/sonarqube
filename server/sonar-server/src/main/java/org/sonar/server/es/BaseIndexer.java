/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.picocontainer.Startable;
import org.sonar.api.utils.System2;

public abstract class BaseIndexer implements Startable {

  private final System2 system2;
  private final ThreadPoolExecutor executor;
  private final IndexType indexType;
  protected final EsClient esClient;
  private final String dateFieldName;
  private volatile long lastUpdatedAt = -1L;

  protected BaseIndexer(System2 system2, EsClient client, long threadKeepAliveSeconds, IndexType indexType,
    String dateFieldName) {
    this.system2 = system2;
    this.indexType = indexType;
    this.dateFieldName = dateFieldName;
    this.esClient = client;
    this.executor = new ThreadPoolExecutor(0, 1,
      threadKeepAliveSeconds, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
  }

  public void index(final IndexerTask task) {
    final long requestedAt = system2.now();
    Future submit = executor.submit(() -> {
      if (lastUpdatedAt == -1L) {
        lastUpdatedAt = esClient.getMaxFieldValue(indexType, dateFieldName);
      }
      if (requestedAt > lastUpdatedAt) {
        long l = task.index(lastUpdatedAt);
        // l can be 0 if no documents were indexed
        lastUpdatedAt = Math.max(l, lastUpdatedAt);
      }
    });
    try {
      Uninterruptibles.getUninterruptibly(submit);
    } catch (ExecutionException e) {
      Throwables.propagate(e);
    }
  }

  public void index() {
    index(this::doIndex);
  }

  protected abstract long doIndex(long lastUpdatedAt);

  @Override
  public void start() {
    // nothing to do at startup
  }

  @Override
  public void stop() {
    executor.shutdown();
  }

  @FunctionalInterface
  public interface IndexerTask {
    long index(long lastUpdatedAt);
  }

}
