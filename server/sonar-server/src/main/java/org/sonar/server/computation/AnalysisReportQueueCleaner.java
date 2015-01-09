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

/**
 * Clean-up queue of reports at server startup:
 * <ul>
 *   <li>remove all reports if server being upgraded to a new version (we assume that
 *   format of reports is not forward-compatible)</li>
 *   <li>reset reports that were in status WORKING while server stopped</li>
 * </ul>
 */
public class AnalysisReportQueueCleaner implements Startable, ServerComponent {

  private final ServerUpgradeStatus serverUpgradeStatus;
  private final DbClient dbClient;

  public AnalysisReportQueueCleaner(ServerUpgradeStatus serverUpgradeStatus, DbClient dbClient) {
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    AnalysisReportDao dao = dbClient.analysisReportDao();
    DbSession session = dbClient.openSession(false);
    try {
      if (serverUpgradeStatus.isUpgraded()) {
        dao.truncate(session);
      } else {
        dao.resetAllToPendingStatus(session);
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
