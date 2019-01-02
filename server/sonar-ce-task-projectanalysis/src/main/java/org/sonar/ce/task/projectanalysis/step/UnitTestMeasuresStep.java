/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.formula.counter.IntSumCounter;
import org.sonar.ce.task.projectanalysis.formula.counter.LongSumCounter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_SUCCESS_DENSITY_KEY;

/**
 * Computes unit test measures on files and then aggregates them on higher components.
 */
public class UnitTestMeasuresStep implements ComputationStep {

  private static final String[] METRICS = new String[] {TESTS_KEY, TEST_ERRORS_KEY, TEST_FAILURES_KEY, SKIPPED_TESTS_KEY, TEST_SUCCESS_DENSITY_KEY, TEST_EXECUTION_TIME_KEY};

  private static final ImmutableList<Formula> FORMULAS = ImmutableList.of(new UnitTestsFormula());

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public UnitTestMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(FORMULAS))
        .visit(treeRootHolder.getRoot());
  }

  private static class UnitTestsFormula implements Formula<UnitTestsCounter> {

    @Override
    public UnitTestsCounter createNewCounter() {
      return new UnitTestsCounter();
    }

    @Override
    public Optional<Measure> createMeasure(UnitTestsCounter counter, CreateMeasureContext context) {
      String metricKey = context.getMetric().getKey();
      Component leaf = counter.getLeaf();
      switch (metricKey) {
        case TESTS_KEY:
          return createIntMeasure(context.getComponent(), leaf, counter.testsCounter.getValue());
        case TEST_ERRORS_KEY:
          return createIntMeasure(context.getComponent(), leaf, counter.testsErrorsCounter.getValue());
        case TEST_FAILURES_KEY:
          return createIntMeasure(context.getComponent(), leaf, counter.testsFailuresCounter.getValue());
        case SKIPPED_TESTS_KEY:
          return createIntMeasure(context.getComponent(), leaf, counter.skippedTestsCounter.getValue());
        case TEST_EXECUTION_TIME_KEY:
          return createLongMeasure(context.getComponent(), leaf, counter.testExecutionTimeCounter.getValue());
        case TEST_SUCCESS_DENSITY_KEY:
          return createDensityMeasure(counter, context.getMetric().getDecimalScale());
        default:
          throw new IllegalStateException(String.format("Metric '%s' is not supported", metricKey));
      }
    }

    private static Optional<Measure> createIntMeasure(Component currentComponent, Component leafComponent, Optional<Integer> metricValue) {
      if (metricValue.isPresent() && leafComponent.getType().isDeeperThan(currentComponent.getType())) {
        return Optional.of(Measure.newMeasureBuilder().create(metricValue.get()));
      }
      return Optional.empty();
    }

    private static Optional<Measure> createLongMeasure(Component currentComponent, Component leafComponent, Optional<Long> metricValue) {
      if (metricValue.isPresent() && leafComponent.getType().isDeeperThan(currentComponent.getType())) {
        return Optional.of(Measure.newMeasureBuilder().create(metricValue.get()));
      }
      return Optional.empty();
    }

    private static Optional<Measure> createDensityMeasure(UnitTestsCounter counter, int decimalScale) {
      if (isPositive(counter.testsCounter.getValue(), true)
        && isPositive(counter.testsErrorsCounter.getValue(), false)
        && isPositive(counter.testsFailuresCounter.getValue(), false)) {
        int tests = counter.testsCounter.getValue().get();
        int errors = counter.testsErrorsCounter.getValue().get();
        int failures = counter.testsFailuresCounter.getValue().get();
        double density = (errors + failures) * 100d / tests;
        return Optional.of(Measure.newMeasureBuilder().create(100d - density, decimalScale));
      }
      return Optional.empty();
    }

    private static boolean isPositive(Optional<Integer> value, boolean isStrictComparison) {
      return value.isPresent() && (isStrictComparison ? (value.get() > 0) : (value.get() >= 0));
    }

    @Override
    public String[] getOutputMetricKeys() {
      return METRICS;
    }
  }

  private static class UnitTestsCounter implements Counter<UnitTestsCounter> {

    private final IntSumCounter testsCounter = new IntSumCounter(TESTS_KEY);
    private final IntSumCounter testsErrorsCounter = new IntSumCounter(TEST_ERRORS_KEY);
    private final IntSumCounter testsFailuresCounter = new IntSumCounter(TEST_FAILURES_KEY);
    private final IntSumCounter skippedTestsCounter = new IntSumCounter(SKIPPED_TESTS_KEY);
    private final LongSumCounter testExecutionTimeCounter = new LongSumCounter(TEST_EXECUTION_TIME_KEY);

    private Component leaf;

    @Override
    public void aggregate(UnitTestsCounter counter) {
      testsCounter.aggregate(counter.testsCounter);
      testsErrorsCounter.aggregate(counter.testsErrorsCounter);
      testsFailuresCounter.aggregate(counter.testsFailuresCounter);
      skippedTestsCounter.aggregate(counter.skippedTestsCounter);
      testExecutionTimeCounter.aggregate(counter.testExecutionTimeCounter);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      this.leaf = context.getLeaf();
      testsCounter.initialize(context);
      testsErrorsCounter.initialize(context);
      testsFailuresCounter.initialize(context);
      skippedTestsCounter.initialize(context);
      testExecutionTimeCounter.initialize(context);
    }

    Component getLeaf() {
      return leaf;
    }
  }

  @Override
  public String getDescription() {
    return "Compute test measures";
  }
}
