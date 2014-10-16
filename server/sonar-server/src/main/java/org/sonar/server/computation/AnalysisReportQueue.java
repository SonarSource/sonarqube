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

import org.sonar.api.ServerComponent;
import org.sonar.core.activity.Activity;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;

public class AnalysisReportQueue implements ServerComponent {
  private final DbClient dbClient;
  private final AnalysisReportDao dao;
  private final ActivityService activityService;

  public AnalysisReportQueue(DbClient dbClient, ActivityService activityService) {
    this.dbClient = dbClient;
    this.activityService = activityService;
    this.dao = dbClient.analysisReportDao();
  }

  public AnalysisReportDto add(String projectKey) {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SCAN_EXECUTION);

    AnalysisReportDto report = newPendingAnalysisReport(projectKey);
    DbSession session = dbClient.openSession(false);
    try {
      checkThatProjectExistsInDatabase(projectKey, session);
      return insertInDatabase(report, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private AnalysisReportDto newPendingAnalysisReport(String projectKey) {
    return new AnalysisReportDto()
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setStatus(PENDING);
  }

  private void checkThatProjectExistsInDatabase(String projectKey, DbSession session) {
    dbClient.componentDao().getAuthorizedComponentByKey(projectKey, session);
  }

  private AnalysisReportDto insertInDatabase(AnalysisReportDto reportTemplate, DbSession session) {
    AnalysisReportDto report = dao.insert(session, reportTemplate);
    session.commit();

    return report;
  }

  public void remove(AnalysisReportDto report) {
    checkArgument(report.getStatus().isInFinalState());

    DbSession session = dbClient.openSession(false);

    try {
      dao.delete(session, report);
      logActivity(report, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void logActivity(AnalysisReportDto report, DbSession session) {
    activityService.write(session, Activity.Type.ANALYSIS_REPORT, new AnalysisReportLog(report));
  }

  /**
   * @return a booked analysis report if one is available, null otherwise
   */
  @CheckForNull
  public synchronized AnalysisReportDto bookNextAvailable() {
    DbSession session = dbClient.openSession(false);
    try {
      AnalysisReportDto nextAvailableReport = dao.getNextAvailableReport(session);
      if (nextAvailableReport == null) {
        return null;
      }

      AnalysisReportDto report = dao.bookAnalysisReport(session, nextAvailableReport);
      session.commit();

      return report;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<AnalysisReportDto> findByProjectKey(String projectKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return dao.findByProjectKey(session, projectKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<AnalysisReportDto> all() {
    DbSession session = dbClient.openSession(false);
    try {
      return dao.findAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
