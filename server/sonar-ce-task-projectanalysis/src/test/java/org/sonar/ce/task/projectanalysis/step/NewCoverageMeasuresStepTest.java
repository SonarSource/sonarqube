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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.scanner.protocol.output.ScannerReport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_LINES_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class NewCoverageMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_1_REF = 1111;
  private static final int FILE_1_REF = 11111;
  private static final int DIRECTORY_2_REF = 1112;
  private static final int FILE_2_REF = 11121;
  private static final int FILE_3_REF = 11122;
  private static final Component FILE_1 = builder(FILE, FILE_1_REF).build();
  private static final Component FILE_2 = builder(FILE, FILE_2_REF).build();
  private static final Component FILE_3 = builder(FILE, FILE_3_REF).build();

  private static final ReportComponent MULTIPLE_FILES_TREE = builder(PROJECT, ROOT_REF)
    .addChildren(
      builder(DIRECTORY, DIRECTORY_1_REF)
        .addChildren(FILE_1)
        .build(),
      builder(DIRECTORY, DIRECTORY_2_REF)
        .addChildren(FILE_2, FILE_3)
        .build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_COVERAGE)
    .add(CoreMetrics.NEW_BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_LINE_COVERAGE);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);
  private NewCoverageMeasuresStep underTest = new NewCoverageMeasuresStep(treeRootHolder, measureRepository, metricRepository, newLinesRepository, reportReader);
  public static final ReportComponent FILE_COMPONENT = ReportComponent.builder(Component.Type.FILE, FILE_1_REF)
    .setFileAttributes(new FileAttributes(false, null, 1)).build();

  @Test
  public void no_measure_for_PROJECT_component() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, ROOT_REF).build());

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_DIRECTORY_component() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.DIRECTORY, DIRECTORY_1_REF).build());

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_unit_test_FILE_component() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.FILE, FILE_1_REF).setFileAttributes(new FileAttributes(true, null, 1)).build());

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measures_for_FILE_component_without_code() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, null, 1)).build());

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void zero_measures_when_nothing_has_changed() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    when(newLinesRepository.getNewLines(FILE_COMPONENT)).thenReturn(Optional.of(Collections.emptySet()));

    reportReader.putCoverage(FILE_COMPONENT.getReportAttributes().getRef(),
      asList(
        ScannerReport.LineCoverage.newBuilder().setLine(2).setHits(true).setConditions(1).setCoveredConditions(1).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(3).setHits(true).build()));

    underTest.execute(new TestComputationStepContext());

    verify_only_zero_measures_on_new_lines_and_conditions_measures(FILE_COMPONENT);
  }

  @Test
  public void zero_measures_for_FILE_component_without_CoverageData() {
    treeRootHolder.setRoot(FILE_1);
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    verify_only_zero_measures_on_new_lines_and_conditions_measures(FILE_1);
  }

  @Test
  public void verify_computation_of_measures_for_new_lines_for_FILE() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);

    treeRootHolder.setRoot(FILE_COMPONENT);
    setNewLines(FILE_1, 1, 2, 4);

    reportReader.putCoverage(FILE_COMPONENT.getReportAttributes().getRef(),
      asList(
        ScannerReport.LineCoverage.newBuilder().setLine(2).setHits(false).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(3).setHits(true).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(4).setHits(true).build()));

    underTest.execute(new TestComputationStepContext());

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_COMPONENT.getReportAttributes().getRef()))).contains(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(2d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(1d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(0d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(0d)));
  }

  @Test
  public void verify_computation_of_measures_for_new_conditions_for_FILE() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    verify_computation_of_measures_for_new_conditions();
  }

  @Test
  public void verify_aggregation_of_measures_for_new_conditions() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);

    treeRootHolder.setRoot(MULTIPLE_FILES_TREE);
    defineNewLinesAndLineCoverage(FILE_1, new LineCoverageValues(3, 4, 1), new LineCoverageValues(0, 3, 2));
    defineNewLinesAndLineCoverage(FILE_2, new LineCoverageValues(0, 14, 6), new LineCoverageValues(0, 13, 7));
    defineNewLinesAndLineCoverage(FILE_3, new LineCoverageValues(3, 4, 1), new LineCoverageValues(1, 13, 7));

    underTest.execute(new TestComputationStepContext());

    // files
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).contains(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(5d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(3d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(7d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(4d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).contains(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(5d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(4d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(27d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(14d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_3_REF))).contains(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(5d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(2d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(17d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(9d)));
    // directories
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_1_REF))).contains(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(5d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(3d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(7d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(4d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_2_REF))).contains(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(10d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(6d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(44d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(23d)));
    // submodule
    MeasureRepoEntry[] repoEntriesFromProject = {entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(15d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(9d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(51d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(27d))};
    // project
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(repoEntriesFromProject);
  }

  private void defineNewLinesAndLineCoverage(Component c, LineCoverageValues line4, LineCoverageValues line6) {
    setNewLines(c, 1, 2, 4, 5, 6, 7);

    reportReader.putCoverage(c.getReportAttributes().getRef(),
      asList(
        ScannerReport.LineCoverage.newBuilder().setLine(2).setHits(false).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(3).setHits(true).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(4).setHits(line4.lineHits > 0).setConditions(line4.conditions).setCoveredConditions(line4.coveredConditions).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(5).setHits(true).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(6).setHits(line6.lineHits > 0).setConditions(line6.conditions).setCoveredConditions(line6.coveredConditions).build(),
        ScannerReport.LineCoverage.newBuilder().setLine(7).setHits(false).build()));
  }

  @Test
  public void verify_aggregates_variations_for_new_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_UNCOVERED_LINES_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY);
    String codeCoverageKey = CoreMetrics.NEW_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  private void verify_aggregates_variations(LinesAndConditionsWithUncoveredMetricKeys metricKeys, String codeCoverageKey, String lineCoverageKey, String branchCoverageKey) {
    treeRootHolder.setRoot(MULTIPLE_FILES_TREE);
    measureRepository
      .addRawMeasure(FILE_1_REF, metricKeys.getLines(), createMeasure(3000d))
      .addRawMeasure(FILE_1_REF, metricKeys.getConditions(), createMeasure(300d))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredLines(), createMeasure(30d))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredConditions(), createMeasure(9d))

      .addRawMeasure(FILE_2_REF, metricKeys.getLines(), createMeasure(2000d))
      .addRawMeasure(FILE_2_REF, metricKeys.getConditions(), createMeasure(400d))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredLines(), createMeasure(200d))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredConditions(), createMeasure(16d));

    underTest.execute(new TestComputationStepContext());

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(98.8d)),
      entryOf(lineCoverageKey, createMeasure(99d)),
      entryOf(branchCoverageKey, createMeasure(97d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(91d)),
      entryOf(lineCoverageKey, createMeasure(90d)),
      entryOf(branchCoverageKey, createMeasure(96d)));
    assertThat(measureRepository.getAddedRawMeasures(FILE_3_REF)).isEmpty();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_1_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(98.8d)),
      entryOf(lineCoverageKey, createMeasure(99d)),
      entryOf(branchCoverageKey, createMeasure(97d)));

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_2_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(91d)),
      entryOf(lineCoverageKey, createMeasure(90d)),
      entryOf(branchCoverageKey, createMeasure(96d)));

    MeasureRepoEntry[] modulesAndProjectEntries = {
      entryOf(codeCoverageKey, createMeasure(95.5d)),
      entryOf(lineCoverageKey, createMeasure(95.4d)),
      entryOf(branchCoverageKey, createMeasure(96.4d))
    };

    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(modulesAndProjectEntries);
  }

  private void verify_only_zero_measures_on_new_lines_and_conditions_measures(Component component) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(component.getReportAttributes().getRef()))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(0d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(0d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(0d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(0d)));
  }

  private static final class LineCoverageValues {
    private final int lineHits;
    private final int conditions;
    private final int coveredConditions;

    public LineCoverageValues(int lineHits, int conditions, int coveredConditions) {
      this.lineHits = lineHits;
      this.conditions = conditions;
      this.coveredConditions = coveredConditions;
    }
  }

  private void verify_computation_of_measures_for_new_conditions() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    defineNewLinesAndLineCoverage(FILE_COMPONENT, new LineCoverageValues(3, 4, 1), new LineCoverageValues(0, 3, 2));

    underTest.execute(new TestComputationStepContext());

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_COMPONENT.getReportAttributes().getRef()))).contains(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(5d)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(3d)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(7d)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(4d)));
  }

  private static Measure createMeasure(Double expectedVariation) {
    return newMeasureBuilder()
      .setVariation(expectedVariation)
      .createNoValue();
  }

  private void setNewLines(Component c, Integer... lines) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    Set<Integer> newLines = new HashSet<>(asList(lines));
    when(newLinesRepository.getNewLines(c)).thenReturn(Optional.of(newLines));
  }
}
