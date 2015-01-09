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
import org.sonar.server.computation.step.ComputationStepRegistry;
import org.sonar.server.db.DbClient;

import java.io.File;

/**
 * Could be merged with {@link org.sonar.server.computation.ComputationWorker}
 * but it would need {@link org.sonar.server.computation.ComputationWorkerLauncher} to
 * declare transitive dependencies as it directly instantiates this class, without
 * using picocontainer.
 */
public class ComputationService implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(ComputationService.class);

  private final DbClient dbClient;
  private final ComputationStepRegistry stepRegistry;
  private final ActivityService activityService;
  private final TempFolder tempFolder;

  public ComputationService(DbClient dbClient, ComputationStepRegistry stepRegistry, ActivityService activityService,
                            TempFolder tempFolder) {
    this.dbClient = dbClient;
    this.stepRegistry = stepRegistry;
    this.activityService = activityService;
    this.tempFolder = tempFolder;
  }

  public void process(AnalysisReportDto report) {
    TimeProfiler profiler = new TimeProfiler(LOG).start(String.format(
      "#%s - %s - processing analysis report", report.getId(), report.getProjectKey()));

    // Persistence of big amount of data can only be done with a batch session for the moment
    DbSession session = dbClient.openSession(true);

    ComponentDto project = findProject(report, session);
    File reportDir = tempFolder.newDir();
    try {
      ComputationContext context = new ComputationContext(report, project, reportDir);
      dbClient.analysisReportDao().selectAndDecompressToDir(session, report.getId(), reportDir);
      for (ComputationStep step : stepRegistry.steps()) {
        TimeProfiler stepProfiler = new TimeProfiler(LOG).start(step.getDescription());
        step.execute(session, context);
        stepProfiler.stop();
      }
      report.succeed();

    } catch (Exception e) {
      report.fail();
      throw Throwables.propagate(e);

    } finally {
      FileUtils.deleteQuietly(reportDir);
      logActivity(session, report, project);
      session.commit();
      MyBatis.closeQuietly(session);
      profiler.stop();
    }
  }

  private ComponentDto findProject(AnalysisReportDto report, DbSession session) {
    return dbClient.componentDao().getByKey(session, report.getProjectKey());
  }

  private void logActivity(DbSession session, AnalysisReportDto report, ComponentDto project) {
    report.setFinishedAt(System2.INSTANCE.newDate());
    activityService.write(session, Activity.Type.ANALYSIS_REPORT, new AnalysisReportLog(report, project));
  }
}
