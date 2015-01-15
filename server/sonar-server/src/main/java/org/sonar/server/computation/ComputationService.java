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

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.activity.Activity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.db.DbClient;

import java.io.File;

public class ComputationService implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(ComputationService.class);

  private final DbClient dbClient;
  private final ComputationSteps steps;
  private final ActivityService activityService;
  private final TempFolder tempFolder;

  public ComputationService(DbClient dbClient, ComputationSteps steps, ActivityService activityService,
    TempFolder tempFolder) {
    this.dbClient = dbClient;
    this.steps = steps;
    this.activityService = activityService;
    this.tempFolder = tempFolder;
  }

  public void process(AnalysisReportDto report) {
    TimeProfiler profiler = new TimeProfiler(LOG).start(String.format(
      "#%s - %s - processing analysis report", report.getId(), report.getProjectKey()));

    ComponentDto project = loadProject(report);
    File reportDir = tempFolder.newDir();
    try {
      ComputationContext context = new ComputationContext(report, project, reportDir);
      decompressReport(report, reportDir);
      for (ComputationStep step : steps.orderedSteps()) {
        TimeProfiler stepProfiler = new TimeProfiler(LOG).start(step.getDescription());
        step.execute(context);
        stepProfiler.stop();
      }
      report.succeed();

    } catch (Exception e) {
      report.fail();
      throw Throwables.propagate(e);

    } finally {
      FileUtils.deleteQuietly(reportDir);
      logActivity(report, project);
      profiler.stop();
    }
  }

  private ComponentDto loadProject(AnalysisReportDto report) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getByKey(session, report.getProjectKey());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void logActivity(AnalysisReportDto report, ComponentDto project) {
    DbSession session = dbClient.openSession(false);
    try {
      report.setFinishedAt(System2.INSTANCE.now());
      activityService.write(session, Activity.Type.ANALYSIS_REPORT, new AnalysisReportLog(report, project));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void decompressReport(AnalysisReportDto report, File toDir) {
    DbSession session = dbClient.openSession(false);
    try {
      dbClient.analysisReportDao().selectAndDecompressToDir(session, report.getId(), toDir);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
