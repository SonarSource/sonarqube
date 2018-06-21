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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CLASSES;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ViewsComplexityMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_REF = 11;
  private static final int SUB_SUBVIEW_1_REF = 111;
  private static final int PROJECT_VIEW_1_REF = 11111;
  private static final int PROJECT_VIEW_2_REF = 11121;
  private static final int SUB_SUBVIEW_2_REF = 112;
  private static final int PROJECT_VIEW_3_REF = 12;

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(VIEW, ROOT_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_REF)
          .addChildren(
            builder(SUBVIEW, SUB_SUBVIEW_1_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
                builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
              .build(),
            builder(SUBVIEW, SUB_SUBVIEW_2_REF).build())
          .build(),
        builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
      .build());
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(COMPLEXITY)
    .add(COMPLEXITY_IN_CLASSES)
    .add(COMPLEXITY_IN_FUNCTIONS)
    .add(FUNCTION_COMPLEXITY_DISTRIBUTION)
    .add(FILE_COMPLEXITY_DISTRIBUTION)
    .add(CLASS_COMPLEXITY_DISTRIBUTION)
    .add(FILE_COMPLEXITY)
    .add(FILES)
    .add(CLASS_COMPLEXITY)
    .add(CLASSES)
    .add(FUNCTION_COMPLEXITY)
    .add(FUNCTIONS)
    .add(COGNITIVE_COMPLEXITY);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private ComputationStep underTest = new ComplexityMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  public void aggregate_complexity() {
    verify_sum_aggregation(COMPLEXITY_KEY);
  }

  @Test
  public void aggregate_complexity_in_classes() {
    verify_sum_aggregation(COMPLEXITY_IN_CLASSES_KEY);
  }

  @Test
  public void aggregate_complexity_in_functions() {
    verify_sum_aggregation(COMPLEXITY_IN_FUNCTIONS_KEY);
  }

  @Test
  public void aggregate_cognitive_complexity_in_functions() {
    verify_sum_aggregation(COGNITIVE_COMPLEXITY_KEY);
  }

  private void verify_sum_aggregation(String metricKey) {
    addRawMeasureValue(PROJECT_VIEW_1_REF, metricKey, 10);
    addRawMeasureValue(PROJECT_VIEW_2_REF, metricKey, 40);
    addRawMeasureValue(PROJECT_VIEW_3_REF, metricKey, 20);

    underTest.execute();

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasures(SUB_SUBVIEW_1_REF, metricKey, 50);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF);
    assertAddedRawMeasures(SUBVIEW_REF, metricKey, 50);
    assertAddedRawMeasures(ROOT_REF, metricKey, 70);
  }

  @Test
  public void aggregate_function_complexity_distribution() {
    verify_distribution_aggregation(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY);
  }

  @Test
  public void aggregate_file_complexity_distribution() {
    verify_distribution_aggregation(FILE_COMPLEXITY_DISTRIBUTION_KEY);
  }

  @Test
  public void aggregate_class_complexity_distribution() {
    verify_distribution_aggregation(CLASS_COMPLEXITY_DISTRIBUTION_KEY);
  }

  private void verify_distribution_aggregation(String metricKey) {
    addRawMeasure(PROJECT_VIEW_1_REF, metricKey, "0.5=3;3.5=5;6.5=9");
    addRawMeasure(PROJECT_VIEW_2_REF, metricKey, "0.5=0;3.5=2;6.5=1");
    addRawMeasure(PROJECT_VIEW_3_REF, metricKey, "0.5=1;3.5=1;6.5=0");

    underTest.execute();

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasures(SUB_SUBVIEW_1_REF, metricKey, "0.5=3;3.5=7;6.5=10");
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF);
    assertAddedRawMeasures(SUBVIEW_REF, metricKey, "0.5=3;3.5=7;6.5=10");
    assertAddedRawMeasures(ROOT_REF, metricKey, "0.5=4;3.5=8;6.5=10");
  }

  @Test
  public void compute_and_aggregate_file_complexity() {
    verify_average_compute_and_aggregation(FILE_COMPLEXITY_KEY, COMPLEXITY_KEY, FILES_KEY);
  }

  @Test
  public void compute_and_aggregate_class_complexity() {
    verify_average_compute_and_aggregation(CLASS_COMPLEXITY_KEY, COMPLEXITY_IN_CLASSES_KEY, CLASSES_KEY);
  }

  @Test
  public void compute_and_aggregate_function_complexity() {
    verify_average_compute_and_aggregation(FUNCTION_COMPLEXITY_KEY, COMPLEXITY_IN_FUNCTIONS_KEY, FUNCTIONS_KEY);
  }

  private void verify_average_compute_and_aggregation(String metricKey, String mainMetric, String byMetric) {
    addRawMeasureValue(PROJECT_VIEW_1_REF, mainMetric, 5);
    addRawMeasureValue(PROJECT_VIEW_1_REF, byMetric, 2);

    addRawMeasureValue(PROJECT_VIEW_2_REF, mainMetric, 1);
    addRawMeasureValue(PROJECT_VIEW_2_REF, byMetric, 1);

    addRawMeasureValue(PROJECT_VIEW_3_REF, mainMetric, 6);
    addRawMeasureValue(PROJECT_VIEW_3_REF, byMetric, 8);

    underTest.execute();

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasures(SUB_SUBVIEW_1_REF, metricKey, 2d);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF);
    assertAddedRawMeasures(SUBVIEW_REF, metricKey, 2d);
    assertAddedRawMeasures(ROOT_REF, metricKey, 1.1d);
  }

  private void addRawMeasure(int componentRef, String metricKey, String value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void assertNoAddedRawMeasureOnProjectViews() {
    assertNoAddedRawMeasure(PROJECT_VIEW_1_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_2_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_3_REF);
  }

  private void assertNoAddedRawMeasure(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef)).isEmpty();
  }

  private void assertAddedRawMeasures(int componentRef, String metricKey, String expected) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(expected)));
  }

  private void assertAddedRawMeasures(int componentRef, String metricKey, int expected) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(expected)));
  }

  private void assertAddedRawMeasures(int componentRef, String metricKey, double expected) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(expected, 1)));
  }

  private void addRawMeasureValue(int componentRef, String metricKey, int value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

}
