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
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.activity.Activity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.db.DbClient;
import org.sonar.server.properties.ProjectSettingsFactory;

public class ComputationService implements ServerComponent {

  private static final Logger LOG = Loggers.get(ComputationService.class);

  private final DbClient dbClient;
  private final ComputationSteps steps;
  private final ActivityService activityService;
  private final ProjectSettingsFactory projectSettingsFactory;

  public ComputationService(DbClient dbClient, ComputationSteps steps, ActivityService activityService, ProjectSettingsFactory projectSettingsFactory) {
    this.dbClient = dbClient;
    this.steps = steps;
    this.activityService = activityService;
    this.projectSettingsFactory = projectSettingsFactory;
  }

  public void process(AnalysisReportDto report) {
    Profiler profiler = Profiler.create(LOG).startInfo(String.format(
      "#%s - %s - processing analysis report", report.getId(), report.getProjectKey()));

    ComponentDto project = loadProject(report);
    try {
      ComputationContext context = new ComputationContext(report, project);
      context.setProjectSettings(projectSettingsFactory.newProjectSettings(dbClient.openSession(false), project.getId()));
      for (ComputationStep step : steps.orderedSteps()) {
        if (ArrayUtils.contains(step.supportedProjectQualifiers(), context.getProject().qualifier())) {
          Profiler stepProfiler = Profiler.create(LOG).startInfo(step.getDescription());
          step.execute(context);
          stepProfiler.stopInfo();
        }
      }
      report.succeed();

    } catch (Exception e) {
      report.fail();
      throw Throwables.propagate(e);

    } finally {
      logActivity(report, project);
      profiler.stopInfo();
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
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
