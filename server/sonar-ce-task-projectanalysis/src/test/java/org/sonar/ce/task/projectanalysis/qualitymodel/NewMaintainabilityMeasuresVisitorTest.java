/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.server.measure.DebtRatingGrid;
import org.sonar.server.measure.Rating;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DEVELOPMENT_COST;
import static org.sonar.api.measures.CoreMetrics.NEW_DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SQALE_DEBT_RATIO;
import static org.sonar.api.measures.CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureAssert.assertThat;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY;

public class NewMaintainabilityMeasuresVisitorTest {

  private static final double[] RATING_GRID = new double[] {0.1, 0.2, 0.5, 1};

  private static final String LANGUAGE_1_KEY = "language 1 key";
  private static final long DEV_COST = 30L;
  private static final int ROOT_REF = 1;
  private static final int LANGUAGE_1_FILE_REF = 11111;
  private static final Offset<Double> VALUE_COMPARISON_OFFSET = Offset.offset(0.01);

  @RegisterExtension
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @RegisterExtension
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_TECHNICAL_DEBT)
    .add(NCLOC_DATA)
    .add(NEW_SQALE_DEBT_RATIO)
    .add(NEW_MAINTAINABILITY_RATING)
    .add(NEW_DEVELOPMENT_COST)
    .add(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT)
    .add(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO)
    .add(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING);

  @RegisterExtension
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);
  private RatingSettings ratingSettings = mock(RatingSettings.class);

  private VisitorsCrawler underTest;

  @BeforeEach
  public void setUp() {
    when(ratingSettings.getDebtRatingGrid()).thenReturn(new DebtRatingGrid(RATING_GRID));
    when(ratingSettings.getDevCost()).thenReturn(DEV_COST);
    underTest = new VisitorsCrawler(Arrays.asList(new NewMaintainabilityMeasuresVisitor(metricRepository, measureRepository, newLinesRepository, ratingSettings)));
  }

  private static Stream<Arguments> metrics() {
    return Stream.of(
      arguments(NEW_TECHNICAL_DEBT_KEY, NEW_SQALE_DEBT_RATIO_KEY, NEW_MAINTAINABILITY_RATING_KEY),
      arguments(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY));
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void project_has_new_measures(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 0);
    assertNewRating(ratingKey, ROOT_REF, A);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void project_has_no_measure_if_new_lines_not_available(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(false);
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(debtRatioKey, ROOT_REF);
    assertNoNewMaintainability(ratingKey, ROOT_REF);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void file_has_no_new_debt_ratio_variation_if_new_lines_not_available(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(false);
    setupOneFileAloneInAProject(remediationEffortKey,50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(debtRatioKey, LANGUAGE_1_FILE_REF);
    assertNoNewDebtRatioMeasure(debtRatioKey, ROOT_REF);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void file_has_0_new_debt_ratio_if_no_line_is_new(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    ReportComponent file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(file)
        .build());
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, remediationEffortKey, createNewDebtMeasure(50));
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    setNewLines(file);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 0);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void file_has_new_debt_ratio_if_some_lines_are_new(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    setupOneFileAloneInAProject(remediationEffortKey,50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 83.33);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 83.33);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_changes_with_language_cost(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(ratingSettings.getDevCost()).thenReturn(DEV_COST * 10);
    setupOneFileAloneInAProject(remediationEffortKey,50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 8.33);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 8.33);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_changes_with_new_technical_debt(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    setupOneFileAloneInAProject(remediationEffortKey, 500, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(500));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 833.33);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_on_non_file_level_is_based_on_new_technical_debt_of_that_level(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    setupOneFileAloneInAProject(remediationEffortKey, 500, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(1200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 833.33);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_when_file_is_unit_test(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    setupOneFileAloneInAProject(remediationEffortKey, 500, Flag.UT_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(1200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 833.33);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_is_0_when_file_has_no_new_lines(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    setupOneFileAloneInAProject(remediationEffortKey,50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 0);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_is_0_on_non_file_level_when_no_file_has_new_lines(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    setupOneFileAloneInAProject(remediationEffortKey,50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 0);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_is_0_when_there_is_no_ncloc_in_file(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    setupOneFileAloneInAProject(remediationEffortKey, 50, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 0);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_is_0_on_non_file_level_when_one_file_has_zero_new_debt_because_of_no_changeset(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    setupOneFileAloneInAProject(remediationEffortKey, 50, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 0);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void new_debt_ratio_is_0_when_ncloc_measure_is_missing(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    setupOneFileAloneInAProject(remediationEffortKey, 50, Flag.SRC_FILE, Flag.MISSING_MEASURE_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 0);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void leaf_components_always_have_a_measure_when_at_least_one_period_exist_and_ratio_is_computed_from_current_level_new_debt(String remediationEffortKey, String debtRatioKey,
    String ratingKey) {
    Component file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(file)
            .build())
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(50);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, remediationEffortKey, newDebtMeasure);
    measureRepository.addRawMeasure(111, remediationEffortKey, createNewDebtMeasure(150));
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(250));
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    setNewLines(file, 3, 4);
    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(debtRatioKey, LANGUAGE_1_FILE_REF, 83.33);
    assertNewDebtRatioValues(debtRatioKey, 111, 83.33);
    assertNewDebtRatioValues(debtRatioKey, ROOT_REF, 83.33);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void compute_new_maintainability_rating(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    ReportComponent file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(file)
            .build())
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(50);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, remediationEffortKey, newDebtMeasure);
    measureRepository.addRawMeasure(111, remediationEffortKey, createNewDebtMeasure(150));
    measureRepository.addRawMeasure(ROOT_REF, remediationEffortKey, createNewDebtMeasure(250));
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date

    setNewLines(file, 3, 4);

    underTest.visit(treeRootHolder.getRoot());

    assertNewRating(ratingKey, LANGUAGE_1_FILE_REF, D);
    assertNewRating(ratingKey, 111, D);
    assertNewRating(ratingKey, ROOT_REF, D);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void compute_new_maintainability_rating_map_to_E(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    ReportComponent file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(file)
            .build())
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(400);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, remediationEffortKey, newDebtMeasure);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));

    setNewLines(file, 3, 4);

    underTest.visit(treeRootHolder.getRoot());
    assertNewRating(ratingKey, LANGUAGE_1_FILE_REF, E);
  }

  @Test
  void compute_new_development_cost() {
    ReportComponent file1 = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 4)).build();
    ReportComponent file2 = builder(FILE, 22_222).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 6)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(file1, file2)
            .build())
        .build());

    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    setNewLines(file1, 3, 4);

    // 6 lines file, only last one is not ncloc
    measureRepository.addRawMeasure(22_222, NCLOC_DATA_KEY, createNclocDataMeasure(1, 2, 3, 4, 5));
    // first 2 lines are before all snapshots, 4 last lines are after PERIOD 2's snapshot date
    setNewLines(file2, 3, 4, 5, 6);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDevelopmentCostValues(ROOT_REF, 5 * DEV_COST);
    assertNewDevelopmentCostValues(LANGUAGE_1_FILE_REF, 2 * DEV_COST);
    assertNewDevelopmentCostValues(22_222, 3 * DEV_COST);
  }

  @ParameterizedTest
  @MethodSource("metrics")
  void compute_new_maintainability_rating_to_A_when_no_debt(String remediationEffortKey, String debtRatioKey, String ratingKey) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(
              builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build())
            .build())
        .build());

    underTest.visit(treeRootHolder.getRoot());

    assertNewRating(ratingKey, LANGUAGE_1_FILE_REF, A);
    assertNewRating(ratingKey, 111, A);
    assertNewRating(ratingKey, ROOT_REF, A);
  }

  private void setupOneFileAloneInAProject(String remediationEffortKey, int newDebt, Flag isUnitTest, Flag withNclocLines, Flag withNewLines) {
    checkArgument(isUnitTest == Flag.UT_FILE || isUnitTest == Flag.SRC_FILE);
    checkArgument(withNclocLines == Flag.WITH_NCLOC || withNclocLines == Flag.NO_NCLOC || withNclocLines == Flag.MISSING_MEASURE_NCLOC);
    checkArgument(withNewLines == Flag.WITH_NEW_LINES || withNewLines == Flag.NO_NEW_LINES);

    Component file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(isUnitTest == Flag.UT_FILE, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(file)
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(newDebt);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, remediationEffortKey, newDebtMeasure);
    if (withNclocLines == Flag.WITH_NCLOC) {
      // 4 lines file, only first one is not ncloc
      measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    } else if (withNclocLines == Flag.NO_NCLOC) {
      // 4 lines file, none of which is ncloc
      measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNoNclocDataMeasure(4));
    }
    if (withNewLines == Flag.WITH_NEW_LINES) {
      // 2 last lines are new
      setNewLines(file, 3, 4);
    }
  }

  private void setNewLines(Component component, int... lineNumbers) {
    HashSet<Integer> newLines = new HashSet<>();
    for (int i : lineNumbers) {
      newLines.add(i);
    }
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    when(newLinesRepository.getNewLines(component)).thenReturn(Optional.of(newLines));

  }

  private enum Flag {
    UT_FILE, SRC_FILE, NO_NEW_LINES, WITH_NEW_LINES, WITH_NCLOC, NO_NCLOC, MISSING_MEASURE_NCLOC
  }

  public static ReportComponent.Builder builder(Component.Type type, int ref) {
    return ReportComponent.builder(type, ref).setKey(String.valueOf(ref));
  }

  private Measure createNewDebtMeasure(long value) {
    return newMeasureBuilder().create(value);
  }

  private static Measure createNclocDataMeasure(Integer... nclocLines) {
    Set<Integer> nclocLinesSet = ImmutableSet.copyOf(nclocLines);
    int max = Ordering.<Integer>natural().max(nclocLinesSet);
    ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();
    for (int i = 1; i <= max; i++) {
      builder.put(i, nclocLinesSet.contains(i) ? 1 : 0);
    }
    return newMeasureBuilder().create(KeyValueFormat.format(builder.build(), KeyValueFormat.newIntegerConverter(), KeyValueFormat.newIntegerConverter()));
  }

  private static Measure createNoNclocDataMeasure(int lineCount) {
    ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();
    for (int i = 1; i <= lineCount; i++) {
      builder.put(i, 0);
    }
    return newMeasureBuilder().create(KeyValueFormat.format(builder.build(), KeyValueFormat.newIntegerConverter(), KeyValueFormat.newIntegerConverter()));
  }

  private void assertNoNewDebtRatioMeasure(String debtRatioKey, int componentRef) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, debtRatioKey))
      .isAbsent();
  }

  private void assertNewDebtRatioValues(String debtRatioKey, int componentRef, double expectedValue) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, debtRatioKey)).hasValue(expectedValue, VALUE_COMPARISON_OFFSET);
  }

  private void assertNewDevelopmentCostValues(int componentRef, float expectedValue) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_DEVELOPMENT_COST_KEY)).hasValue(expectedValue, VALUE_COMPARISON_OFFSET);
  }

  private void assertNewRating(String ratingKey, int componentRef, Rating expectedValue) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, ratingKey)).hasValue(expectedValue.getIndex());
  }

  private void assertNoNewMaintainability(String ratingKey, int componentRef) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, ratingKey))
      .isAbsent();
  }
}
