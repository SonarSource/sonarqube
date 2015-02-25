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
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.protocol.output.BatchOutputReader;
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
import java.io.IOException;

public class ComputationService implements ServerComponent {

  private static final Logger LOG = Loggers.get(ComputationService.class);

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

  public void process(ReportQueue.Item item) {
    Profiler profiler = Profiler.create(LOG).startDebug(String.format(
      "Analysis of project %s (report %d)", item.dto.getProjectKey(), item.dto.getId()));

    ComponentDto project = loadProject(item);
    try {
      File reportDir = extractReportInDir(item);
      BatchOutputReader reader = new BatchOutputReader(reportDir);
      ComputationContext context = new ComputationContext(reader, project);
      for (ComputationStep step : steps.orderedSteps()) {
        if (ArrayUtils.contains(step.supportedProjectQualifiers(), context.getProject().qualifier())) {
          Profiler stepProfiler = Profiler.createIfDebug(LOG).startDebug(step.getDescription());
          step.execute(context);
          stepProfiler.stopDebug();
        }
      }
      item.dto.succeed();

    } catch (Exception e) {
      item.dto.fail();
      throw Throwables.propagate(e);

    } finally {
      logActivity(item.dto, project);
      profiler.stopInfo();
    }
  }

  private File extractReportInDir(ReportQueue.Item item) {
    File dir = tempFolder.newDir();
    try {
      Profiler profiler = Profiler.createIfDebug(LOG).start();
      ZipUtils.unzip(item.zipFile, dir);
      if (profiler.isDebugEnabled()) {
        String message = String.format("Report extracted | size=%s | project=%s",
          FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(dir)), item.dto.getProjectKey());
        profiler.stopDebug(message);
      }
      return dir;
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to unzip %s into %s", item.zipFile, dir), e);
    }
  }

  private ComponentDto loadProject(ReportQueue.Item queueItem) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getByKey(session, queueItem.dto.getProjectKey());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void logActivity(AnalysisReportDto report, ComponentDto project) {
    DbSession session = dbClient.openSession(false);
    try {
      report.setFinishedAt(System2.INSTANCE.now());
      activityService.write(session, Activity.Type.ANALYSIS_REPORT, new ReportActivity(report, project));
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
