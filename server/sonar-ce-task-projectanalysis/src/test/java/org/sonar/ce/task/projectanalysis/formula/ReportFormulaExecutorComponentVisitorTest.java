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
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.formula.counter.IntValue;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;
import static org.sonar.test.ExceptionCauseMatcher.hasType;

public class ReportFormulaExecutorComponentVisitorTest {
  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_1_REF = 111;
  private static final int FILE_1_REF = 1111;
  private static final int FILE_2_REF = 1112;
  private static final int DIRECTORY_2_REF = 121;
  private static final int FILE_3_REF = 1211;

  private static final ReportComponent BALANCED_COMPONENT_TREE = ReportComponent.builder(PROJECT, ROOT_REF)
    .addChildren(
      ReportComponent.builder(DIRECTORY, DIRECTORY_1_REF)
        .addChildren(
          builder(Component.Type.FILE, FILE_1_REF).build(),
          builder(Component.Type.FILE, FILE_2_REF).build())
        .build(),
      ReportComponent.builder(DIRECTORY, DIRECTORY_2_REF)
        .addChildren(
          builder(Component.Type.FILE, FILE_3_REF).build())
        .build())
    .build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
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

    measureRepository.addRawMeasure(FILE_1_REF, LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, LINES_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(FILE_3_REF, LINES_KEY, newMeasureBuilder().create(2));

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(BALANCED_COMPONENT_TREE);

    assertAddedRawMeasure(ROOT_REF, 20);
    assertAddedRawMeasure(111, 18);
    assertAddedRawMeasure(FILE_1_REF, 10);
    assertAddedRawMeasure(FILE_2_REF, 8);
    assertAddedRawMeasure(DIRECTORY_2_REF, 2);
    assertAddedRawMeasure(FILE_3_REF, 2);
  }

  @Test
  public void verify_multi_metric_formula_support_and_aggregation() {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);

    measureRepository.addRawMeasure(FILE_1_REF, LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, LINES_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(FILE_3_REF, LINES_KEY, newMeasureBuilder().create(2));

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeMultiMetricFormula()))
      .visit(BALANCED_COMPONENT_TREE);

    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(30)),
      entryOf(NEW_COVERAGE_KEY, newMeasureBuilder().create(120)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(111))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(28)),
      entryOf(NEW_COVERAGE_KEY, newMeasureBuilder().create(118)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(20)),
      entryOf(NEW_COVERAGE_KEY, newMeasureBuilder().create(110)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(18)),
      entryOf(NEW_COVERAGE_KEY, newMeasureBuilder().create(108)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_2_REF))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(12)),
      entryOf(NEW_COVERAGE_KEY, newMeasureBuilder().create(102)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_3_REF))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().create(12)),
      entryOf(NEW_COVERAGE_KEY, newMeasureBuilder().create(102)));
  }

  @Test
  public void verify_aggregation_on_variation() {
    treeRootHolder.setRoot(BALANCED_COMPONENT_TREE);

    measureRepository.addRawMeasure(FILE_1_REF, NEW_LINES_TO_COVER_KEY, createMeasureWithVariation(10));
    measureRepository.addRawMeasure(FILE_2_REF, NEW_LINES_TO_COVER_KEY, createMeasureWithVariation(8));
    measureRepository.addRawMeasure(FILE_3_REF, NEW_LINES_TO_COVER_KEY, createMeasureWithVariation(2));

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeVariationFormula()))
      .visit(BALANCED_COMPONENT_TREE);

    assertAddedRawMeasureVariation(ROOT_REF, 20);
    assertAddedRawMeasureVariation(DIRECTORY_1_REF, 18);
    assertAddedRawMeasureVariation(FILE_1_REF, 10);
    assertAddedRawMeasureVariation(FILE_2_REF, 8);
    assertAddedRawMeasureVariation(DIRECTORY_2_REF, 2);
    assertAddedRawMeasureVariation(FILE_3_REF, 2);
  }

  @Test
  public void measures_are_0_when_there_is_no_input_measure() {
    ReportComponent project = ReportComponent.builder(PROJECT, ROOT_REF)
      .addChildren(
        ReportComponent.builder(DIRECTORY, DIRECTORY_1_REF)
          .addChildren(
            builder(Component.Type.FILE, FILE_1_REF).build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(project);

    assertAddedRawMeasure(ROOT_REF, 0);
    assertAddedRawMeasure(DIRECTORY_1_REF, 0);
    assertAddedRawMeasure(FILE_1_REF, 0);
  }

  @Test
  public void add_measure_even_when_leaf_is_not_FILE() {
    ReportComponent project = ReportComponent.builder(PROJECT, ROOT_REF)
      .addChildren(
        ReportComponent.builder(DIRECTORY, 111).build())
      .build();
    treeRootHolder.setRoot(project);

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(project);

    assertAddedRawMeasure(DIRECTORY_1_REF, 0);
  }

  @Test
  public void compute_measure_on_project_without_children() {
    ReportComponent root = builder(PROJECT, ROOT_REF).build();
    treeRootHolder.setRoot(root);
    measureRepository.addRawMeasure(ROOT_REF, LINES_KEY, newMeasureBuilder().create(10));

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(root);

    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(entryOf(NCLOC_KEY, newMeasureBuilder().create(10)));
  }

  @Test
  public void ignore_measure_defined_on_project_when_measure_is_defined_on_leaf() {
    ReportComponent root = builder(PROJECT, ROOT_REF)
      .addChildren(
        builder(Component.Type.FILE, FILE_1_REF).build())
      .build();
    treeRootHolder.setRoot(root);
    measureRepository.addRawMeasure(ROOT_REF, LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_1_REF, LINES_KEY, newMeasureBuilder().create(2));

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(root);

    assertAddedRawMeasure(ROOT_REF, 2);
    assertAddedRawMeasure(FILE_1_REF, 2);
  }

  @Test
  public void fail_when_trying_to_compute_file_measure_already_existing_in_report() {
    ReportComponent root = builder(PROJECT, ROOT_REF)
      .addChildren(
        builder(Component.Type.FILE, FILE_1_REF).build())
      .build();
    treeRootHolder.setRoot(root);
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(2));

    expectedException.expectCause(hasType(UnsupportedOperationException.class)
      .andMessage(String.format("A measure can only be set once for Component (ref=%s), Metric (key=%s)", FILE_1_REF, NCLOC_KEY)));

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(root);
  }

  @Test
  public void fail_on_project_without_children_already_having_computed_measure() {
    ReportComponent root = builder(PROJECT, ROOT_REF).build();
    treeRootHolder.setRoot(root);
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(10));

    expectedException.expectCause(hasType(UnsupportedOperationException.class)
      .andMessage(String.format("A measure can only be set once for Component (ref=%s), Metric (key=%s)", ROOT_REF, NCLOC_KEY)));

    new PathAwareCrawler<>(formulaExecutorComponentVisitor(new FakeFormula()))
      .visit(root);
  }

  private FormulaExecutorComponentVisitor formulaExecutorComponentVisitor(Formula formula) {
    return FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(ImmutableList.of(formula));
  }

  private static Measure createMeasureWithVariation(double variation) {
    return newMeasureBuilder().setVariation(variation).createNoValue();
  }

  private void assertAddedRawMeasure(int componentRef, int expectedValue) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(entryOf(NCLOC_KEY, newMeasureBuilder().create(expectedValue)));
  }

  private void assertAddedRawMeasureVariation(int componentRef, int variation) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(entryOf(NEW_COVERAGE_KEY, createMeasureWithVariation(variation)));
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
      // verify the context which is passed to the method
      assertThat(context.getLeaf().getChildren()).isEmpty();

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
      // verify the context which is passed to the method
      assertThat(context.getLeaf().getChildren()).isEmpty();

      Optional<Measure> measureOptional = context.getMeasure(NEW_LINES_TO_COVER_KEY);
      if (!measureOptional.isPresent()) {
        return;
      }
      this.values.increment((int) measureOptional.get().getVariation());
    }
  }

}
