/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.analysis.cache.cleaning;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.LocalDateTime;
import org.sonar.api.platform.Server;
import org.sonar.db.DbClient;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class AnalysisCacheCleaningSchedulerImpl implements AnalysisCacheCleaningScheduler {
  private final AnalysisCacheCleaningExecutorService executorService;
  private final DbClient dbClient;

  public AnalysisCacheCleaningSchedulerImpl(AnalysisCacheCleaningExecutorService executorService, DbClient dbClient) {
    this.executorService = executorService;
    this.dbClient = dbClient;
  }

  @Override public void onServerStart(Server server) {
    LocalDateTime now = LocalDateTime.now();
    // schedule run at midnight everyday
    LocalDateTime nextRun = now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
    long initialDelay = Duration.between(now, nextRun).getSeconds();
    executorService.scheduleAtFixedRate(this::clean, initialDelay, DAYS.toSeconds(1), SECONDS);
  }

  @VisibleForTesting
  void clean() {
    try (var dbSession = dbClient.openSession(false)) {
      dbClient.scannerAnalysisCacheDao().cleanOlderThan7Days(dbSession);
      dbSession.commit();
    }
  }
}
