/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.component.FileAttributes;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolderRule;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepositoryRule;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
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

public class NewQualityModelMeasuresVisitorTest {
  private static final String LANGUAGE_1_KEY = "language 1 key";
  private static final long LANGUAGE_1_DEV_COST = 30l;
  private static final long PERIOD_2_SNAPSHOT_DATE = 12323l;
  private static final long PERIOD_5_SNAPSHOT_DATE = 99999999l;
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
    .add(NEW_SQALE_DEBT_RATIO);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  private RatingSettings ratingSettings = mock(RatingSettings.class);

  private VisitorsCrawler underTest = new VisitorsCrawler(Arrays.<ComponentVisitor>asList(new NewQualityModelMeasuresVisitor(metricRepository, measureRepository, scmInfoRepository,
    periodsHolder, ratingSettings)));

  @Test
  public void project_has_new_debt_ratio_variation_for_each_defined_period() {
    setTwoPeriods();
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void project_has_no_new_debt_ratio_variation_if_there_is_no_period() {
    periodsHolder.setPeriods();
    treeRootHolder.setRoot(builder(PROJECT, ROOT_REF).build());

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(ROOT_REF);
  }

  @Test
  public void file_has_no_new_debt_ratio_variation_if_there_is_no_period() {
    periodsHolder.setPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50, 12));

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(LANGUAGE_1_FILE_REF);
    assertNoNewDebtRatioMeasure(ROOT_REF);
  }

  @Test
  public void file_has_0_new_debt_ratio_if_all_scm_dates_are_before_snapshot_dates() {
    setTwoPeriods();
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY)).build()
        )
        .build()
    );
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50, 12));
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_2_SNAPSHOT_DATE - 100, 4));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void file_has_new_debt_ratio_if_some_scm_dates_are_after_snapshot_dates() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50, 12));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 83.33, 0);
  }

  @Test
  public void new_debt_ratio_changes_with_language_cost() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST * 10);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50, 12));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 8.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 8.33, 0);
  }

  @Test
  public void new_debt_ratio_changes_with_new_technical_debt() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, 120, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(500, 120));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 833.33, 0);
  }

  @Test
  public void new_debt_ratio_on_non_file_level_is_based_on_new_technical_debt_of_that_level() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, 120, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(1200, 820));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 833.33, 0);
  }

  @Test
  public void new_debt_ratio_when_file_is_unit_test() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, 120, Flag.UT_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(1200, 820));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 833.33, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_file_has_no_changesets() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50, 12));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void new_debt_ratio_is_0_on_non_file_level_when_no_file_has_changesets() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200, 162));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_there_is_no_ncloc_in_file() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50, 12));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void new_debt_ratio_is_0_on_non_file_level_when_one_file_has_zero_new_debt_because_of_no_changeset() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200, 162));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_ncloc_measure_is_missing() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.MISSING_MEASURE_NCLOC, Flag.WITH_CHANGESET);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(50, 12));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void leaf_components_always_have_a_measure_when_at_least_one_period_exist_and_ratio_is_computed_from_current_level_new_debt() {
    setTwoPeriods();
    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, 11)
            .addChildren(
              builder(DIRECTORY, 111)
                .addChildren(
                  builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY)).build()
                ).build()
            ).build()
        ).build()
    );

    Measure newDebtMeasure = createNewDebtMeasure(50, 12);
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    measureRepository.addRawMeasure(111, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(150, 112));
    measureRepository.addRawMeasure(11, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(200, 112));
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, createNewDebtMeasure(250, 212));
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_2_SNAPSHOT_DATE - 100, 2, PERIOD_2_SNAPSHOT_DATE + 100, 2));

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33, 0);
    assertNewDebtRatioValues(111, 83.33, 0);
    assertNewDebtRatioValues(11, 83.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 83.33, 0);
  }

  @Test
  public void new_debt_ratio_is_computed_for_five_periods() throws Exception {
    long period1 = 10000L;
    long period2 = 20000L;
    long period3 = 30000L;
    long period4 = 40000L;
    long period5 = 50000L;

    periodsHolder.setPeriods(
      new Period(1, SOME_PERIOD_MODE, null, period1, SOME_ANALYSIS_UUID),
      new Period(2, SOME_PERIOD_MODE, null, period2, SOME_ANALYSIS_UUID),
      new Period(3, SOME_PERIOD_MODE, null, period3, SOME_ANALYSIS_UUID),
      new Period(4, SOME_PERIOD_MODE, null, period4, SOME_ANALYSIS_UUID),
      new Period(5, SOME_PERIOD_MODE, null, period5, SOME_ANALYSIS_UUID));

    when(ratingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);

    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_1_KEY)).build())
        .build()
    );

    Measure newDebtMeasure = newMeasureBuilder().setVariations(new MeasureVariations(500d, 500d, 500d, 120d, 120d)).createNoValue();
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    // 4 lines file, only first one is not ncloc
    measureRepository.addRawMeasure(LANGUAGE_1_FILE_REF, NCLOC_DATA_KEY, createNclocDataMeasure(2, 3, 4));
    // first 2 lines are before all snapshots, 2 last lines are after PERIOD 2's snapshot date
    scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(period2 - 100, 2, period2 + 100, 2));

    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY,
      newMeasureBuilder().setVariations(new MeasureVariations(1200d, 1200d, 1200d, 820d, 820d)).createNoValue());

    underTest.visit(treeRootHolder.getRoot());

    assertThat(measureRepository.getAddedRawMeasure(LANGUAGE_1_FILE_REF, NEW_SQALE_DEBT_RATIO_KEY))
      .hasVariation1(833.333, VARIATION_COMPARISON_OFFSET)
      .hasVariation2(833.333, VARIATION_COMPARISON_OFFSET)
      .hasVariation3(0d, VARIATION_COMPARISON_OFFSET)
      .hasVariation4(0d, VARIATION_COMPARISON_OFFSET)
      .hasVariation5(0d, VARIATION_COMPARISON_OFFSET);
  }

  private void setupOneFileAloneInAProject(int newDebtPeriod2, int newDebtPeriod4, Flag isUnitTest, Flag withNclocLines, Flag withChangeSets) {
    checkArgument(isUnitTest == Flag.UT_FILE || isUnitTest == Flag.SRC_FILE);
    checkArgument(withNclocLines == Flag.WITH_NCLOC || withNclocLines == Flag.NO_NCLOC || withNclocLines == Flag.MISSING_MEASURE_NCLOC);
    checkArgument(withChangeSets == Flag.WITH_CHANGESET || withChangeSets == Flag.NO_CHANGESET);

    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(FILE, LANGUAGE_1_FILE_REF).setFileAttributes(new FileAttributes(isUnitTest == Flag.UT_FILE, LANGUAGE_1_KEY)).build()
        )
        .build()
    );

    Measure newDebtMeasure = createNewDebtMeasure(newDebtPeriod2, newDebtPeriod4);
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
      scmInfoRepository.setScmInfo(LANGUAGE_1_FILE_REF, createChangesets(PERIOD_2_SNAPSHOT_DATE - 100, 2, PERIOD_2_SNAPSHOT_DATE + 100, 2));
    }
  }

  private enum Flag {
    UT_FILE, SRC_FILE, NO_CHANGESET, WITH_CHANGESET, WITH_NCLOC, NO_NCLOC, MISSING_MEASURE_NCLOC
  }

  public static ReportComponent.Builder builder(Component.Type type, int ref) {
    return ReportComponent.builder(type, ref).setKey(String.valueOf(ref));
  }

  private Measure createNewDebtMeasure(double period2Value, double period4Value) {
    return newMeasureBuilder().setVariations(new MeasureVariations(null, period2Value, null, period4Value, null)).createNoValue();
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

  private void assertNewDebtRatioValues(int componentRef, double expectedPeriod2Value, double expectedPeriod4Value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, NEW_SQALE_DEBT_RATIO_KEY))
      .hasVariation2(expectedPeriod2Value, VARIATION_COMPARISON_OFFSET)
      .hasVariation4(expectedPeriod4Value, VARIATION_COMPARISON_OFFSET);
  }

  private void setTwoPeriods() {
    periodsHolder.setPeriods(
      new Period(2, SOME_PERIOD_MODE, null, PERIOD_2_SNAPSHOT_DATE, SOME_ANALYSIS_UUID),
      new Period(4, SOME_PERIOD_MODE, null, PERIOD_5_SNAPSHOT_DATE, SOME_ANALYSIS_UUID));
  }
}
