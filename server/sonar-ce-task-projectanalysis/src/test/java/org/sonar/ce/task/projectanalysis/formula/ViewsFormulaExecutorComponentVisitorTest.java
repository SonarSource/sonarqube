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
package org.sonar.ce.task.projectanalysis.formula;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.formula.counter.IntValue;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ViewsFormulaExecutorComponentVisitorTest {
  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_1_REF = 11;
  private static final int SUB_SUBVIEW_REF = 111;
  private static final int PROJECT_VIEW_1_REF = 1111;
  private static final int PROJECT_VIEW_2_REF = 1113;
  private static final int SUBVIEW_2_REF = 12;
  private static final int PROJECT_VIEW_3_REF = 121;
  private static final int PROJECT_VIEW_4_REF = 13;

  private static final ViewsComponent BALANCED_COMPONENT_TREE = ViewsComponent.builder(VIEW, ROOT_REF)
    .addChildren(
      ViewsComponent.builder(SUBVIEW, SUBVIEW_1_REF)
        .addChildren(
          ViewsComponent.builder(SUBVIEW, SUB_SUBVIEW_REF)
            .addChildren(
              builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
              builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
            .build())
        .build(),
      ViewsComponent.builder(SUBVIEW, SUBVIEW_2_REF)
        .addChildren(
          builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
        .build(),
      builder(PROJECT_VIEW, PROJECT_VIEW_4_REF).build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINES)
    .add(CoreMetrics.NCLOC)
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_COVERAGE);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Test
  public void verify_aggregation_on_value() {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);
    addRawMeasure(PROJECT_VIEW_1_REF, 1, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_2_REF, 2, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_3_REF, 3, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_4_REF, 4, LINES_KEY);

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(BALANCED_COMPONENT_TREE);

    verifyProjectViewsHasNoAddedRawMeasures();
    verifySingleMetricValue(SUB_SUBVIEW_REF, 3);
    verifySingleMetricValue(SUBVIEW_1_REF, 3);
    verifySingleMetricValue(SUBVIEW_2_REF, 3);
    verifySingleMetricValue(ROOT_REF, 10);
  }

  private MeasureRepositoryRule addRawMeasure(int componentRef, int value, String metricKey) {
    return measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  @Test
  public void verify_multi_metric_formula_support_and_aggregation() {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);
    addRawMeasure(PROJECT_VIEW_1_REF, 1, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_2_REF, 2, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_3_REF, 5, LINES_KEY);
    addRawMeasure(PROJECT_VIEW_4_REF, 4, LINES_KEY);

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeMultiMetricFormula()))
      .visit(BALANCED_COMPONENT_TREE);

    verifyProjectViewsHasNoAddedRawMeasures();
    verifyMultiMetricValues(SUB_SUBVIEW_REF, 13, 103);
    verifyMultiMetricValues(SUBVIEW_1_REF, 13, 103);
    verifyMultiMetricValues(SUBVIEW_2_REF, 15, 105);
    verifyMultiMetricValues(ROOT_REF, 22, 112);
  }

  @Test
  public void verify_aggregation_on_variations() {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);

    addRawMeasureWithVariation(PROJECT_VIEW_1_REF, NEW_LINES_TO_COVER_KEY, 10);
    addRawMeasureWithVariation(PROJECT_VIEW_2_REF, NEW_LINES_TO_COVER_KEY, 8);
    addRawMeasureWithVariation(PROJECT_VIEW_3_REF, NEW_LINES_TO_COVER_KEY, 2);
    addRawMeasureWithVariation(PROJECT_VIEW_4_REF, NEW_LINES_TO_COVER_KEY, 3);

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeVariationFormula()))
      .visit(BALANCED_COMPONENT_TREE);

    verifyProjectViewsHasNoAddedRawMeasures();
    verifySingleMetricWithVariation(SUB_SUBVIEW_REF, 18);
    verifySingleMetricWithVariation(SUBVIEW_1_REF, 18);
    verifySingleMetricWithVariation(SUBVIEW_2_REF, 2);
    verifySingleMetricWithVariation(ROOT_REF, 23);
  }

  private void verifySingleMetricWithVariation(int componentRef, int variation) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(entryOf(NEW_COVERAGE_KEY, createMeasureWithVariation(variation)));
  }

  private MeasureRepositoryRule addRawMeasureWithVariation(int componentRef, String metricKey, int variation) {
    return measureRepository.addRawMeasure(componentRef, metricKey, createMeasureWithVariation(variation));
  }

  private static Measure createMeasureWithVariation(double variation) {
    return newMeasureBuilder().setVariation(variation).createNoValue();
  }

  @Test
  public void verify_no_measure_added_on_projectView() {
    ViewsComponent project = ViewsComponent.builder(VIEW, ROOT_REF)
      .addChildren(
        ViewsComponent.builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            ViewsComponent.builder(SUBVIEW, SUB_SUBVIEW_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(project);

    assertNoAddedRawMeasure(PROJECT_VIEW_1_REF);
    verifySingleMetricValue(SUB_SUBVIEW_REF, 0);
    verifySingleMetricValue(SUBVIEW_1_REF, 0);
    verifySingleMetricValue(ROOT_REF, 0);
  }

  @Test
  public void add_measure_even_if_leaf_is_not_a_PROJECT_VIEW() {
    ViewsComponent project = ViewsComponent.builder(VIEW, ROOT_REF)
      .addChildren(
        ViewsComponent.builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            ViewsComponent.builder(SUBVIEW, SUB_SUBVIEW_REF).build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(project);

    verifySingleMetricValue(SUB_SUBVIEW_REF, 0);
    verifySingleMetricValue(SUBVIEW_1_REF, 0);
    verifySingleMetricValue(ROOT_REF, 0);
  }

  private class FakeFormula implements Formula<FakeCounter> {

    @Override
    public FakeCounter createNewCounter() {
      return new FakeCounter();
    }

    @Override
    public Optional<Measure> createMeasure(FakeCounter counter, CreateMeasureContext context) {
      // verify the context which is passed to the method
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric()).isSameAs(metricRepository.getByKey(NCLOC_KEY));

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
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric())
        .isIn(metricRepository.getByKey(NEW_LINES_TO_COVER_KEY), metricRepository.getByKey(NEW_COVERAGE_KEY));

      return Optional.of(Measure.newMeasureBuilder().create(counter.value + metricOffset(context.getMetric())));
    }

    private int metricOffset(Metric metric) {
      if (metric.getKey().equals(NEW_LINES_TO_COVER_KEY)) {
        return 10;
      }
      if (metric.getKey().equals(NEW_COVERAGE_KEY)) {
        return 100;
      }
      throw new IllegalArgumentException("Unsupported metric " + metric);
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_LINES_TO_COVER_KEY, NEW_COVERAGE_KEY};
    }
  }

  private class FakeCounter implements Counter<FakeCounter> {
    private int value = 0;

    @Override
    public void aggregate(FakeCounter counter) {
      this.value += counter.value;
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      verifyLeafContext(context);

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
      assertThat(context.getComponent()).isNotNull();
      assertThat(context.getMetric()).isSameAs(metricRepository.getByKey(NEW_COVERAGE_KEY));

      IntValue measureVariations = counter.values;
      if (measureVariations.isSet()) {
        return Optional.of(
          newMeasureBuilder()
            .setVariation(measureVariations.getValue())
            .createNoValue());
      }
      return Optional.empty();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_COVERAGE_KEY};
    }
  }

  private class FakeVariationCounter implements Counter<FakeVariationCounter> {
    private final IntValue values = new IntValue();

    @Override
    public void aggregate(FakeVariationCounter counter) {
      values.increment(counter.values);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      verifyLeafContext(context);

      Optional<Measure> measureOptional = context.getMeasure(NEW_LINES_TO_COVER_KEY);
      if (!measureOptional.isPresent()) {
        return;
      }
      this.values.increment((int) measureOptional.get().getVariation());
    }

  }

  private FormulaExecutorComponentVisitor formulaExecutorComponentVisitor(Formula formula) {
    return FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(ImmutableList.of(formula));
  }

  private void verifyProjectViewsHasNoAddedRawMeasures() {
    assertNoAddedRawMeasure(PROJECT_VIEW_1_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_2_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_3_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_4_REF);
  }

  private void assertNoAddedRawMeasure(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef)).isEmpty();
  }

  private void verifySingleMetricValue(int componentRef, int measureValue) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(entryOf(NCLOC_KEY, newMeasureBuilder().create(measureValue)));
  }

  private void verifyMultiMetricValues(int componentRef, int valueLinesToCover, int valueItCoverage) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(
        entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(valueLinesToCover)),
        entryOf(NEW_COVERAGE_KEY, newMeasureBuilder().create(valueItCoverage)));
  }

  private void verifyLeafContext(CounterInitializationContext context) {
    assertThat(context.getLeaf().getChildren()).isEmpty();
  }

}
