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
package org.sonar.server.computation.queue;

import org.picocontainer.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.computation.monitoring.CEQueueStatus;

/**
 * Cleans-up the queue, initializes JMX counters then schedule
 * the execution of workers. That allows to not prevent workers
 * from peeking the queue before it's ready.
 */
@ServerSide
public class CeQueueInitializer implements Startable {

  private final DbClient dbClient;
  private final CEQueueStatus queueStatus;
  private final CeQueueCleaner cleaner;
  private final CeProcessingScheduler scheduler;

  public CeQueueInitializer(DbClient dbClient, CEQueueStatus queueStatus, CeQueueCleaner cleaner, CeProcessingScheduler scheduler) {
    this.dbClient = dbClient;
    this.queueStatus = queueStatus;
    this.cleaner = cleaner;
    this.scheduler = scheduler;
  }

  @Override
  public void start() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      initJmxCounters(dbSession);
      cleaner.clean(dbSession);
      scheduler.startScheduling();

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void initJmxCounters(DbSession dbSession) {
    queueStatus.initPendingCount(dbClient.ceQueueDao().countAll(dbSession));
  }
}
