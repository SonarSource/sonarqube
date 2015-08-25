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

package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.formula.CounterInitializationContext;
import org.sonar.server.computation.formula.counter.IntSumCounter;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_SUCCESS_DENSITY_KEY;
import static org.sonar.server.computation.formula.SumFormula.createIntSumFormula;

/**
 * Computes unit test measures on files and then aggregates them on higher components.
 */
public class UnitTestMeasuresStep implements ComputationStep {

  private static final String[] METRICS = new String[] {TESTS_KEY, TEST_ERRORS_KEY, TEST_FAILURES_KEY, TEST_SUCCESS_DENSITY_KEY};

  private static final ImmutableList<Formula> FORMULAS = ImmutableList.<Formula>of(
    createIntSumFormula(TEST_EXECUTION_TIME_KEY),
    createIntSumFormula(SKIPPED_TESTS_KEY),
    new UnitTestsFormula());

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public UnitTestMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute() {
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
      switch (metricKey) {
        case TESTS_KEY:
          return createMeasure(context.getComponent().getType(), counter.testsCounter.getValue());
        case TEST_ERRORS_KEY:
          return createMeasure(context.getComponent().getType(), counter.testsErrorsCounter.getValue());
        case TEST_FAILURES_KEY:
          return createMeasure(context.getComponent().getType(), counter.testsFailuresCounter.getValue());
        case TEST_SUCCESS_DENSITY_KEY:
          return createDensityMeasure(counter);
        default:
          throw new IllegalStateException(String.format("Metric '%s' is not supported", metricKey));
      }
    }

    private static Optional<Measure> createMeasure(Component.Type componentType, Optional<Integer> metricValue) {
      if (metricValue.isPresent() && CrawlerDepthLimit.LEAVES.isDeeperThan(componentType)) {
        return Optional.of(Measure.newMeasureBuilder().create(metricValue.get()));
      }
      return Optional.absent();
    }

    private static Optional<Measure> createDensityMeasure(UnitTestsCounter counter) {
      if (isPositive(counter.testsCounter.getValue(), true)
        && isPositive(counter.testsErrorsCounter.getValue(), false)
        && isPositive(counter.testsFailuresCounter.getValue(), false)) {
        int tests = counter.testsCounter.getValue().get();
        int errors = counter.testsErrorsCounter.getValue().get();
        int failures = counter.testsFailuresCounter.getValue().get();
        double density = (errors + failures) * 100d / tests;
        return Optional.of(Measure.newMeasureBuilder().create(100d - density));
      }
      return Optional.absent();
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

    @Override
    public void aggregate(UnitTestsCounter counter) {
      testsCounter.aggregate(counter.testsCounter);
      testsErrorsCounter.aggregate(counter.testsErrorsCounter);
      testsFailuresCounter.aggregate(counter.testsFailuresCounter);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      testsCounter.initialize(context);
      testsErrorsCounter.initialize(context);
      testsFailuresCounter.initialize(context);
    }
  }

  @Override
  public String getDescription() {
    return "Aggregation of comment measures";
  }
}
