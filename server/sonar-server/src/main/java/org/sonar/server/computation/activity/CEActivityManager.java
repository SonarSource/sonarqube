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
package org.sonar.server.computation.activity;

import javax.annotation.CheckForNull;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.db.DbClient;

import static org.sonar.api.utils.DateUtils.formatDateTimeNullSafe;
import static org.sonar.api.utils.DateUtils.longToDate;

public class CEActivityManager {
  private final ActivityService activityService;
  private final DbClient dbClient;

  public CEActivityManager(ActivityService activityService, DbClient dbClient) {
    this.activityService = activityService;
    this.dbClient = dbClient;
  }

  public void saveActivity(AnalysisReportDto report) {
    ComponentDto project = loadProject(report.getProjectKey());
    Activity activity = new Activity();
    activity.setType(Activity.Type.ANALYSIS_REPORT);
    activity.setAction("LOG_ANALYSIS_REPORT");
    activity
        .setData("key", String.valueOf(report.getId()))
        .setData("projectKey", report.getProjectKey())
        .setData("status", String.valueOf(report.getStatus()))
        .setData("submittedAt", formatDateTimeNullSafe(longToDate(report.getCreatedAt())))
        .setData("startedAt", formatDateTimeNullSafe(longToDate(report.getStartedAt())))
        .setData("finishedAt", formatDateTimeNullSafe(longToDate(report.getFinishedAt())));
    if (project != null) {
      activity
          .setData("projectName", project.name())
          .setData("projectUuid", project.uuid());
    }
    activityService.save(activity);
  }

  @CheckForNull
  private ComponentDto loadProject(String projectKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().selectNullableByKey(session, projectKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
