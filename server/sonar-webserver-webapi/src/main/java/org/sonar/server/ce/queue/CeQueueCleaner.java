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
package org.sonar.server.ce.queue;

import java.util.List;
import org.sonar.api.Startable;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.server.ServerSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.queue.CeQueue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * Cleans-up the Compute Engine queue.
 */
@ServerSide
public class CeQueueCleaner implements Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(CeQueueCleaner.class);

  private final DbClient dbClient;
  private final ServerUpgradeStatus serverUpgradeStatus;
  private final CeQueue queue;

  public CeQueueCleaner(DbClient dbClient, ServerUpgradeStatus serverUpgradeStatus, CeQueue queue) {
    this.dbClient = dbClient;
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.queue = queue;
  }

  @Override
  public void start() {
    if (serverUpgradeStatus.isUpgraded()) {
      cleanOnUpgrade();
    }
    cleanUpTaskInputOrphans();
  }

  private void cleanOnUpgrade() {
    // we assume that pending tasks are not compatible with the new version
    // and can't be processed
    LOGGER.info("Cancel all pending tasks (due to upgrade)");
    queue.clear();
  }

  private void cleanUpTaskInputOrphans() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      // Reports that have been processed are not kept in database yet.
      // They are supposed to be systematically dropped.
      // Let's clean-up orphans if any.
      List<String> uuids = dbClient.ceTaskInputDao().selectUuidsNotInQueue(dbSession);
      dbClient.ceTaskInputDao().deleteByUuids(dbSession, uuids);
      dbSession.commit();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
