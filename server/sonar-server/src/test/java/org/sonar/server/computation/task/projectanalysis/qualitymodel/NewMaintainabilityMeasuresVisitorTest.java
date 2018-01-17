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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.FileAttributes;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepositoryRule;

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
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureAssert.assertThat;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.D;

public class NewMaintainabilityMeasuresVisitorTest {

  private static final double[] RATING_GRID = new double[] {0.1, 0.2, 0.5, 1};

  private static final String LANGUAGE_1_KEY = "language 1 key";
  private static final long LANGUAGE_1_DEV_COST = 30l;
  private static final long PERIOD_SNAPSHOT_DATE = 12323l;
  private static final String SOME_ANALYSIS_UUID = "9993l";
  private static final String SOME_PERIOD_MODE = "some mode";
  private static final int ROOT_REF = 1;
  private static final int LANGUAGE_1_FILE_REF = 11111;
  private static final Offset<Double> VARIATION_COMPARISON_OFFSET = Offset.offset(0.01);

  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();
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
  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  private RatingSettings ratingSettings = mock(RatingSettings.class);

  private VisitorsCrawler underTest;

  @Before
  public void setUp() throws Exception {
    when(ratingSettings.getDebtRatingGrid()).thenReturn(new DebtRatingGrid(RATING_GRID));
    underTest = new VisitorsCrawler(Arrays.asList(new NewMaintainabilityMeasuresVisitor(metricRepository, measureRepository, scmInfoRepository,
      periodsHolder, ratingSettings)));
  }

  @Test
  public void project_has_new_measures_for_each_defined_period() {
    setPeriod();
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(ROOT_REF, 0);
    assertNewMaintainability(ROOT_REF, A);
  }

  @Test
  public void project_has_no_measure_if_there_is_no_period() {
    periodsHolder.setPeriod(null);
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(ROOT_REF);
    assertNoNewMaintainability(ROOT_REF);
  }

  @Test
  public void file_has_no_new_debt_ratio_variation_if_there_is_no_period() {
    periodsHolder.setPeriod(null);
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(LANGUAGE_1_FILE_REF);
    assertNoNewDebtRatioMeasure(ROOT_REF);
  }

  @Test
  public void file_has_0_new_debt_ratio_if_all_scm_dates_are_before_snapshot_dates() {
    setPeriod();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build())
        .build());
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_SNAPSHOT_DATE - 100, 4));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void file_has_new_debt_ratio_if_some_scm_dates_are_after_snapshot_dates() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33);
    assertNewDebtRatioValues(ROOT_REF, 83.33);
  }

  @Test
  public void file_has_new_debt_ratio_if_only_has_some_scm_dates_which_are_after_snapshot_dates() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProjectWithPartialChangesets(50, Flag.SRC_FILE, Flag.WITH_NCLOC);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33);
    assertNewDebtRatioValues(ROOT_REF, 83.33);
  }

  @Test
  public void new_debt_ratio_changes_with_language_cost() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST * 10);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 8.33);
    assertNewDebtRatioValues(ROOT_REF, 8.33);
  }

  @Test
  public void new_debt_ratio_changes_with_new_technical_debt() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(500));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(ROOT_REF, 833.33);
  }

  @Test
  public void new_debt_ratio_on_non_file_level_is_based_on_new_technical_debt_of_that_level() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(1200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(ROOT_REF, 833.33);
  }

  @Test
  public void new_debt_ratio_when_file_is_unit_test() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, Flag.UT_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(1200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33);
    assertNewDebtRatioValues(ROOT_REF, 833.33);
  }

  @Test
  public void new_debt_ratio_is_0_when_file_has_no_changesets() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_on_non_file_level_when_no_file_has_changesets() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_there_is_no_ncloc_in_file() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_on_non_file_level_when_one_file_has_zero_new_debt_because_of_no_changeset() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_ncloc_measure_is_missing() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, Flag.SRC_FILE, Flag.MISSING_MEASURE_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0);
    assertNewDebtRatioValues(ROOT_REF, 0);
  }

  @Test
  public void leaf_components_always_have_a_measure_when_at_least_one_period_exist_and_ratio_is_computed_from_current_level_new_debt() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, 11)
            .addChildren(
              builder(DIRECTORY, 111)
                .addChildren(
                  builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build())
                .build())
            .build())
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(50);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    measureRepository.addRawMeasure(111, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(150));
    measureRepository.addRawMeasure(11, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200));
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(250));
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_SNAPSHOT_DATE - 100, 2, PERIOD_SNAPSHOT_DATE + 100, 2));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33);
    assertNewDebtRatioValues(111, 83.33);
    assertNewDebtRatioValues(11, 83.33);
    assertNewDebtRatioValues(ROOT_REF, 83.33);
  }

  @Test
  public void compute_new_maintainability_rating() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, 11)
            .addChildren(
              builder(DIRECTORY, 111)
                .addChildren(
                  builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build())
                .build())
            .build())
        .build());

    Measure newDebtMeasure = createNewDebtMeasure(50);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    measureRepository.addRawMeasure(111, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(150));
    measureRepository.addRawMeasure(11, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200));
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(250));
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_SNAPSHOT_DATE - 100, 2, PERIOD_SNAPSHOT_DATE + 100, 2));

    underTest.visit(treeRootHolder.getRoot());

    assertNewMaintainability(LANGUAGE_1_FILE_REF, D);
    assertNewMaintainability(111, D);
    assertNewMaintainability(11, D);
    assertNewMaintainability(ROOT_REF, D);
  }

  @Test
  public void compute_new_development_cost() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, 11)
            .addChildren(
              builder(DIRECTORY, 111)
                .addChildren(
                  builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 4)).build(),
                  builder(FILE, 22_222).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 6)).build())
                .build())
            .build())
        .build());

    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_SNAPSHOT_DATE - 100, 2, PERIOD_SNAPSHOT_DATE + 100, 2));
    // 6 lines file, only last one is not ncloc
    measureRepository.addRawMeasure(22_222, NCLOC_DATA_KEY, createNclocDataMeasure(1, 2, 3, 4, 5));
    // first 2 lines are before all snapshots, 4 last lines are after PERIOD 2's snapshot date
    scmInfoRepository.setScmInfo(22_222, createChangesets(PERIOD_SNAPSHOT_DATE - 100, 2, PERIOD_SNAPSHOT_DATE + 100, 4));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDevelopmentCostValues(ROOT_REF, 5 * LANGUAGE_1_DEV_COST);
    assertNewDevelopmentCostValues(LANGUAGE_1_FILE_REF, 2 * LANGUAGE_1_DEV_COST);
    assertNewDevelopmentCostValues(22_222, 3 * LANGUAGE_1_DEV_COST);
  }

  @Test
  public void compute_new_maintainability_rating_to_A_when_no_debt() {
    setPeriod();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, 11)
            .addChildren(
              builder(DIRECTORY, 111)
                .addChildren(
                  builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY, 1)).build())
                .build())
            .build())
        .build());

    underTest.visit(treeRootHolder.getRoot());

    assertNewMaintainability(LANGUAGE_1_FILE_REF, A);
    assertNewMaintainability(111, A);
    assertNewMaintainability(11, A);
    assertNewMaintainability(ROOT_REF, A);
  }

  private void setupOneFileAloneInAProject(int newDebt, Flag isUnitTest, Flag withNclocLines, Flag withChangeSets) {
    checkArgument(isUnitTest == Flag.UT_FILE || isUnitTest == Flag.SRC_FILE);
    checkArgument(withNclocLines == Flag.WITH_NCLOC || withNclocLines == Flag.NO_NCLOC || withNclocLines == Flag.MISSING_MEASURE_NCLOC);
    checkArgument(withChangeSets == Flag.WITH_CHANGESET || withChangeSets == Flag.NO_CHANGESET);

    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(isUnitTest == Flag.UT_FILE, LANGUAGE_1_KEY, 1)).build())
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
    if (withChangeSets == Flag.WITH_CHANGESET) {
      // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
      scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_SNAPSHOT_DATE - 100, 2, PERIOD_SNAPSHOT_DATE + 100, 2));
    }
  }

  private void setupOneFileAloneInAProjectWithPartialChangesets(int newDebt, Flag isUnitTest, Flag withNclocLines) {
    checkArgument(isUnitTest == Flag.UT_FILE || isUnitTest == Flag.SRC_FILE);
    checkArgument(withNclocLines == Flag.WITH_NCLOC || withNclocLines == Flag.NO_NCLOC || withNclocLines == Flag.MISSING_MEASURE_NCLOC);

    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(isUnitTest == Flag.UT_FILE, LANGUAGE_1_KEY, 1)).build())
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
    // 2 last lines are after PERIOD 2's snapshot date
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesetsForLines(PERIOD_SNAPSHOT_DATE + 100, 3, 4));
  }

  private enum Flag {
    UT_FILE, SRC_FILE, NO_CHANGESET, WITH_CHANGESET, WITH_NCLOC, NO_NCLOC, MISSING_MEASURE_NCLOC
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

  /**
   * Creates changesets of {@code lines} lines which all have the same date {@code scmDate}.
   */
  private static Changeset[] createChangesets(long scmDate, int lines) {
    Changeset changetset = Changeset.newChangesetBuilder().setDate(scmDate).setRevision("rev-1").build();
    Changeset[] changesets = new Changeset[lines];
    for (int i = 0; i < lines; i++) {
      changesets[i] = changetset;
    }
    return changesets;
  }

  /**
   * Creates changesets for specific lines, which all have the same date {@code scmDate}.
   */
  private static Map<Integer, Changeset> createChangesetsForLines(long scmDate, int... lines) {
    Changeset changetset = Changeset.newChangesetBuilder().setDate(scmDate).setRevision("rev-1").build();
    return Arrays.stream(lines).boxed().collect(Collectors.toMap(l -> l, l -> changetset));
  }

  /**
   * Creates a changeset of {@code lineCount} lines which have the date {@code scmDate} and {@code otherLineCount} lines which
   * have the date {@code otherScmDate}.
   */
  private static Changeset[] createChangesets(long scmDate, int lineCount, long otherScmDate, int otherLineCount) {
    Changeset[] changesets = new Changeset[lineCount + otherLineCount];
    Changeset changetset1 = Changeset.newChangesetBuilder().setDate(scmDate).setRevision("rev-1").build();
    for (int i = 0; i < lineCount; i++) {
      changesets[i] = changetset1;
    }
    Changeset changetset2 = Changeset.newChangesetBuilder().setDate(otherScmDate).setRevision("rev-2").build();
    for (int i = lineCount; i < lineCount + otherLineCount; i++) {
      changesets[i] = changetset2;
    }
    return changesets;
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

  private void setPeriod() {
    periodsHolder.setPeriod(new Period(SOME_PERIOD_MODE, null, PERIOD_SNAPSHOT_DATE, SOME_ANALYSIS_UUID));
  }
}
