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
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class CoverageMeasuresStepTest {

  private static final Offset<Double> DEFAULT_OFFSET = Offset.offset(0.1d);

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_REF = 123;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int UNIT_TEST_FILE_REF = 12342;
  private static final int FILE_2_REF = 12343;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule()
    .setPeriods(
      new Period(2, "mode2", null, 52L, 96L),
      new Period(5, "mode5", null, 52L, 96L)
    );
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINES_TO_COVER)
    .add(CoreMetrics.CONDITIONS_TO_COVER)
    .add(CoreMetrics.UNCOVERED_LINES)
    .add(CoreMetrics.UNCOVERED_CONDITIONS)
    .add(CoreMetrics.COVERAGE)
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_COVERAGE)
    .add(CoreMetrics.IT_LINES_TO_COVER)
    .add(CoreMetrics.IT_CONDITIONS_TO_COVER)
    .add(CoreMetrics.IT_UNCOVERED_LINES)
    .add(CoreMetrics.IT_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.IT_COVERAGE)
    .add(CoreMetrics.NEW_IT_LINES_TO_COVER)
    .add(CoreMetrics.NEW_IT_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_IT_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_IT_COVERAGE)
    .add(CoreMetrics.OVERALL_LINES_TO_COVER)
    .add(CoreMetrics.OVERALL_CONDITIONS_TO_COVER)
    .add(CoreMetrics.OVERALL_UNCOVERED_LINES)
    .add(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.OVERALL_COVERAGE)
    .add(CoreMetrics.NEW_OVERALL_LINES_TO_COVER)
    .add(CoreMetrics.NEW_OVERALL_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_OVERALL_COVERAGE)
    .add(CoreMetrics.BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_BRANCH_COVERAGE)
    .add(CoreMetrics.IT_BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_IT_BRANCH_COVERAGE)
    .add(CoreMetrics.OVERALL_BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_OVERALL_BRANCH_COVERAGE)
    .add(CoreMetrics.LINE_COVERAGE)
    .add(CoreMetrics.NEW_LINE_COVERAGE)
    .add(CoreMetrics.IT_LINE_COVERAGE)
    .add(CoreMetrics.NEW_IT_LINE_COVERAGE)
    .add(CoreMetrics.OVERALL_LINE_COVERAGE)
    .add(CoreMetrics.NEW_OVERALL_LINE_COVERAGE);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  CoverageMeasuresStep underTest = new CoverageMeasuresStep(treeRootHolder, periodsHolder, metricRepository, measureRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(MODULE, SUB_MODULE_REF)
                .addChildren(
                  builder(DIRECTORY, DIRECTORY_REF)
                    .addChildren(
                      builder(FILE, FILE_1_REF).build(),
                      builder(FILE, UNIT_TEST_FILE_REF).setFileAttributes(new FileAttributes(true, "some language")).build(),
                      builder(FILE, FILE_2_REF).build()
                    ).build()
                ).build()
            ).build()
        ).build());
  }

  @Test
  public void verify_aggregates_values_for_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.CONDITIONS_TO_COVER_KEY,
      CoreMetrics.UNCOVERED_LINES_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY
      );
    String codeCoverageKey = CoreMetrics.COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.BRANCH_COVERAGE_KEY;

    verify_aggregates_values(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_values_for_IT_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.IT_LINES_TO_COVER_KEY, CoreMetrics.IT_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.IT_UNCOVERED_LINES_KEY, CoreMetrics.IT_UNCOVERED_CONDITIONS_KEY
      );
    String codeCoverageKey = CoreMetrics.IT_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.IT_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.IT_BRANCH_COVERAGE_KEY;

    verify_aggregates_values(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_values_for_Overall_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.OVERALL_LINES_TO_COVER_KEY, CoreMetrics.OVERALL_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS_KEY
      );
    String codeCoverageKey = CoreMetrics.OVERALL_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.OVERALL_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.OVERALL_BRANCH_COVERAGE_KEY;

    verify_aggregates_values(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  private void verify_aggregates_values(LinesAndConditionsWithUncoveredMetricKeys metricKeys, String codeCoverageKey, String lineCoverageKey, String branchCoverageKey) {
    measureRepository
      .addRawMeasure(FILE_1_REF, metricKeys.getLines(), newMeasureBuilder().create(3000L))
      .addRawMeasure(FILE_1_REF, metricKeys.getConditions(), newMeasureBuilder().create(300L))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(30L))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(9L))

      .addRawMeasure(FILE_2_REF, metricKeys.getLines(), newMeasureBuilder().create(2000L))
      .addRawMeasure(FILE_2_REF, metricKeys.getConditions(), newMeasureBuilder().create(400L))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(200L))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(16L))

      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getLines(), newMeasureBuilder().create(1000L))
      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getConditions(), newMeasureBuilder().create(100L))
      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(10L))
      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(3L));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, codeCoverageKey).get().getDoubleValue()).isEqualTo(98.8d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, lineCoverageKey).get().getDoubleValue()).isEqualTo(99d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, branchCoverageKey).get().getDoubleValue()).isEqualTo(97d, DEFAULT_OFFSET);

    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, codeCoverageKey).get().getDoubleValue()).isEqualTo(91d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, lineCoverageKey).get().getDoubleValue()).isEqualTo(90d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, branchCoverageKey).get().getDoubleValue()).isEqualTo(96d, DEFAULT_OFFSET);

    assertThat(measureRepository.getAddedRawMeasures(UNIT_TEST_FILE_REF)).isEmpty();

    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, codeCoverageKey).get().getDoubleValue()).isEqualTo(95.5d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, lineCoverageKey).get().getDoubleValue()).isEqualTo(95.4d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, branchCoverageKey).get().getDoubleValue()).isEqualTo(96.4d, DEFAULT_OFFSET);

    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, codeCoverageKey).get().getDoubleValue()).isEqualTo(95.5d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, lineCoverageKey).get().getDoubleValue()).isEqualTo(95.4d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(SUB_MODULE_REF, branchCoverageKey).get().getDoubleValue()).isEqualTo(96.4d, DEFAULT_OFFSET);

    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, codeCoverageKey).get().getDoubleValue()).isEqualTo(95.5d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, lineCoverageKey).get().getDoubleValue()).isEqualTo(95.4d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(MODULE_REF, branchCoverageKey).get().getDoubleValue()).isEqualTo(96.4d, DEFAULT_OFFSET);

    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, codeCoverageKey).get().getDoubleValue()).isEqualTo(95.5d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, lineCoverageKey).get().getDoubleValue()).isEqualTo(95.4d, DEFAULT_OFFSET);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, branchCoverageKey).get().getDoubleValue()).isEqualTo(96.4d, DEFAULT_OFFSET);
  }

  @Test
  public void verify_aggregates_variations_for_new_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_UNCOVERED_LINES_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY
      );
    String codeCoverageKey = CoreMetrics.NEW_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_variations_for_new_IT_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_IT_LINES_TO_COVER_KEY, CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY
      );
    String codeCoverageKey = CoreMetrics.NEW_IT_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_IT_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_IT_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_variations_for_new_Overall_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY
      );
    String codeCoverageKey = CoreMetrics.NEW_OVERALL_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_OVERALL_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_OVERALL_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  private void verify_aggregates_variations(LinesAndConditionsWithUncoveredMetricKeys metricKeys, String codeCoverageKey, String lineCoverageKey, String branchCoverageKey) {
    measureRepository
      .addRawMeasure(FILE_1_REF, metricKeys.getLines(), measureWithVariation(3000L, 2000L))
      .addRawMeasure(FILE_1_REF, metricKeys.getConditions(), measureWithVariation(300L, 400L))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredLines(), measureWithVariation(30L, 200L))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredConditions(), measureWithVariation(9L, 16L))

      .addRawMeasure(FILE_2_REF, metricKeys.getLines(), measureWithVariation(2000L, 3000L))
      .addRawMeasure(FILE_2_REF, metricKeys.getConditions(), measureWithVariation(400L, 300L))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredLines(), measureWithVariation(200L, 30L))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredConditions(), measureWithVariation(16L, 9L))

      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getLines(), measureWithVariation(1000L, 2000L))
      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getConditions(), measureWithVariation(100L, 400L))
      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getUncoveredLines(), measureWithVariation(10L, 200L))
      .addRawMeasure(UNIT_TEST_FILE_REF, metricKeys.getUncoveredConditions(), measureWithVariation(3L, 16L));

    underTest.execute();

    checkMeasureVariations(FILE_1_REF, codeCoverageKey, null, 98.8, null, null, 91d);
    checkMeasureVariations(FILE_1_REF, lineCoverageKey, null, 99d, null, null, 90d);
    checkMeasureVariations(FILE_1_REF, branchCoverageKey, null, 97d, null, null, 96d);

    checkMeasureVariations(FILE_2_REF, codeCoverageKey, null, 91d, null, null, 98.8d);
    checkMeasureVariations(FILE_2_REF, lineCoverageKey, null, 90d, null, null, 99d);
    checkMeasureVariations(FILE_2_REF, branchCoverageKey, null, 96d, null, null, 97d);

    assertThat(measureRepository.getAddedRawMeasures(UNIT_TEST_FILE_REF)).isEmpty();

    checkMeasureVariations(DIRECTORY_REF, codeCoverageKey, null, 95.5d, null, null, 95.5d);
    checkMeasureVariations(DIRECTORY_REF, lineCoverageKey, null, 95.4d, null, null, 95.4d);
    checkMeasureVariations(DIRECTORY_REF, branchCoverageKey, null, 96.4d, null, null, 96.4d);

    checkMeasureVariations(SUB_MODULE_REF, codeCoverageKey, null, 95.5d, null, null, 95.5d);
    checkMeasureVariations(SUB_MODULE_REF, lineCoverageKey, null, 95.4d, null, null, 95.4d);
    checkMeasureVariations(SUB_MODULE_REF, branchCoverageKey, null, 96.4d, null, null, 96.4d);

    checkMeasureVariations(MODULE_REF, codeCoverageKey, null, 95.5d, null, null, 95.5d);
    checkMeasureVariations(MODULE_REF, lineCoverageKey, null, 95.4d, null, null, 95.4d);
    checkMeasureVariations(MODULE_REF, branchCoverageKey, null, 96.4d, null, null, 96.4d);

    checkMeasureVariations(ROOT_REF, codeCoverageKey, null, 95.5d, null, null, 95.5d);
    checkMeasureVariations(ROOT_REF, lineCoverageKey, null, 95.4d, null, null, 95.4d);
    checkMeasureVariations(ROOT_REF, branchCoverageKey, null, 96.4d, null, null, 96.4d);
  }

  private static Measure measureWithVariation(double variation2, double variation5) {
    return newMeasureBuilder().setVariations(new MeasureVariations(null, variation2, null, null, variation5)).createNoValue();
  }

  private void checkMeasureVariations(int fileRef, String metricKey, Double... expectedVariations) {
    Optional<Measure> measure = measureRepository.getAddedRawMeasure(fileRef, metricKey);
    if (measure.isPresent()) {
      MeasureVariations measureVariations = measure.get().getVariations();
      for (int i = 0; i < expectedVariations.length - 1; i++) {
        Double expectedVariation = expectedVariations[i];
        int period = i + 1;
        if (expectedVariation != null) {
          assertThat(measureVariations.hasVariation(period)).isTrue();
          assertThat(measureVariations.getVariation(period)).isEqualTo(expectedVariation, DEFAULT_OFFSET);
        } else {
          assertThat(measureVariations.hasVariation(period)).isFalse();
        }
      }
    } else {
      fail(String.format("No measure on metric '%s' for component '%s'", metricKey, fileRef));
    }
  }
}
