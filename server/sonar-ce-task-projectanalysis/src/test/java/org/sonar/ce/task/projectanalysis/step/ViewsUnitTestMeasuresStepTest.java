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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS;
import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_SUCCESS_DENSITY;
import static org.sonar.api.measures.CoreMetrics.TEST_SUCCESS_DENSITY_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ViewsUnitTestMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_REF = 12;
  private static final int SUB_SUBVIEW_1_REF = 123;
  private static final int PROJECT_VIEW_1_REF = 12341;
  private static final int PROJECT_VIEW_2_REF = 12342;
  private static final int SUB_SUBVIEW_2_REF = 124;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(VIEW, ROOT_REF)
        .addChildren(
          builder(SUBVIEW, SUBVIEW_REF)
            .addChildren(
              builder(SUBVIEW, SUB_SUBVIEW_1_REF)
                .addChildren(
                  builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
                  builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
                .build(),
              builder(SUBVIEW, SUB_SUBVIEW_2_REF).build())
            .build())
        .build());
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(TESTS)
    .add(TEST_ERRORS)
    .add(TEST_FAILURES)
    .add(TEST_EXECUTION_TIME)
    .add(SKIPPED_TESTS)
    .add(TEST_SUCCESS_DENSITY);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputationStep underTest = new UnitTestMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  public void aggregate_tests() {
    checkMeasuresAggregation(TESTS_KEY, 100, 400, 500);
  }

  @Test
  public void aggregate_tests_in_errors() {
    checkMeasuresAggregation(TEST_ERRORS_KEY, 100, 400, 500);
  }

  @Test
  public void aggregate_tests_in_failures() {
    checkMeasuresAggregation(TEST_FAILURES_KEY, 100, 400, 500);
  }

  @Test
  public void aggregate_tests_execution_time() {
    checkMeasuresAggregation(TEST_EXECUTION_TIME_KEY, 100L, 400L, 500L);
  }

  @Test
  public void aggregate_skipped_tests_time() {
    checkMeasuresAggregation(SKIPPED_TESTS_KEY, 100, 400, 500);
  }

  @Test
  public void compute_test_success_density() {
    addedRawMeasure(PROJECT_VIEW_1_REF, TESTS_KEY, 10);
    addedRawMeasure(PROJECT_VIEW_2_REF, TESTS_KEY, 20);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_ERRORS_KEY, 2);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_ERRORS_KEY, 5);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_FAILURES_KEY, 4);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_FAILURES_KEY, 1);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasureValue(SUB_SUBVIEW_1_REF, TEST_SUCCESS_DENSITY_KEY, 60d);
    assertAddedRawMeasureValue(SUBVIEW_REF, TEST_SUCCESS_DENSITY_KEY, 60d);
    assertAddedRawMeasureValue(ROOT_REF, TEST_SUCCESS_DENSITY_KEY, 60d);
  }

  @Test
  public void compute_test_success_density_when_zero_tests_in_errors() {
    addedRawMeasure(PROJECT_VIEW_1_REF, TESTS_KEY, 10);
    addedRawMeasure(PROJECT_VIEW_2_REF, TESTS_KEY, 20);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_ERRORS_KEY, 0);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_ERRORS_KEY, 0);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_FAILURES_KEY, 4);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_FAILURES_KEY, 1);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasureValue(SUB_SUBVIEW_1_REF, TEST_SUCCESS_DENSITY_KEY, 83.3d);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF, TEST_SUCCESS_DENSITY_KEY);
    assertAddedRawMeasureValue(SUBVIEW_REF, TEST_SUCCESS_DENSITY_KEY, 83.3d);
    assertAddedRawMeasureValue(ROOT_REF, TEST_SUCCESS_DENSITY_KEY, 83.3d);
  }

  @Test
  public void compute_test_success_density_when_zero_tests_in_failures() {
    addedRawMeasure(PROJECT_VIEW_1_REF, TESTS_KEY, 10);
    addedRawMeasure(PROJECT_VIEW_2_REF, TESTS_KEY, 20);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_ERRORS_KEY, 2);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_ERRORS_KEY, 5);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_FAILURES_KEY, 0);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_FAILURES_KEY, 0);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasureValue(SUB_SUBVIEW_1_REF, TEST_SUCCESS_DENSITY_KEY, 76.7d);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF, TEST_SUCCESS_DENSITY_KEY);
    assertAddedRawMeasureValue(SUBVIEW_REF, TEST_SUCCESS_DENSITY_KEY, 76.7d);
    assertAddedRawMeasureValue(ROOT_REF, TEST_SUCCESS_DENSITY_KEY, 76.7d);
  }

  @Test
  public void compute_100_percent_test_success_density_when_no_tests_in_errors_or_failures() {
    addedRawMeasure(PROJECT_VIEW_1_REF, TESTS_KEY, 10);
    addedRawMeasure(PROJECT_VIEW_2_REF, TESTS_KEY, 20);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_ERRORS_KEY, 0);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_ERRORS_KEY, 0);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_FAILURES_KEY, 0);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_FAILURES_KEY, 0);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasureValue(SUB_SUBVIEW_1_REF, TEST_SUCCESS_DENSITY_KEY, 100d);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF, TEST_SUCCESS_DENSITY_KEY);
    assertAddedRawMeasureValue(SUBVIEW_REF, TEST_SUCCESS_DENSITY_KEY, 100d);
    assertAddedRawMeasureValue(ROOT_REF, TEST_SUCCESS_DENSITY_KEY, 100d);
  }

  @Test
  public void compute_0_percent_test_success_density() {
    int value = 10;
    String metricKey = TESTS_KEY;
    int componentRef = PROJECT_VIEW_1_REF;
    addedRawMeasure(componentRef, metricKey, value);
    addedRawMeasure(PROJECT_VIEW_2_REF, TESTS_KEY, 20);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_ERRORS_KEY, 8);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_ERRORS_KEY, 15);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_FAILURES_KEY, 2);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_FAILURES_KEY, 5);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasureValue(SUB_SUBVIEW_1_REF, TEST_SUCCESS_DENSITY_KEY, 0d);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF, TEST_SUCCESS_DENSITY_KEY);
    assertAddedRawMeasureValue(SUBVIEW_REF, TEST_SUCCESS_DENSITY_KEY, 0d);
    assertAddedRawMeasureValue(ROOT_REF, TEST_SUCCESS_DENSITY_KEY, 0d);
  }

  @Test
  public void do_not_compute_test_success_density_when_no_tests_in_errors() {
    addedRawMeasure(PROJECT_VIEW_1_REF, TESTS_KEY, 10);
    addedRawMeasure(PROJECT_VIEW_2_REF, TESTS_KEY, 20);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_FAILURES_KEY, 4);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_FAILURES_KEY, 1);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertNoAddedRawMeasure(SUB_SUBVIEW_1_REF, TEST_SUCCESS_DENSITY_KEY);
    assertNoAddedRawMeasure(SUBVIEW_REF, TEST_SUCCESS_DENSITY_KEY);
    assertNoAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY);
  }

  @Test
  public void do_not_compute_test_success_density_when_no_tests_in_failure() {
    addedRawMeasure(PROJECT_VIEW_1_REF, TESTS_KEY, 10);
    addedRawMeasure(PROJECT_VIEW_2_REF, TESTS_KEY, 20);

    addedRawMeasure(PROJECT_VIEW_1_REF, TEST_ERRORS_KEY, 0);
    addedRawMeasure(PROJECT_VIEW_2_REF, TEST_ERRORS_KEY, 0);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertNoAddedRawMeasure(SUB_SUBVIEW_1_REF, TEST_SUCCESS_DENSITY_KEY);
    assertNoAddedRawMeasure(SUBVIEW_REF, TEST_SUCCESS_DENSITY_KEY);
    assertNoAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY);
  }

  private void checkMeasuresAggregation(String metricKey, int file1Value, int file2Value, int expectedValue) {
    addedRawMeasure(PROJECT_VIEW_1_REF, metricKey, file1Value);
    addedRawMeasure(PROJECT_VIEW_2_REF, metricKey, file2Value);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasureValue(SUB_SUBVIEW_1_REF, metricKey, expectedValue);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF, TEST_SUCCESS_DENSITY_KEY);
    assertAddedRawMeasureValue(SUBVIEW_REF, metricKey, expectedValue);
    assertAddedRawMeasureValue(ROOT_REF, metricKey, expectedValue);
  }

  private void checkMeasuresAggregation(String metricKey, long file1Value, long file2Value, long expectedValue) {
    measureRepository.addRawMeasure(PROJECT_VIEW_1_REF, metricKey, newMeasureBuilder().create(file1Value));
    measureRepository.addRawMeasure(PROJECT_VIEW_2_REF, metricKey, newMeasureBuilder().create(file2Value));

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasureValue(SUB_SUBVIEW_1_REF, metricKey, expectedValue);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF, TEST_SUCCESS_DENSITY_KEY);
    assertAddedRawMeasureValue(SUBVIEW_REF, metricKey, expectedValue);
    assertAddedRawMeasureValue(ROOT_REF, metricKey, expectedValue);
  }

  private void addedRawMeasure(int componentRef, String metricKey, int value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void assertAddedRawMeasureValue(int componentRef, String metricKey, double value) {
    assertThat(entryOf(metricKey, measureRepository.getAddedRawMeasure(componentRef, metricKey).get()))
      .isEqualTo(entryOf(metricKey, newMeasureBuilder().create(value, 1)));
  }

  private void assertAddedRawMeasureValue(int componentRef, String metricKey, long value) {
    assertThat(entryOf(metricKey, measureRepository.getAddedRawMeasure(componentRef, metricKey).get()))
      .isEqualTo(entryOf(metricKey, newMeasureBuilder().create(value)));
  }

  private void assertNoAddedRawMeasure(int componentRef, String metricKey) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey)).isNotPresent();
  }

  private void assertAddedRawMeasureValue(int componentRef, String metricKey, int expectedValue) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
  }

  private void assertNoAddedRawMeasureOnProjectViews() {
    assertThat(measureRepository.getAddedRawMeasures(PROJECT_VIEW_1_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(PROJECT_VIEW_2_REF)).isEmpty();
  }

}
