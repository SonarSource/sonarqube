/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.pushapi.scheduler.purge;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.util.GlobalLockManager;

import static java.util.concurrent.TimeUnit.SECONDS;

@ServerSide
public class PushEventsPurgeScheduler implements Startable {
  private static final Logger LOG = Loggers.get(PushEventsPurgeScheduler.class);
  private static final String LOCK_NAME = "PushPurgeCheck";

  @VisibleForTesting
  static final String INITIAL_DELAY_IN_SECONDS = "sonar.push.events.purge.initial.delay";
  @VisibleForTesting
  static final String ENQUEUE_DELAY_IN_SECONDS = "sonar.push.events.purge.enqueue.delay";

  private static final int ENQUEUE_LOCK_DELAY_IN_SECONDS = 60;

  private final DbClient dbClient;
  private final Configuration config;
  private final GlobalLockManager lockManager;
  private final PushEventsPurgeExecutorService executorService;
  private final System2 system;

  public PushEventsPurgeScheduler(DbClient dbClient, Configuration config, GlobalLockManager lockManager,
    PushEventsPurgeExecutorService executorService, System2 system) {
    this.dbClient = dbClient;
    this.executorService = executorService;
    this.config = config;
    this.lockManager = lockManager;
    this.system = system;
  }

  @Override
  public void start() {
    executorService.scheduleAtFixedRate(this::checks, getInitialDelay(),
      getEnqueueDelay(), SECONDS);
  }

  private void checks() {
    try {
      // Avoid enqueueing push events purge task multiple times
      if (!lockManager.tryLock(LOCK_NAME, ENQUEUE_LOCK_DELAY_IN_SECONDS)) {
        return;
      }
      purgeExpiredPushEvents();
    } catch (Exception e) {
      LOG.error("Error in Push Events Purge scheduler", e);
    }
  }

  private void purgeExpiredPushEvents() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> uuids = dbClient.pushEventDao().selectUuidsOfExpiredEvents(dbSession, getExpiredTimestamp());
      LOG.debug(String.format("%s push events to be deleted...", uuids.size()));
      dbClient.pushEventDao().deleteByUuids(dbSession, uuids);
      dbSession.commit();
    }
  }

  public long getInitialDelay() {
    return config.getLong(INITIAL_DELAY_IN_SECONDS).orElse(60 * 60L);
  }

  public long getEnqueueDelay() {
    return config.getLong(ENQUEUE_DELAY_IN_SECONDS).orElse(60 * 60L);
  }

  private long getExpiredTimestamp() {
    return Instant.ofEpochMilli(system.now())
      .minus(1, ChronoUnit.HOURS)
      .toEpochMilli();
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
