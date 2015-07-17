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

package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.formula.counter.IntVariationValue;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_IT_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class FormulaExecutorComponentVisitorTest {
  public static final DumbComponent BALANCED_COMPONENT_TREE = DumbComponent.builder(PROJECT, 1)
    .addChildren(
      DumbComponent.builder(MODULE, 11)
        .addChildren(
          DumbComponent.builder(DIRECTORY, 111)
            .addChildren(
              builder(Component.Type.FILE, 1111).build(),
              builder(Component.Type.FILE, 1112).build()
            ).build()
        ).build(),
      DumbComponent.builder(MODULE, 12)
        .addChildren(
          DumbComponent.builder(DIRECTORY, 121)
            .addChildren(
              builder(Component.Type.FILE, 1211).build()
            ).build()
        ).build()
    ).build();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINES)
    .add(CoreMetrics.NCLOC)
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_IT_COVERAGE);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule()
    .setPeriods(new Period(2, "some mode", null, 95l, 756l), new Period(5, "some other mode", null, 756L, 956L));

  FormulaExecutorComponentVisitor underTest = FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
    .withVariationSupport(periodsHolder)
    .buildFor(ImmutableList.<Formula>of(new FakeFormula(), new FakeVariationFormula()));

  @Test
  public void verify_aggregation_on_value() throws Exception {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);

    measureRepository.addRawMeasure(1111, LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(1112, LINES_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(1211, LINES_KEY, newMeasureBuilder().create(2));

    underTest.visit(BALANCED_COMPONENT_TREE);

    assertThat(measureRepository.getNewRawMeasure(1, NCLOC_KEY).get().getIntValue()).isEqualTo(20);
    assertThat(measureRepository.getNewRawMeasure(11, NCLOC_KEY).get().getIntValue()).isEqualTo(18);
    assertThat(measureRepository.getNewRawMeasure(111, NCLOC_KEY).get().getIntValue()).isEqualTo(18);
    assertThat(measureRepository.getNewRawMeasure(1111, NCLOC_KEY).get().getIntValue()).isEqualTo(10);
    assertThat(measureRepository.getNewRawMeasure(1112, NCLOC_KEY).get().getIntValue()).isEqualTo(8);
    assertThat(measureRepository.getNewRawMeasure(12, NCLOC_KEY).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getNewRawMeasure(121, NCLOC_KEY).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getNewRawMeasure(1211, NCLOC_KEY).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void verify_multi_metric_formula_support_and_aggregation() throws Exception {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);

    measureRepository.addRawMeasure(1111, LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(1112, LINES_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(1211, LINES_KEY, newMeasureBuilder().create(2));

    FormulaExecutorComponentVisitor underTest = FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .withVariationSupport(periodsHolder)
      .buildFor(ImmutableList.<Formula>of(new FakeMultiMetricFormula()));
    underTest.visit(BALANCED_COMPONENT_TREE);

    assertThat(measureRepository.getNewRawMeasure(1, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(30);
    assertThat(measureRepository.getNewRawMeasure(1, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(120);
    assertThat(measureRepository.getNewRawMeasure(11, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(28);
    assertThat(measureRepository.getNewRawMeasure(11, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(118);
    assertThat(measureRepository.getNewRawMeasure(111, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(28);
    assertThat(measureRepository.getNewRawMeasure(111, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(118);
    assertThat(measureRepository.getNewRawMeasure(1111, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(20);
    assertThat(measureRepository.getNewRawMeasure(1111, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(110);
    assertThat(measureRepository.getNewRawMeasure(1112, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(18);
    assertThat(measureRepository.getNewRawMeasure(1112, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(108);
    assertThat(measureRepository.getNewRawMeasure(12, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(12);
    assertThat(measureRepository.getNewRawMeasure(12, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(102);
    assertThat(measureRepository.getNewRawMeasure(121, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(12);
    assertThat(measureRepository.getNewRawMeasure(121, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(102);
    assertThat(measureRepository.getNewRawMeasure(1211, NEW_LINES_TO_COVER_KEY).get().getIntValue()).isEqualTo(12);
    assertThat(measureRepository.getNewRawMeasure(1211, NEW_IT_COVERAGE_KEY).get().getIntValue()).isEqualTo(102);
  }

  @Test
  public void verify_aggregation_on_variations() throws Exception {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);

    measureRepository.addRawMeasure(1111, NEW_LINES_TO_COVER_KEY, createMeasureWithVariation(10, 20));
    measureRepository.addRawMeasure(1112, NEW_LINES_TO_COVER_KEY, createMeasureWithVariation(8, 16));
    measureRepository.addRawMeasure(1211, NEW_LINES_TO_COVER_KEY, createMeasureWithVariation(2, 4));

    underTest.visit(BALANCED_COMPONENT_TREE);

    assertThat(measureRepository.getNewRawMeasure(1, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(20);
    assertThat(measureRepository.getNewRawMeasure(1, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(40);
    assertThat(measureRepository.getNewRawMeasure(11, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(18);
    assertThat(measureRepository.getNewRawMeasure(11, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(36);
    assertThat(measureRepository.getNewRawMeasure(111, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(18);
    assertThat(measureRepository.getNewRawMeasure(111, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(36);
    assertThat(measureRepository.getNewRawMeasure(1111, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(10);
    assertThat(measureRepository.getNewRawMeasure(1111, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(20);
    assertThat(measureRepository.getNewRawMeasure(1112, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(8);
    assertThat(measureRepository.getNewRawMeasure(1112, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(16);
    assertThat(measureRepository.getNewRawMeasure(12, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(2);
    assertThat(measureRepository.getNewRawMeasure(12, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(4);
    assertThat(measureRepository.getNewRawMeasure(121, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(2);
    assertThat(measureRepository.getNewRawMeasure(121, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(4);
    assertThat(measureRepository.getNewRawMeasure(1211, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation2()).isEqualTo(2);
    assertThat(measureRepository.getNewRawMeasure(1211, NEW_IT_COVERAGE_KEY).get().getVariations().getVariation5()).isEqualTo(4);
  }

  private static Measure createMeasureWithVariation(double variation2Value, double variation5Value) {
    return newMeasureBuilder().setVariations(new MeasureVariations(null, variation2Value, null, null, variation5Value)).createNoValue();
  }

  @Test
  public void add_no_measure() throws Exception {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111)
              .addChildren(
                builder(Component.Type.FILE, 1111).build()
              ).build()
          ).build()
      ).build();
    treeRootHolder.setRoot(project);

    underTest.visit(project);

    assertThat(measureRepository.getNewRawMeasures(1)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(11)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(111)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(1111)).isEmpty();
  }

  @Test
  public void add_no_measure_when_no_file() throws Exception {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111).build()
          ).build()
      ).build();
    treeRootHolder.setRoot(project);

    underTest.visit(project);

    assertThat(measureRepository.getNewRawMeasures(1)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(11)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(111)).isEmpty();
  }

  private class FakeFormula implements Formula<FakeCounter> {

    @Override
    public FakeCounter createNewCounter() {
      return new FakeCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeCounter counter, CreateMeasureContext context) {
      // verify the context which is passed to the method
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric()).isSameAs(metricRepository.getByKey(NCLOC_KEY));

      // simplest computation
      if (counter.value <= 0) {
        return Optional.absent();
      }
      return Optional.of(Measure.newMeasureBuilder().create(counter.value));
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NCLOC_KEY};
    }
  }

  private class FakeMultiMetricFormula implements Formula<FakeCounter> {

    @Override
    public FakeCounter createNewCounter() {
      return new FakeCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeCounter counter, CreateMeasureContext context) {
      // verify the context which is passed to the method
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric())
        .isIn(metricRepository.getByKey(NEW_LINES_TO_COVER_KEY), metricRepository.getByKey(NEW_IT_COVERAGE_KEY));

      // simplest computation
      if (counter.value <= 0) {
        return Optional.absent();
      }
      return Optional.of(Measure.newMeasureBuilder().create(counter.value + metricOffset(context.getMetric())));
    }

    private int metricOffset(Metric metric) {
      if (metric.getKey().equals(NEW_LINES_TO_COVER_KEY)) {
        return 10;
      }
      if (metric.getKey().equals(NEW_IT_COVERAGE_KEY)) {
        return 100;
      }
      throw new IllegalArgumentException("Unsupported metric " + metric);
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_LINES_TO_COVER_KEY, NEW_IT_COVERAGE_KEY};
    }
  }

  private class FakeCounter implements Counter<FakeCounter> {
    private int value = 0;

    @Override
    public void aggregate(FakeCounter counter) {
      this.value += counter.value;
    }

    @Override
    public void aggregate(FileAggregateContext context) {
      // verify the context which is passed to the method
      assertThat(context.getFile().getRef()).isIn(1111, 1112, 1211);
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());

      Optional<Measure> measureOptional = context.getMeasure(LINES_KEY);
      if (measureOptional.isPresent()) {
        value += measureOptional.get().getIntValue();
      }
    }
  }

  private class FakeVariationFormula implements Formula<FakeVariationCounter> {

    @Override
    public FakeVariationCounter createNewCounter() {
      return new FakeVariationCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeVariationCounter counter, CreateMeasureContext context) {
      // verify the context which is passed to the method
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric()).isSameAs(metricRepository.getByKey(NEW_IT_COVERAGE_KEY));

      Optional<MeasureVariations> measureVariations = counter.values.toMeasureVariations();
      if (measureVariations.isPresent()) {
        return Optional.of(
          newMeasureBuilder()
            .setVariations(measureVariations.get())
            .createNoValue()
          );
      }
      return Optional.absent();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_IT_COVERAGE_KEY};
    }
  }

  private class FakeVariationCounter implements Counter<FakeVariationCounter> {
    private final IntVariationValue.Array values = IntVariationValue.newArray();

    @Override
    public void aggregate(FakeVariationCounter counter) {
      values.incrementAll(counter.values);
    }

    @Override
    public void aggregate(FileAggregateContext context) {
      // verify the context which is passed to the method
      assertThat(context.getFile().getRef()).isIn(1111, 1112, 1211);
      assertThat(context.getPeriods()).isEqualTo(periodsHolder.getPeriods());

      Optional<Measure> measureOptional = context.getMeasure(NEW_LINES_TO_COVER_KEY);
      if (!measureOptional.isPresent()) {
        return;
      }
      for (Period period : context.getPeriods()) {
        this.values.increment(
          period,
          (int) measureOptional.get().getVariations().getVariation(period.getIndex()));
      }
    }
  }

}
