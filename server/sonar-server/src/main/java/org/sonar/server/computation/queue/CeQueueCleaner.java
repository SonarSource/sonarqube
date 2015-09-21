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

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.ReportFiles;

/**
 * Cleans-up the Compute Engine queue and resets the JMX counters.
 * CE workers must not be started before execution of this class.
 */
@ServerSide
public class CeQueueCleaner {

  private static final Logger LOGGER = Loggers.get(CeQueueCleaner.class);

  private final DbClient dbClient;
  private final ServerUpgradeStatus serverUpgradeStatus;
  private final ReportFiles reportFiles;
  private final CeQueueImpl queue;

  public CeQueueCleaner(DbClient dbClient, ServerUpgradeStatus serverUpgradeStatus, ReportFiles reportFiles, CeQueueImpl queue) {
    this.dbClient = dbClient;
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.reportFiles = reportFiles;
    this.queue = queue;
  }

  public void clean(DbSession dbSession) {
    if (serverUpgradeStatus.isUpgraded()) {
      cleanOnUpgrade();
    } else {
      verifyConsistency(dbSession);
    }
  }

  private void cleanOnUpgrade() {
    // we assume that pending tasks are not compatible with the new version
    // and can't be processed
    LOGGER.info("Cancel all pending tasks (due to upgrade)");
    queue.clear();
  }

  private void verifyConsistency(DbSession dbSession) {
    // server is not being upgraded
    dbClient.ceQueueDao().resetAllToPendingStatus(dbSession);
    dbSession.commit();

    // verify that the report files are available for the tasks in queue
    Set<String> uuidsInQueue = new HashSet<>();
    for (CeQueueDto queueDto : dbClient.ceQueueDao().selectAllInAscOrder(dbSession)) {
      uuidsInQueue.add(queueDto.getUuid());
      if (CeTaskTypes.REPORT.equals(queueDto.getTaskType()) && !reportFiles.fileForUuid(queueDto.getUuid()).exists()) {
        // the report is not available on file system
        queue.cancel(dbSession, queueDto);
      }
    }

    // clean-up filesystem
    for (String uuid : reportFiles.listUuids()) {
      if (!uuidsInQueue.contains(uuid)) {
        reportFiles.deleteIfExists(uuid);
      }
    }
  }
}
