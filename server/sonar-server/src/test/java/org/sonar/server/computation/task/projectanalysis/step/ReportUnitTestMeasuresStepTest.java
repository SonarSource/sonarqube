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

import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.FileAttributes;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

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
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ReportUnitTestMeasuresStepTest {

  private static final Offset<Double> DEFAULT_OFFSET = Offset.offset(0.01d);

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_REF = 123;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12342;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(MODULE, SUB_MODULE_REF)
                .addChildren(
                  builder(DIRECTORY, DIRECTORY_REF)
                    .addChildren(
                      builder(FILE, FILE_1_REF).setFileAttributes(new FileAttributes(true, null, 1)).build(),
                      builder(FILE, FILE_2_REF).setFileAttributes(new FileAttributes(true, null, 1)).build())
                    .build())
                .build())
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
    measureRepository.addRawMeasure(FILE_1_REF, TESTS_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, TESTS_KEY, newMeasureBuilder().create(20));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(5));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(4));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(1));

    underTest.execute();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).contains(entryOf(TEST_SUCCESS_DENSITY_KEY, newMeasureBuilder().create(40d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).contains(entryOf(TEST_SUCCESS_DENSITY_KEY, newMeasureBuilder().create(70d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(entryOf(TEST_SUCCESS_DENSITY_KEY, newMeasureBuilder().create(60d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).contains(entryOf(TEST_SUCCESS_DENSITY_KEY, newMeasureBuilder().create(60d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).contains(entryOf(TEST_SUCCESS_DENSITY_KEY, newMeasureBuilder().create(60d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(entryOf(TEST_SUCCESS_DENSITY_KEY, newMeasureBuilder().create(60d, 1)));
  }

  @Test
  public void compute_test_success_density_when_zero_tests_in_errors() {
    measureRepository.addRawMeasure(FILE_1_REF, TESTS_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, TESTS_KEY, newMeasureBuilder().create(20));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(0));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(4));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(1));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(60d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(95d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(83.3d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(83.3d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(83.3d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(83.3d, DEFAULT_OFFSET);
  }

  @Test
  public void compute_test_success_density_when_zero_tests_in_failures() {
    measureRepository.addRawMeasure(FILE_1_REF, TESTS_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, TESTS_KEY, newMeasureBuilder().create(20));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(5));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(0));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(80d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(75d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(76.7d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(76.7d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(76.7d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(76.7d, DEFAULT_OFFSET);
  }

  @Test
  public void compute_100_percent_test_success_density_when_no_tests_in_errors_or_failures() {
    measureRepository.addRawMeasure(FILE_1_REF, TESTS_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, TESTS_KEY, newMeasureBuilder().create(20));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(0));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(0));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
  }

  @Test
  public void compute_0_percent_test_success_density() {
    measureRepository.addRawMeasure(FILE_1_REF, TESTS_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, TESTS_KEY, newMeasureBuilder().create(20));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(15));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(5));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
  }

  @Test
  public void do_not_compute_test_success_density_when_no_tests_in_errors() {
    measureRepository.addRawMeasure(FILE_1_REF, TESTS_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, TESTS_KEY, newMeasureBuilder().create(20));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(4));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_FAILURES_KEY, newMeasureBuilder().create(1));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
  }

  @Test
  public void do_not_compute_test_success_density_when_no_tests_in_failure() {
    measureRepository.addRawMeasure(FILE_1_REF, TESTS_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, TESTS_KEY, newMeasureBuilder().create(20));

    measureRepository.addRawMeasure(FILE_1_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, TEST_ERRORS_KEY, newMeasureBuilder().create(0));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, TEST_SUCCESS_DENSITY_KEY)).isAbsent();
  }

  private void checkMeasuresAggregation(String metricKey, int file1Value, int file2Value, int expectedValue) {
    measureRepository.addRawMeasure(FILE_1_REF, metricKey, newMeasureBuilder().create(file1Value));
    measureRepository.addRawMeasure(FILE_2_REF, metricKey, newMeasureBuilder().create(file2Value));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, metricKey)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, metricKey)).isAbsent();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
  }

  private void checkMeasuresAggregation(String metricKey, long file1Value, long file2Value, long expectedValue) {
    measureRepository.addRawMeasure(FILE_1_REF, metricKey, newMeasureBuilder().create(file1Value));
    measureRepository.addRawMeasure(FILE_2_REF, metricKey, newMeasureBuilder().create(file2Value));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, metricKey)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, metricKey)).isAbsent();
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(expectedValue)));
  }
}
