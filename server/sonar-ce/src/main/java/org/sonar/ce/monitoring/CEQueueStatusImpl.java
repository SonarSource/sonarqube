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
package org.sonar.ce.monitoring;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.server.property.InternalProperties;

import static com.google.common.base.Preconditions.checkArgument;

public class CEQueueStatusImpl implements CEQueueStatus {

  private final DbClient dbClient;
  private final System2 system;
  private final AtomicLong inProgress = new AtomicLong(0);
  private final AtomicLong error = new AtomicLong(0);
  private final AtomicLong success = new AtomicLong(0);
  private final AtomicLong processingTime = new AtomicLong(0);

  public CEQueueStatusImpl(DbClient dbClient, System2 system) {
    this.dbClient = dbClient;
    this.system = system;
  }

  @Override
  public long addInProgress() {
    return inProgress.incrementAndGet();
  }

  @Override
  public long addError(long processingTimeInMs) {
    addProcessingTime(processingTimeInMs);
    inProgress.decrementAndGet();
    return error.incrementAndGet();
  }

  @Override
  public long addSuccess(long processingTimeInMs) {
    addProcessingTime(processingTimeInMs);
    inProgress.decrementAndGet();
    return success.incrementAndGet();
  }

  private void addProcessingTime(long ms) {
    checkArgument(ms >= 0, "Processing time can not be < 0");
    processingTime.addAndGet(ms);
  }

  @Override
  public long getPendingCount() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.ceQueueDao().countByStatus(dbSession, CeQueueDto.Status.PENDING);
    }
  }

  @Override
  public Optional<Long> getLongestTimePending() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.ceQueueDao().selectCreationDateOfOldestPendingByMainComponentUuid(dbSession, null)
        .map(creationDate -> system.now() - creationDate);
    }
  }

  @Override
  public boolean areWorkersPaused() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<String> val = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.COMPUTE_ENGINE_PAUSE);
      return "true".equals(val.orElse(null));
    }
  }

  @Override
  public long getInProgressCount() {
    return inProgress.get();
  }

  @Override
  public long getErrorCount() {
    return error.get();
  }

  @Override
  public long getSuccessCount() {
    return success.get();
  }

  @Override
  public long getProcessingTime() {
    return processingTime.get();
  }
}
