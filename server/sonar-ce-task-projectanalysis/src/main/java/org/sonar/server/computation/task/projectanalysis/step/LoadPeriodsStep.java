/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Populates the {@link PeriodHolder}
 * <p/>
 * Here is how these periods are computed :
 * - Read the period property ${@link org.sonar.core.config.CorePropertyDefinitions#LEAK_PERIOD}
 * - Try to find the matching snapshots from the property
 * - If a snapshot is found, a period is set to the repository
 */
public class LoadPeriodsStep implements ComputationStep {

  private final DbClient dbClient;
  private final ConfigurationRepository configRepository;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PeriodHolderImpl periodsHolder;

  public LoadPeriodsStep(DbClient dbClient, ConfigurationRepository settingsRepository, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    PeriodHolderImpl periodsHolder) {
    this.dbClient = dbClient;
    this.configRepository = settingsRepository;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.periodsHolder = periodsHolder;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          execute(project);
        }
      }).visit(treeRootHolder.getRoot());
  }

  public void execute(Component projectOrView) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      periodsHolder.setPeriod(buildPeriod(projectOrView, dbSession));
    }
  }

  @CheckForNull
  private Period buildPeriod(Component projectOrView, DbSession session) {
    Optional<ComponentDto> projectDto = dbClient.componentDao().selectByKey(session, projectOrView.getKey());
    // No project on first analysis, no period
    if (!projectDto.isPresent()) {
      return null;
    }

    boolean isReportType = projectOrView.getType().isReportType();
    PeriodResolver periodResolver = new PeriodResolver(dbClient, session, projectDto.get().uuid(), analysisMetadataHolder.getAnalysisDate(),
      isReportType ? projectOrView.getReportAttributes().getVersion() : null);

    Configuration config = configRepository.getConfiguration();
    Period period = periodResolver.resolve(config);
    // SONAR-4700 Add a past snapshot only if it exists
    if (period != null) {
      return period;
    }
    return null;
  }

  @Override
  public String getDescription() {
    return "Load differential periods";
  }
}
