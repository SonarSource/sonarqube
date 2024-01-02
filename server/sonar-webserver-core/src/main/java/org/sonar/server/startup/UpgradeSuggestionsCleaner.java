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
package org.sonar.server.startup;

import org.sonar.api.Startable;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskMessageType;

/**
 * Clean up messages (like removing upgrade suggestions after an edition upgrade)
 */
@ServerSide
public class UpgradeSuggestionsCleaner implements Startable {

  private static final Logger LOGGER = Loggers.get(UpgradeSuggestionsCleaner.class);

  private final DbClient dbClient;
  private final SonarRuntime sonarRuntime;

  public UpgradeSuggestionsCleaner(DbClient dbClient, SonarRuntime sonarRuntime) {
    this.dbClient = dbClient;
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void start() {
    if (sonarRuntime.getEdition() == SonarEdition.COMMUNITY) {
      return;
    }

    deleteUpgradeMessageDismissals();
  }

  private void deleteUpgradeMessageDismissals() {
    LOGGER.info("Dismissed messages cleanup");
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.userDismissedMessagesDao().deleteByType(dbSession, CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
      dbClient.ceTaskMessageDao().deleteByType(dbSession, CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
      dbSession.commit();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
