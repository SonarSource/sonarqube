/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;

public class LiveMeasureTreeUpdaterImpl implements LiveMeasureTreeUpdater {
  private final DbClient dbClient;
  private final MeasureUpdateFormulaFactory formulaFactory;

  public LiveMeasureTreeUpdaterImpl(DbClient dbClient, MeasureUpdateFormulaFactory formulaFactory) {
    this.dbClient = dbClient;
    this.formulaFactory = formulaFactory;
  }

  @Override
  public void update(DbSession dbSession, SnapshotDto lastAnalysis, Configuration config, ComponentIndex components, BranchDto branch, MeasureMatrix measures) {
    long beginningOfLeak = getBeginningOfLeakPeriod(lastAnalysis, branch);
    boolean shouldUseLeakFormulas = shouldUseLeakFormulas(lastAnalysis, branch);

    // 1. set new measure from issues to each component from touched components to the root
    updateMatrixWithIssues(dbSession, measures, components, config, shouldUseLeakFormulas, beginningOfLeak);

    // 2. aggregate new measures up the component tree
    updateMatrixWithHierarchy(measures, components, config, shouldUseLeakFormulas);
  }

  private void updateMatrixWithHierarchy(MeasureMatrix matrix, ComponentIndex components, Configuration config, boolean useLeakFormulas) {
    DebtRatingGrid debtRatingGrid = new DebtRatingGrid(config);
    FormulaContextImpl context = new FormulaContextImpl(matrix, components, debtRatingGrid);
    components.getSortedTree().forEach(c -> {
      for (MeasureUpdateFormula formula : formulaFactory.getFormulas()) {
        if (shouldComputeMetric(formula, useLeakFormulas, components.getBranch(), matrix)) {
          context.change(c, formula);
          try {
            formula.computeHierarchy(context);
          } catch (RuntimeException e) {
            throw new IllegalStateException("Fail to compute " + formula.getMetric().getKey() + " on "
              + context.getComponent().getKey() + " (uuid: " + context.getComponent().uuid() + ")", e);
          }
        }
      }
    });
  }

  private void updateMatrixWithIssues(DbSession dbSession, MeasureMatrix matrix, ComponentIndex components, Configuration config, boolean useLeakFormulas, long beginningOfLeak) {
    DebtRatingGrid debtRatingGrid = new DebtRatingGrid(config);
    FormulaContextImpl context = new FormulaContextImpl(matrix, components, debtRatingGrid);

    components.getSortedTree().forEach(c -> {
      IssueCounter issueCounter = new IssueCounter(dbClient.issueDao().selectIssueGroupsByComponent(dbSession, c, beginningOfLeak),
        dbClient.issueDao().selectIssueImpactGroupsByComponent(dbSession, c));
      for (MeasureUpdateFormula formula : formulaFactory.getFormulas()) {
        if (shouldComputeMetric(formula, useLeakFormulas, components.getBranch(), matrix)) {
          context.change(c, formula);
          try {
            formula.compute(context, issueCounter);
          } catch (RuntimeException e) {
            throw new IllegalStateException("Fail to compute " + formula.getMetric().getKey() + " on "
              + context.getComponent().getKey() + " (uuid: " + context.getComponent().uuid() + ")", e);
          }
        }
      }
    });
  }

  private static boolean shouldComputeMetric(MeasureUpdateFormula formula, boolean useLeakFormulas, ComponentDto branchComponent,
                                      MeasureMatrix matrix) {
    // Use formula when the leak period is defined, it's a PR, or the formula is not about the leak period
    return (useLeakFormulas || !formula.isOnLeak())
           // Some metrics should only be computed if the metric has been computed on the branch before (during analysis).
           // Otherwise, the computed measure would only apply to the touched components and be incomplete.
           && (!formula.isOnlyIfComputedOnBranch() || matrix.getMeasure(branchComponent, formula.getMetric().getKey()).isPresent());
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

    void change(ComponentDto component, MeasureUpdateFormula formula) {
      this.currentComponent = component;
      this.currentFormula = formula;
    }

    public List<Double> getChildrenValues() {
      List<ComponentDto> children = componentIndex.getChildren(currentComponent);
      return children.stream()
        .flatMap(c -> matrix.getMeasure(c, currentFormula.getMetric().getKey()).stream())
        .map(LiveMeasureDto::getValue)
        .filter(Objects::nonNull)
        .toList();
    }

    public List<String> getChildrenTextValues() {
      List<ComponentDto> children = componentIndex.getChildren(currentComponent);
      return children.stream()
        .flatMap(c -> matrix.getMeasure(c, currentFormula.getMetric().getKey()).stream())
        .map(LiveMeasureDto::getTextValue)
        .filter(Objects::nonNull)
        .toList();
    }

    /**
     * Some child components may not have the measures 'SECURITY_HOTSPOTS_TO_REVIEW_STATUS' and 'SECURITY_HOTSPOTS_REVIEWED_STATUS' saved for them,
     * so we may need to calculate them based on 'SECURITY_HOTSPOTS_REVIEWED' and 'SECURITY_HOTSPOTS'.
     */
    @Override
    public long getChildrenHotspotsReviewed() {
      return getChildrenHotspotsReviewed(SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY, SECURITY_HOTSPOTS_REVIEWED_KEY, SECURITY_HOTSPOTS_KEY);
    }

    /**
     * Some child components may not have the measure 'SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY'. We assume that 'SECURITY_HOTSPOTS_KEY' has the same value.
     */
    @Override
    public long getChildrenHotspotsToReview() {
      return componentIndex.getChildren(currentComponent)
        .stream()
        .map(c -> matrix.getMeasure(c, SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).or(() -> matrix.getMeasure(c, SECURITY_HOTSPOTS_KEY)))
        .mapToLong(lmOpt -> lmOpt.flatMap(lm -> Optional.ofNullable(lm.getValue())).orElse(0D).longValue())
        .sum();
    }

    @Override
    public long getChildrenNewHotspotsReviewed() {
      return getChildrenHotspotsReviewed(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, NEW_SECURITY_HOTSPOTS_KEY);
    }

    /**
     * Some child components may not have the measure 'NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY'. We assume that 'NEW_SECURITY_HOTSPOTS_KEY' has the same value.
     */
    @Override
    public long getChildrenNewHotspotsToReview() {
      return componentIndex.getChildren(currentComponent)
        .stream()
        .map(c -> matrix.getMeasure(c, NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).or(() -> matrix.getMeasure(c, NEW_SECURITY_HOTSPOTS_KEY)))
        .mapToLong(lmOpt -> lmOpt.flatMap(lm -> Optional.ofNullable(lm.getValue())).orElse(0D).longValue())
        .sum();
    }

    private long getChildrenHotspotsReviewed(String metricKey, String percMetricKey, String hotspotsMetricKey) {
      return componentIndex.getChildren(currentComponent)
        .stream()
        .mapToLong(c -> getHotspotsReviewed(c, metricKey, percMetricKey, hotspotsMetricKey))
        .sum();
    }

    private long getHotspotsReviewed(ComponentDto c, String metricKey, String percMetricKey, String hotspotsMetricKey) {
      Optional<LiveMeasureDto> measure = matrix.getMeasure(c, metricKey);
      return measure.map(lm -> Optional.ofNullable(lm.getValue()).orElse(0D).longValue())
        .orElseGet(() -> matrix.getMeasure(c, percMetricKey)
          .flatMap(percentage -> matrix.getMeasure(c, hotspotsMetricKey)
            .map(hotspots -> {
              double perc = Optional.ofNullable(percentage.getValue()).orElse(0D) / 100D;
              double toReview = Optional.ofNullable(hotspots.getValue()).orElse(0D);
              double reviewed = (toReview * perc) / (1D - perc);
              return Math.round(reviewed);
            }))
          .orElse(0L));
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
    public void setValue(double value) {
      String metricKey = currentFormula.getMetric().getKey();
      matrix.setValue(currentComponent, metricKey, value);
    }

    @Override
    public void setValue(Rating value) {
      String metricKey = currentFormula.getMetric().getKey();
      matrix.setValue(currentComponent, metricKey, value);
    }

    @Override
    public void setValue(String value) {
      String metricKey = currentFormula.getMetric().getKey();
      matrix.setValue(currentComponent, metricKey, value);
    }
  }
}
