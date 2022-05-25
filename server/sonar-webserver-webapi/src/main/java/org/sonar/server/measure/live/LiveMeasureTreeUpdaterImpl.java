/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.server.measure.DebtRatingGrid;
import org.sonar.server.measure.Rating;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;

public class LiveMeasureTreeUpdaterImpl implements LiveMeasureTreeUpdater {
  private final DbClient dbClient;
  private final MeasureUpdateFormulaFactory formulaFactory;
  private final HotspotMeasureUpdater hotspotMeasureUpdater;

  public LiveMeasureTreeUpdaterImpl(DbClient dbClient, MeasureUpdateFormulaFactory formulaFactory, HotspotMeasureUpdater hotspotMeasureUpdater) {
    this.dbClient = dbClient;
    this.formulaFactory = formulaFactory;
    this.hotspotMeasureUpdater = hotspotMeasureUpdater;
  }

  @Override
  public void update(DbSession dbSession, SnapshotDto lastAnalysis, Configuration config, ComponentIndex components, BranchDto branch, MeasureMatrix measures) {
    long beginningOfLeak = getBeginningOfLeakPeriod(lastAnalysis, branch);
    boolean shouldUseLeakFormulas = shouldUseLeakFormulas(lastAnalysis, branch);

    // 1. set new measure from issues to each component from touched components to the root
    updateMatrixWithIssues(dbSession, measures, components, config, shouldUseLeakFormulas, beginningOfLeak);

    // 2. aggregate new measures up the component tree
    updateMatrixWithHierarchy(measures, components, config, shouldUseLeakFormulas);

    // 3. Count hotspots at root level
    // this is only necessary because the count of reviewed and to_review hotspots is only saved for the root (not for all components).
    // For that reason, we can't incrementally generate the new counts up the tree. To have the correct numbers for the root component, we
    // run this extra step that set the hotspots measures to the root based on the total count of hotspots.
    hotspotMeasureUpdater.apply(dbSession, measures, components, shouldUseLeakFormulas, beginningOfLeak);
  }

  private void updateMatrixWithHierarchy(MeasureMatrix matrix, ComponentIndex components, Configuration config, boolean useLeakFormulas) {
    DebtRatingGrid debtRatingGrid = new DebtRatingGrid(config);
    FormulaContextImpl context = new FormulaContextImpl(matrix, components, debtRatingGrid);
    components.getSortedTree().forEach(c -> {
      for (MeasureUpdateFormula formula : formulaFactory.getFormulas()) {
        if (useLeakFormulas || !formula.isOnLeak()) {
          context.change(c, formula);
          try {
            formula.computeHierarchy(context);
          } catch (RuntimeException e) {
            throw new IllegalStateException("Fail to compute " + formula.getMetric().getKey() + " on " + context.getComponent().getDbKey(), e);
          }
        }
      }
    });
  }

  private void updateMatrixWithIssues(DbSession dbSession, MeasureMatrix matrix, ComponentIndex components, Configuration config, boolean useLeakFormulas, long beginningOfLeak) {
    DebtRatingGrid debtRatingGrid = new DebtRatingGrid(config);
    FormulaContextImpl context = new FormulaContextImpl(matrix, components, debtRatingGrid);

    components.getSortedTree().forEach(c -> {
      IssueCounter issueCounter = new IssueCounter(dbClient.issueDao().selectIssueGroupsByComponent(dbSession, c, beginningOfLeak));
      for (MeasureUpdateFormula formula : formulaFactory.getFormulas()) {
        // use formulas when the leak period is defined, it's a PR, or the formula is not about the leak period
        if (useLeakFormulas || !formula.isOnLeak()) {
          context.change(c, formula);
          try {
            formula.compute(context, issueCounter);
          } catch (RuntimeException e) {
            throw new IllegalStateException("Fail to compute " + formula.getMetric().getKey() + " on " + context.getComponent().getDbKey(), e);
          }
        }
      }
    });
  }

  private static long getBeginningOfLeakPeriod(SnapshotDto lastAnalysis, BranchDto branch) {
    if (isPR(branch)) {
      return 0L;
    } else if (REFERENCE_BRANCH.name().equals(lastAnalysis.getPeriodMode())) {
      return -1;
    } else {
      return Optional.ofNullable(lastAnalysis.getPeriodDate()).orElse(Long.MAX_VALUE);
    }
  }

  private static boolean isPR(BranchDto branch) {
    return branch.getBranchType() == BranchType.PULL_REQUEST;
  }

  private static boolean shouldUseLeakFormulas(SnapshotDto lastAnalysis, BranchDto branch) {
    return lastAnalysis.getPeriodDate() != null || isPR(branch) || REFERENCE_BRANCH.name().equals(lastAnalysis.getPeriodMode());
  }

  public static class FormulaContextImpl implements MeasureUpdateFormula.Context {
    private final MeasureMatrix matrix;
    private final ComponentIndex componentIndex;
    private final DebtRatingGrid debtRatingGrid;
    private ComponentDto currentComponent;
    private MeasureUpdateFormula currentFormula;

    public FormulaContextImpl(MeasureMatrix matrix, ComponentIndex componentIndex, DebtRatingGrid debtRatingGrid) {
      this.matrix = matrix;
      this.componentIndex = componentIndex;
      this.debtRatingGrid = debtRatingGrid;
    }

    private void change(ComponentDto component, MeasureUpdateFormula formula) {
      this.currentComponent = component;
      this.currentFormula = formula;
    }

    public List<Double> getChildrenValues() {
      List<ComponentDto> children = componentIndex.getChildren(currentComponent);
      return children.stream()
        .flatMap(c -> matrix.getMeasure(c, currentFormula.getMetric().getKey()).stream())
        .map(LiveMeasureDto::getValue)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }

    public List<Double> getChildrenLeakValues() {
      List<ComponentDto> children = componentIndex.getChildren(currentComponent);
      return children.stream()
        .flatMap(c -> matrix.getMeasure(c, currentFormula.getMetric().getKey()).stream())
        .map(LiveMeasureDto::getVariation)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
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
    public Optional<Double> getValue(Metric metric) {
      Optional<LiveMeasureDto> measure = matrix.getMeasure(currentComponent, metric.getKey());
      return measure.map(LiveMeasureDto::getValue);
    }

    @Override
    public Optional<String> getText(Metric metric) {
      Optional<LiveMeasureDto> measure = matrix.getMeasure(currentComponent, metric.getKey());
      return measure.map(LiveMeasureDto::getTextValue);
    }

    @Override
    public Optional<Double> getLeakValue(Metric metric) {
      Optional<LiveMeasureDto> measure = matrix.getMeasure(currentComponent, metric.getKey());
      return measure.map(LiveMeasureDto::getVariation);
    }

    @Override
    public void setValue(double value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(!currentFormula.isOnLeak(), "Formula of metric %s accepts only leak values", metricKey);
      matrix.setValue(currentComponent, metricKey, value);
    }

    @Override
    public void setLeakValue(double value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(currentFormula.isOnLeak(), "Formula of metric %s does not accept leak values", metricKey);
      matrix.setLeakValue(currentComponent, metricKey, value);
    }

    @Override
    public void setValue(Rating value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(!currentFormula.isOnLeak(), "Formula of metric %s accepts only leak values", metricKey);
      matrix.setValue(currentComponent, metricKey, value);
    }

    @Override
    public void setLeakValue(Rating value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(currentFormula.isOnLeak(), "Formula of metric %s does not accept leak values", metricKey);
      matrix.setLeakValue(currentComponent, metricKey, value);
    }
  }
}
