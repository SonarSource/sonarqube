/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.SettingsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderImpl;

import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.server.computation.component.CrawlerDepthLimit.reportMaxDepth;

/**
 * Populates the {@link org.sonar.server.computation.period.PeriodsHolder}
 * <p/>
 * Here is how these periods are computed :
 * - Read the 5 period properties ${@link org.sonar.core.config.CorePropertyDefinitions#TIMEMACHINE_PERIOD_PREFIX}
 * - Try to find the matching snapshots from the properties
 * - If a snapshot is found, a new period is added to the repository
 */
public class LoadPeriodsStep implements ComputationStep {

  private static final int NUMBER_OF_PERIODS = 5;

  private final DbClient dbClient;
  private final SettingsRepository settingsRepository;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PeriodsHolderImpl periodsHolder;

  public LoadPeriodsStep(DbClient dbClient, SettingsRepository settingsRepository, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    PeriodsHolderImpl periodsHolder) {
    this.dbClient = dbClient;
    this.settingsRepository = settingsRepository;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.periodsHolder = periodsHolder;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(reportMaxDepth(PROJECT).withViewsMaxDepth(VIEW), PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          execute(project);
        }

        @Override
        public void visitView(Component view) {
          execute(view);
        }
      }).visit(treeRootHolder.getRoot());
  }

  public void execute(Component projectOrView) {
    DbSession session = dbClient.openSession(false);
    try {
      periodsHolder.setPeriods(buildPeriods(projectOrView, session));
    } finally {
      dbClient.closeSession(session);
    }
  }

  private List<Period> buildPeriods(Component projectOrView, DbSession session) {
    Optional<ComponentDto> projectDto = dbClient.componentDao().selectByKey(session, projectOrView.getKey());
    // No project on first analysis, no period
    if (!projectDto.isPresent()) {
      return Collections.emptyList();
    }

    boolean isReportType = projectOrView.getType().isReportType();
    PeriodResolver periodResolver = new PeriodResolver(dbClient, session, projectDto.get().getId(), analysisMetadataHolder.getAnalysisDate(),
      isReportType ? projectOrView.getReportAttributes().getVersion() : null,
      isReportType ? Qualifiers.PROJECT : Qualifiers.VIEW);

    Settings settings = settingsRepository.getSettings(projectOrView);
    List<Period> periods = new ArrayList<>(5);
    for (int index = 1; index <= NUMBER_OF_PERIODS; index++) {
      Period period = periodResolver.resolve(index, settings);
      // SONAR-4700 Add a past snapshot only if it exists
      if (period != null) {
        periods.add(period);
      }
    }
    return periods;
  }

  @Override
  public String getDescription() {
    return "Load differential periods";
  }
}
