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
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.computation.activity.CEActivityManager;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.db.DbClient;

import static org.sonar.core.computation.db.AnalysisReportDto.Status.FAILED;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.SUCCESS;

@ServerSide
public class ComputationService {

  private static final Logger LOG = Loggers.get(ComputationService.class);

  private final ReportQueue.Item item;
  private final ComputationSteps steps;
  private final BatchReportReader reportReader;
  private final CEActivityManager activityManager;
  private final System2 system;
  private final DbClient dbClient;
  private final LanguageRepository languageRepository;

  public ComputationService(ReportQueue.Item item, ComputationSteps steps, CEActivityManager activityManager, System2 system,
    BatchReportReader reportReader, DbClient dbClient, LanguageRepository languageRepository) {
    this.item = item;
    this.steps = steps;
    this.reportReader = reportReader;
    this.activityManager = activityManager;
    this.system = system;
    this.dbClient = dbClient;
    this.languageRepository = languageRepository;
  }

  public void process() {
    String projectKey = item.dto.getProjectKey();
    Profiler profiler = Profiler.create(LOG).startDebug(
      String.format("Analysis of project %s (report %d)", projectKey, item.dto.getId())
      );

    try {
      ComputationContext context = new ComputationContext(reportReader, null, null, dbClient, ComponentTreeBuilders.from(reportReader), languageRepository);

      for (ComputationStep step : steps.instances()) {
        Profiler stepProfiler = Profiler.createIfDebug(LOG).startDebug(step.getDescription());
        step.execute(context);
        stepProfiler.stopDebug();
      }
      item.dto.setStatus(SUCCESS);
    } catch (Throwable e) {
      item.dto.setStatus(FAILED);
      throw Throwables.propagate(e);
    } finally {
      item.dto.setFinishedAt(system.now());
      activityManager.saveActivity(item.dto);
      profiler.stopInfo();
    }
  }
}
