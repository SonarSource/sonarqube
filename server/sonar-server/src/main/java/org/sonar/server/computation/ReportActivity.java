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

import com.google.common.collect.ImmutableMap;
import org.sonar.core.activity.ActivityLog;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;

import java.util.Map;

import static org.sonar.api.utils.DateUtils.formatDateTimeNullSafe;
import static org.sonar.api.utils.DateUtils.longToDate;

public class ReportActivity implements ActivityLog {

  private static final String ACTION = "LOG_ANALYSIS_REPORT";

  private final AnalysisReportDto report;
  private final ComponentDto project;

  public ReportActivity(AnalysisReportDto report, ComponentDto project) {
    this.report = report;
    this.project = project;
  }

  @Override
  public Map<String, String> getDetails() {
    return ImmutableMap.<String, String>builder()
      .put("key", String.valueOf(report.getId()))
      .put("projectKey", project.key())
      .put("projectName", project.name())
      .put("projectUuid", project.uuid())
      .put("status", String.valueOf(report.getStatus()))
      .put("submittedAt", formatDateTimeNullSafe(longToDate(report.getCreatedAt())))
      .put("startedAt", formatDateTimeNullSafe(longToDate(report.getStartedAt())))
      .put("finishedAt", formatDateTimeNullSafe(longToDate(report.getFinishedAt())))
      .build();
  }

  @Override
  public String getAction() {
    return ACTION;
  }
}
