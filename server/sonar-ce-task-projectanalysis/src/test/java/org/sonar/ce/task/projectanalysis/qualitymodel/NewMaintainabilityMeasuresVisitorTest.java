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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

public class NewMaintainabilityMeasuresVisitorTest {

  private static final double[] RATING_GRID = new double[] {0.1, 0.2, 0.5, 1};

  private static final String LANGUAGE_1_KEY = "language 1 key";
  private static final long LANGUAGE_1_DEV_COST = 30l;
  private static final int ROOT_REF = 1;
  private static final int LANGUAGE_1_FILE_REF = 11111;
  private static final Offset<Double> VARIATION_COMPARISON_OFFSET = Offset.offset(0.01);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_TECHNICAL_DEBT)
    .add(NCLOC_DATA)
    .add(NEW_SQALE_DEBT_RATIO)
    .add(NEW_MAINTAINABILITY_RATING)
    .add(NEW_DEVELOPMENT_COST);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);
  private RatingSettings ratingSettings = mock(RatingSettings.class);

  private VisitorsCrawler underTest;

  @Before
  public void setUp() throws Exception {
    when(ratingSettings.getDebtRatingGrid()).thenReturn(new DebtRatingGrid(RATING_GRID));
    underTest = new VisitorsCrawler(Arrays.asList(new NewMaintainabilityMeasuresVisitor(metricRepository, measureRepository, newLinesRepository, ratingSettings)));
  }

  @Test
  public void project_has_new_measures() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(ROOT_REF, 0);
    assertNewMaintainability(ROOT_REF, A);
  }

  @Test
  public void project_has_no_measure_if_new_lines_not_available() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(false);
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(ROOT_REF);
    assertNoNewMaintainability(ROOT_REF);
  }

  @Test
  public void file_has_no_new_debt_ratio_variation_if_new_lines_not_available() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(false);
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(LANGUAGE_1_FILE_REF);
    assertNoNewDebtRatioMeasure(ROOT_REF);
  }

  @Test
  public void file_has_0_new_debt_ratio_if_no_line_is_new() {
    ReportComponent file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(file)
        .build());
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    setNewLines(file);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void file_has_new_debt_ratio_if_some_lines_are_new() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33);
    assertNewDebtRatioValues(ROOT_REF, 83.33);
  }

  @Test
  public void new_debt_ratio_changes_with_language_cost() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST * 10);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 8.33);
    assertNewDebtRatioValues(ROOT_REF, 8.33);
  }

  @Test
  public void new_debt_ratio_changes_with_new_technical_debt() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(500));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(ROOT_REF, 833.33);
  }

  @Test
  public void new_debt_ratio_on_non_file_level_is_based_on_new_technical_debt_of_that_level() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(1200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(ROOT_REF, 833.33);
  }

  @Test
  public void new_debt_ratio_when_file_is_unit_test() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, Flag.UT_FILE, Flag.WITH_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(1200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(ROOT_REF, 833.33);
  }

  @Test
  public void new_debt_ratio_is_0_when_file_has_no_new_lines() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_on_non_file_level_when_no_file_has_new_lines() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_there_is_no_ncloc_in_file() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_on_non_file_level_when_one_file_has_zero_new_debt_because_of_no_changeset() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_ncloc_measure_is_missing() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.MISSING_MEASURE_NCLOC, Flag.WITH_NEW_LINES);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void leaf_components_always_have_a_measure_when_at_least_one_period_exist_and_ratio_is_computed_from_current_level_new_debt() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    Component file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(file)
            .build())
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(50);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    measureRepository.addRawMeasure(111, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(150));
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(250));
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    setNewLines(file, 3, 4);
    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33);
    assertNewDebtRatioValues(111, 83.33);
    assertNewDebtRatioValues(ROOT_REF, 83.33);
  }

  @Test
  public void compute_new_maintainability_rating() {
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    ReportComponent file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(file)
            .build())
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(50);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    measureRepository.addRawMeasure(111, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(150));
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(250));
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date

    setNewLines(file, 3, 4);

    underTest.visit(treeRootHolder.getRoot());

    assertNewMaintainability(LANGUAGE_1_FILE_REF, D);
    assertNewMaintainability(111, D);
    assertNewMaintainability(ROOT_REF, D);
  }

  @Test
  public void compute_new_development_cost() {
    ReportComponent file1 = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 4)).build();
    ReportComponent file2 = builder(FILE, 22_222).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 6)).build();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
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

    assertNewDevelopmentCostValues(ROOT_REF, 5 * LANGUAGE_1_DEV_COST);
    assertNewDevelopmentCostValues(LANGUAGE_1_FILE_REF, 2 * LANGUAGE_1_DEV_COST);
    assertNewDevelopmentCostValues(22_222, 3 * LANGUAGE_1_DEV_COST);
  }

  @Test
  public void compute_new_maintainability_rating_to_A_when_no_debt() {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(
              builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build())
            .build())
        .build());

    underTest.visit(treeRootHolder.getRoot());

    assertNewMaintainability(LANGUAGE_1_FILE_REF, A);
    assertNewMaintainability(111, A);
    assertNewMaintainability(ROOT_REF, A);
  }

  private void setupOneFileAloneInAProject(int newDebt, Flag isUnitTest, Flag withNclocLines, Flag withNewLines) {
    checkArgument(isUnitTest == Flag.UT_FILE || isUnitTest == Flag.SRC_FILE);
    checkArgument(withNclocLines == Flag.WITH_NCLOC || withNclocLines == Flag.NO_NCLOC || withNclocLines == Flag.MISSING_MEASURE_NCLOC);
    checkArgument(withNewLines == Flag.WITH_NEW_LINES || withNewLines == Flag.NO_NEW_LINES);

    Component file = builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(isUnitTest == Flag.UT_FILE, LANGUAGE_1_KEY, 1)).build();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(file)
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(newDebt);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
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

  private Measure createNewDebtMeasure(double variation) {
    return newMeasureBuilder().setVariation(variation).createNoValue();
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

  private void assertNoNewDebtRatioMeasure(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SQALE_DEBT_RATIO_KEY))
      .isAbsent();
  }

  private void assertNewDebtRatioValues(int componentRef, double expectedVariation) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SQALE_DEBT_RATIO_KEY)).hasVariation(expectedVariation, VARIATION_COMPARISON_OFFSET);
  }

  private void assertNewDevelopmentCostValues(int componentRef, long expectedVariation) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_DEVELOPMENT_COST_KEY)).hasVariation(expectedVariation, VARIATION_COMPARISON_OFFSET);
  }

  private void assertNewMaintainability(int componentRef, Rating expectedVariation) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_MAINTAINABILITY_RATING_KEY)).hasVariation(expectedVariation.getIndex());
  }

  private void assertNoNewMaintainability(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_MAINTAINABILITY_RATING_KEY))
      .isAbsent();
  }
}
