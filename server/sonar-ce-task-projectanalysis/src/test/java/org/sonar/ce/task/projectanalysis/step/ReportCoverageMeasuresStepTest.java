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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.scanner.protocol.output.ScannerReport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ReportCoverageMeasuresStepTest {
  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int UNIT_TEST_FILE_REF = 12342;
  private static final int FILE_2_REF = 12343;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINES_TO_COVER)
    .add(CoreMetrics.CONDITIONS_TO_COVER)
    .add(CoreMetrics.UNCOVERED_LINES)
    .add(CoreMetrics.UNCOVERED_CONDITIONS)
    .add(CoreMetrics.COVERAGE)
    .add(CoreMetrics.BRANCH_COVERAGE)
    .add(CoreMetrics.LINE_COVERAGE);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  CoverageMeasuresStep underTest = new CoverageMeasuresStep(treeRootHolder, metricRepository, measureRepository, reportReader);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF)
            .addChildren(
              builder(FILE, FILE_1_REF).build(),
              builder(FILE, UNIT_TEST_FILE_REF).setFileAttributes(new FileAttributes(true, "some language", 1)).build(),
              builder(FILE, FILE_2_REF).build())
            .build())
        .build());
  }

  @Test
  public void verify_aggregates_values_for_lines_and_conditions() {

    reportReader.putCoverage(FILE_1_REF,
      asList(
        ScannerReport.LineCoverage.newBuilder().setLine(2).setHits(false).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(3).setHits(true).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(4).setHits(true).setConditions(4).setCoveredConditions(1).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(5).setConditions(8).setCoveredConditions(2).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(6).setHits(false).setConditions(3).setCoveredConditions(0).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(7).setHits(false).build()));

    reportReader.putCoverage(FILE_2_REF,
      asList(
        ScannerReport.LineCoverage.newBuilder().setLine(2).setHits(true).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(3).setHits(false).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(5).setHits(true).setConditions(5).setCoveredConditions(1).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(6).setConditions(10).setCoveredConditions(3).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(7).setHits(false).setConditions(1).setCoveredConditions(0).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(8).setHits(false).build()));

    underTest.execute(new TestComputationStepContext());

    MeasureRepoEntry[] nonFileRepoEntries = {
      entryOf(LINES_TO_COVER_KEY, newMeasureBuilder().create(5 + 5)),
      entryOf(CONDITIONS_TO_COVER_KEY, newMeasureBuilder().create(4 + 8 + 3 + 5 + 10 + 1)),
      entryOf(UNCOVERED_LINES_KEY, newMeasureBuilder().create(3 + 3)),
      entryOf(UNCOVERED_CONDITIONS_KEY, newMeasureBuilder().create(3 + 6 + 3 + 4 + 7 + 1))
    };

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(nonFileRepoEntries);
  }

  @Test
  public void verify_aggregates_values_for_code_line_and_branch_coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      LINES_TO_COVER_KEY, CONDITIONS_TO_COVER_KEY,
      UNCOVERED_LINES_KEY, UNCOVERED_CONDITIONS_KEY);
    String codeCoverageKey = CoreMetrics.COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.BRANCH_COVERAGE_KEY;

    verify_coverage_aggregates_values(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  private void verify_coverage_aggregates_values(LinesAndConditionsWithUncoveredMetricKeys metricKeys, String codeCoverageKey, String lineCoverageKey, String branchCoverageKey) {
    measureRepository
      .addRawMeasure(FILE_1_REF, metricKeys.getLines(), newMeasureBuilder().create(3000))
      .addRawMeasure(FILE_1_REF, metricKeys.getConditions(), newMeasureBuilder().create(300))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(30))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(9))

      .addRawMeasure(FILE_2_REF, metricKeys.getLines(), newMeasureBuilder().create(2000))
      .addRawMeasure(FILE_2_REF, metricKeys.getConditions(), newMeasureBuilder().create(400))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(200))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(16));

    underTest.execute(new TestComputationStepContext());

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).contains(
      entryOf(codeCoverageKey, newMeasureBuilder().create(98.8d, 1)),
      entryOf(lineCoverageKey, newMeasureBuilder().create(99d, 1)),
      entryOf(branchCoverageKey, newMeasureBuilder().create(97d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).contains(
      entryOf(codeCoverageKey, newMeasureBuilder().create(91d, 1)),
      entryOf(lineCoverageKey, newMeasureBuilder().create(90d, 1)),
      entryOf(branchCoverageKey, newMeasureBuilder().create(96d, 1)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(UNIT_TEST_FILE_REF))).isEmpty();

    MeasureRepoEntry[] nonFileRepoEntries = {
      entryOf(codeCoverageKey, newMeasureBuilder().create(95.5d, 1)),
      entryOf(lineCoverageKey, newMeasureBuilder().create(95.4d, 1)),
      entryOf(branchCoverageKey, newMeasureBuilder().create(96.4d, 1))
    };

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(nonFileRepoEntries);
  }

}
