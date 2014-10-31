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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.activity.Activity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.db.DbClient;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * since 5.0
 */
public class ComputationService implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(ComputationService.class);

  private final DbClient dbClient;
  private final ComputationStepRegistry stepRegistry;
  private final ActivityService activityService;

  public ComputationService(DbClient dbClient, ComputationStepRegistry stepRegistry, ActivityService activityService) {
    this.dbClient = dbClient;
    this.stepRegistry = stepRegistry;
    this.activityService = activityService;
  }

  public void analyzeReport(AnalysisReportDto report) {
    TimeProfiler profiler = new TimeProfiler(LOG).start(String.format("#%s - %s - Analysis report processing", report.getId(), report.getProjectKey()));

    // Synchronization of a lot of data can only be done with a batch session for the moment
    DbSession session = dbClient.openSession(true);

    ComponentDto project = findProject(report, session);

    try {
      report.succeed();
      for (ComputationStep step : stepRegistry.steps()) {
        TimeProfiler stepProfiler = new TimeProfiler(LOG).start(step.getDescription());
        step.execute(session, report, project);
        session.commit();
        stepProfiler.stop();
      }

    } catch (Exception exception) {
      report.fail();
      Throwables.propagate(exception);
    } finally {
      logActivity(session, report, project);
      session.commit();
      MyBatis.closeQuietly(session);
      profiler.stop();
    }
  }

  private ComponentDto findProject(AnalysisReportDto report, DbSession session) {
    return checkNotNull(dbClient.componentDao().getByKey(session, report.getProjectKey()));
  }

  private void logActivity(DbSession session, AnalysisReportDto report, ComponentDto project) {
    report.setFinishedAt(new Date(System2.INSTANCE.now()));
    activityService.write(session, Activity.Type.ANALYSIS_REPORT, new AnalysisReportLog(report, project));
  }
}
