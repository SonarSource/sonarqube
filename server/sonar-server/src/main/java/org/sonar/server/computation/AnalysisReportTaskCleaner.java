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

package org.sonar.server.computation;

import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;

public class AnalysisReportTaskCleaner implements Startable, ServerComponent {
  private final ServerUpgradeStatus serverUpgradeStatus;
  private final DbClient dbClient;

  public AnalysisReportTaskCleaner(ServerUpgradeStatus serverUpgradeStatus, DbClient dbClient) {
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    DbSession session = dbClient.openSession(false);
    AnalysisReportDao dao = dbClient.analysisReportDao();

    try {
      if (serverUpgradeStatus.isUpgraded()) {
        dao.cleanWithTruncate(session);
      } else {
        dao.cleanWithUpdateAllToPendingStatus(session);
      }

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
