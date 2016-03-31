/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.queue;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.computation.taskprocessor.CeProcessingScheduler;

/**
 * Cleans-up the queue, initializes JMX counters then schedule
 * the execution of workers. That allows to not prevent workers
 * from peeking the queue before it's ready.
 */
@ComputeEngineSide
public class CeQueueInitializer implements ServerStartHandler {

  private final DbClient dbClient;
  private final CeQueueCleaner cleaner;
  private final CeProcessingScheduler scheduler;
  private boolean done = false;

  public CeQueueInitializer(DbClient dbClient, CeQueueCleaner cleaner, CeProcessingScheduler scheduler) {
    this.dbClient = dbClient;
    this.cleaner = cleaner;
    this.scheduler = scheduler;
  }

  @Override
  public void onServerStart(Server server) {
    if (!done) {
      initCe();
      this.done = true;
    }
  }

  private void initCe() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      cleaner.clean(dbSession);
      scheduler.startScheduling();

    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
