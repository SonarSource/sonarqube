/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;

import static java.lang.String.format;

public class RecoveryIndexer implements Startable {

  private static final Logger LOGGER = Loggers.get(RecoveryIndexer.class);
  private static final String LOG_PREFIX = "Elasticsearch recovery - ";
  private static final String PROPERTY_INITIAL_DELAY = "sonar.search.recovery.initialDelayInMs";
  private static final String PROPERTY_DELAY = "sonar.search.recovery.delayInMs";
  private static final String PROPERTY_MIN_AGE = "sonar.search.recovery.minAgeInMs";
  private static final String PROPERTY_LOOP_LIMIT = "sonar.search.recovery.loopLimit";
  private static final long DEFAULT_DELAY_IN_MS = 5L * 60 * 1000;
  private static final long DEFAULT_MIN_AGE_IN_MS = 5L * 60 * 1000;
  private static final int DEFAULT_LOOP_LIMIT = 10_000;
  private static final double CIRCUIT_BREAKER_IN_PERCENT = 0.7;

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1,
    new ThreadFactoryBuilder()
      .setPriority(Thread.MIN_PRIORITY)
      .setNameFormat("RecoveryIndexer-%d")
      .build());
  private final System2 system2;
  private final Configuration config;
  private final DbClient dbClient;
  private final Map<String, Indexer> indexersByType;
  private final long minAgeInMs;
  private final long loopLimit;

  public RecoveryIndexer(System2 system2, Configuration config, DbClient dbClient, ResilientIndexer... indexers) {
    this.system2 = system2;
    this.config = config;
    this.dbClient = dbClient;
    this.indexersByType = new HashMap<>();
    Arrays.stream(indexers).forEach(i -> i.getIndexTypes().forEach(indexType -> indexersByType.put(indexType.format(), new Indexer(indexType, i))));
    this.minAgeInMs = getSetting(PROPERTY_MIN_AGE, DEFAULT_MIN_AGE_IN_MS);
    this.loopLimit = getSetting(PROPERTY_LOOP_LIMIT, DEFAULT_LOOP_LIMIT);
  }

  private static final class Indexer {
    private final IndexType indexType;
    private final ResilientIndexer delegate;

    private Indexer(IndexType indexType, ResilientIndexer delegate) {
      this.indexType = indexType;
      this.delegate = delegate;
    }

    public IndexType getIndexType() {
      return indexType;
    }

    public ResilientIndexer getDelegate() {
      return delegate;
    }
  }

  @Override
  public void start() {
    long delayInMs = getSetting(PROPERTY_DELAY, DEFAULT_DELAY_IN_MS);

    // in the cluster mode, avoid (but not prevent!) simultaneous executions of recovery
    // indexers so that a document is not handled multiple times.
    long initialDelayInMs = getSetting(PROPERTY_INITIAL_DELAY, RandomUtils.nextInt(1 + (int) (delayInMs / 2)));

    executorService.scheduleAtFixedRate(
      this::recover,
      initialDelayInMs,
      delayInMs,
      TimeUnit.MILLISECONDS);
  }

  @Override
  public void stop() {
    try {
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.error(LOG_PREFIX + "Unable to stop recovery indexer in timely fashion", e);
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  @VisibleForTesting
  void recover() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Profiler profiler = Profiler.create(LOGGER).start();
      long beforeDate = system2.now() - minAgeInMs;
      IndexingResult result = new IndexingResult();

      Collection<EsQueueDto> items = dbClient.esQueueDao().selectForRecovery(dbSession, beforeDate, loopLimit);
      while (!items.isEmpty()) {
        IndexingResult loopResult = new IndexingResult();

        groupItemsByDocType(items).asMap().forEach((type, typeItems) -> loopResult.add(doIndex(dbSession, type, typeItems)));
        result.add(loopResult);

        if (loopResult.getSuccessRatio() <= CIRCUIT_BREAKER_IN_PERCENT) {
          LOGGER.error(LOG_PREFIX + "too many failures [{}/{} documents], waiting for next run", loopResult.getFailures(), loopResult.getTotal());
          break;
        }

        if (loopResult.getTotal() == 0L) {
          break;
        }

        items = dbClient.esQueueDao().selectForRecovery(dbSession, beforeDate, loopLimit);
      }
      if (result.getTotal() > 0L) {
        profiler.stopInfo(LOG_PREFIX + format("%d documents processed [%d failures]", result.getTotal(), result.getFailures()));
      }
    } catch (Throwable t) {
      LOGGER.error(LOG_PREFIX + "fail to recover documents", t);
    }
  }

  private IndexingResult doIndex(DbSession dbSession, String docType, Collection<EsQueueDto> typeItems) {
    LOGGER.trace(LOG_PREFIX + "processing {} [{}]", typeItems.size(), docType);

    Indexer indexer = indexersByType.get(docType);
    if (indexer == null) {
      LOGGER.error(LOG_PREFIX + "ignore {} items with unsupported type [{}]", typeItems.size(), docType);
      return new IndexingResult();
    }
    return indexer.delegate.index(dbSession, typeItems);
  }

  private static ListMultimap<String, EsQueueDto> groupItemsByDocType(Collection<EsQueueDto> items) {
    return items.stream().collect(MoreCollectors.index(EsQueueDto::getDocType));
  }

  private long getSetting(String key, long defaultValue) {
    long val = config.getLong(key).orElse(defaultValue);
    LOGGER.debug(LOG_PREFIX + "{}={}", key, val);
    return val;
  }
}
