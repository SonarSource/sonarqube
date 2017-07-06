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

import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
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
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.user.index.UserIndexer;

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
  private static final double CIRCUIT_BREAKER_IN_PERCENT = 0.3;

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1,
    new ThreadFactoryBuilder()
      .setPriority(Thread.MIN_PRIORITY)
      .setNameFormat("RecoveryIndexer-%d")
      .build());
  private final System2 system2;
  private final Configuration config;
  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final RuleIndexer ruleIndexer;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final long minAgeInMs;
  private final long loopLimit;

  public RecoveryIndexer(System2 system2, Configuration config, DbClient dbClient,
    UserIndexer userIndexer, RuleIndexer ruleIndexer, ActiveRuleIndexer activeRuleIndexer) {
    this.system2 = system2;
    this.config = config;
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.ruleIndexer = ruleIndexer;
    this.activeRuleIndexer = activeRuleIndexer;
    this.minAgeInMs = getSetting(PROPERTY_MIN_AGE, DEFAULT_MIN_AGE_IN_MS);
    this.loopLimit = getSetting(PROPERTY_LOOP_LIMIT, DEFAULT_LOOP_LIMIT);
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

  void recover() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Profiler profiler = Profiler.create(LOGGER).start();
      long beforeDate = system2.now() - minAgeInMs;
      IndexingResult result = new IndexingResult();

      Collection<EsQueueDto> items = dbClient.esQueueDao().selectForRecovery(dbSession, beforeDate, loopLimit);
      while (!items.isEmpty()) {
        IndexingResult loopResult = new IndexingResult();

        ListMultimap<EsQueueDto.Type, EsQueueDto> itemsByType = groupItemsByType(items);
        for (Map.Entry<EsQueueDto.Type, Collection<EsQueueDto>> entry : itemsByType.asMap().entrySet()) {
          loopResult.add(doIndex(dbSession, entry.getKey(), entry.getValue()));
        }

        result.add(loopResult);
        if (loopResult.getFailureRatio() >= CIRCUIT_BREAKER_IN_PERCENT) {
          LOGGER.error(LOG_PREFIX + "too many failures [{}/{} documents], waiting for next run", loopResult.getFailures(), loopResult.getTotal());
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

  private IndexingResult doIndex(DbSession dbSession, EsQueueDto.Type type, Collection<EsQueueDto> typeItems) {
    LOGGER.trace(LOG_PREFIX + "processing {} {}", typeItems.size(), type);
    switch (type) {
      case USER:
        return userIndexer.index(dbSession, typeItems);
      case RULE_EXTENSION:
      case RULE:
        return ruleIndexer.index(dbSession, typeItems);
      case ACTIVE_RULE:
        return activeRuleIndexer.index(dbSession, typeItems);
      default:
        LOGGER.error(LOG_PREFIX + "ignore {} documents with unsupported type {}", typeItems.size(), type);
        return new IndexingResult();
    }
  }

  private static ListMultimap<EsQueueDto.Type, EsQueueDto> groupItemsByType(Collection<EsQueueDto> items) {
    return items.stream().collect(MoreCollectors.index(EsQueueDto::getDocType));
  }

  private long getSetting(String key, long defaultValue) {
    long val = config.getLong(key).orElse(defaultValue);
    LOGGER.debug(LOG_PREFIX + "{}={}", key, val);
    return val;
  }
}
