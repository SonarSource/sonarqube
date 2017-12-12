/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.measure.live;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.DebtRatingGrid;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating;

public class LiveMeasureComputerImpl implements LiveMeasureComputer {

  private final DbClient dbClient;
  private final MeasureMatrixLoader matrixLoader;
  private final Configuration config;
  private final IssueMetricFormulaFactory formulaFactory;

  public LiveMeasureComputerImpl(DbClient dbClient, MeasureMatrixLoader matrixLoader, Configuration config, IssueMetricFormulaFactory formulaFactory) {
    this.dbClient = dbClient;
    this.matrixLoader = matrixLoader;
    this.config = config;
    this.formulaFactory = formulaFactory;
  }

  @Override
  public void refresh(DbSession dbSession, Collection<ComponentDto> components) {
    if (components.isEmpty()) {
      return;
    }

    Map<String, List<ComponentDto>> componentsByProjectUuid = components.stream().collect(Collectors.groupingBy(ComponentDto::projectUuid));
    for (List<ComponentDto> groupedComponents : componentsByProjectUuid.values()) {
      refreshComponentsOnSameProject(dbSession, groupedComponents);
    }
  }

  private void refreshComponentsOnSameProject(DbSession dbSession, List<ComponentDto> components) {
    String projectUuid = components.iterator().next().projectUuid();
    Optional<SnapshotDto> lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, projectUuid);
    if (!lastAnalysis.isPresent()) {
      // project has been deleted at the same time ?
      return;
    }
    Optional<Long> beginningOfLeakPeriod = lastAnalysis.map(SnapshotDto::getPeriodDate);

    MeasureMatrix matrix = matrixLoader.load(dbSession, components, formulaFactory.getFormulaMetrics());
    DebtRatingGrid debtRatingGrid = new DebtRatingGrid(config);

    matrix.getBottomUpComponents().forEach(c -> {
      IssueCounterImpl issueCounter = new IssueCounterImpl(dbClient.issueDao().selectGroupsOfComponentTreeOnLeak(dbSession, c, beginningOfLeakPeriod.orElse(Long.MAX_VALUE)));
      FormulaContextImpl context = new FormulaContextImpl(matrix, debtRatingGrid);
      for (IssueMetricFormula formula : formulaFactory.getFormulas()) {
        // exclude leak formulas when leak period is not defined
        if (beginningOfLeakPeriod.isPresent() || !formula.isOnLeak()) {
          context.change(c, formula);
          try {
            formula.compute(context, issueCounter);
          } catch (RuntimeException e) {
            throw new IllegalStateException("Fail to compute " + formula.getMetric().getKey() + " on " + context.getComponent().getDbKey(), e);
          }
        }
      }
    });

    // persist the measures that have been created or updated
    // TODO test concurrency (CE deletes or updates the row)
    matrix.getChanged().forEach(m -> dbClient.liveMeasureDao().insertOrUpdate(dbSession, m, null));
    dbSession.commit();
  }

  private static class FormulaContextImpl implements IssueMetricFormula.Context {
    private final MeasureMatrix matrix;
    private final DebtRatingGrid debtRatingGrid;
    private ComponentDto currentComponent;
    private IssueMetricFormula currentFormula;

    private FormulaContextImpl(MeasureMatrix matrix, DebtRatingGrid debtRatingGrid) {
      this.matrix = matrix;
      this.debtRatingGrid = debtRatingGrid;
    }

    private void change(ComponentDto component, IssueMetricFormula formula) {
      this.currentComponent = component;
      this.currentFormula = formula;
    }

    @Override
    public ComponentDto getComponent() {
      return currentComponent;
    }

    @Override
    public DebtRatingGrid getDebtRatingGrid() {
      return debtRatingGrid;
    }

    @Override
    public OptionalDouble getValue(Metric metric) {
      return matrix.getValue(currentComponent, metric);
    }

    @Override
    public void setValue(double value) {
      if (currentFormula.isOnLeak()) {
        matrix.setLeakValue(currentComponent, currentFormula.getMetric(), value);
      } else {
        matrix.setValue(currentComponent, currentFormula.getMetric(), value);
      }
    }

    @Override
    public void setValue(Rating value) {
      if (currentFormula.isOnLeak()) {
        matrix.setLeakValue(currentComponent, currentFormula.getMetric(), value);
      } else {
        matrix.setValue(currentComponent, currentFormula.getMetric(), value);
      }
    }
  }
}
