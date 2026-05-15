/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.cleaning;

import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.CeDistributedInformation;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeProfiler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static java.util.concurrent.TimeUnit.SECONDS;

public class CeActivitiesPurgeSchedulerImpl implements CeActivitiesPurgeScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(CeActivitiesPurgeSchedulerImpl.class);

  static final int SCHEDULE_HOUR = 23;

  private final CeActivitiesPurgeExecutorService executorService;
  private final CeDistributedInformation ceDistributedInformation;
  private final DbClient dbClient;
  private final PurgeProfiler profiler;

  public CeActivitiesPurgeSchedulerImpl(CeActivitiesPurgeExecutorService executorService,
    CeDistributedInformation ceDistributedInformation, DbClient dbClient, PurgeProfiler profiler) {
    this.executorService = executorService;
    this.ceDistributedInformation = ceDistributedInformation;
    this.dbClient = dbClient;
    this.profiler = profiler;
  }

  @Override
  public void startScheduling() {
    Instant now = Instant.now();
    Instant nextRun = LocalDate.now(ZoneOffset.UTC).atTime(LocalTime.of(SCHEDULE_HOUR, 0)).toInstant(ZoneOffset.UTC);
    if (!now.isBefore(nextRun)) {
      nextRun = nextRun.plus(Duration.ofDays(1));
    }
    long initialDelaySeconds = Duration.between(now, nextRun).getSeconds();
    executorService.scheduleWithFixedDelay(this::purgeCeActivities, initialDelaySeconds, Duration.ofDays(1).getSeconds(), SECONDS);
  }

  void purgeCeActivities() {
    Lock lock = null;
    boolean locked = false;
    try {
      lock = ceDistributedInformation.acquireCleanJobLock();
      locked = lock.tryLock();
      if (locked) {
        purge();
      }
    } catch (Exception e) {
      LOG.warn("Failed to run scheduled CE activities purge. Will be retried on next run.", e);
    } finally {
      if (locked) {
        lock.unlock();
      }
    }
  }

  private void purge() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.purgeDao().purgeCeActivities(dbSession, profiler);
      dbClient.purgeDao().purgeCeScannerContexts(dbSession, profiler);
      dbSession.commit();
    } catch (Exception e) {
      LOG.warn("Failed to purge CE activities during scheduled run. Will be retried on next run.", e);
    }
  }
}
