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
package org.sonar.server.computation.sqale;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.VisitorsCrawler;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;
import org.sonar.server.computation.scm.Changeset;
import org.sonar.server.computation.scm.ScmInfoRepositoryRule;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureAssert.assertThat;

public class SqaleNewMeasuresVisitorTest {
  private static final String LANGUAGE_1_KEY = "language 1 key";
  private static final long LANGUAGE_1_DEV_COST = 30l;
  private static final long PERIOD_2_SNAPSHOT_DATE = 12323l;
  private static final long PERIOD_5_SNAPSHOT_DATE = 99999999l;
  private static final long SOME_SNAPSHOT_ID = 9993l;
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
    .add(CoreMetrics.NEW_TECHNICAL_DEBT)
    .add(CoreMetrics.NCLOC_DATA)
    .add(CoreMetrics.NEW_SQALE_DEBT_RATIO);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  private SqaleRatingSettings sqaleRatingSettings = mock(SqaleRatingSettings.class);

  private VisitorsCrawler underTest = new VisitorsCrawler(Arrays.<ComponentVisitor>asList(new SqaleNewMeasuresVisitor(metricRepository, measureRepository, scmInfoRepository,
    periodsHolder, sqaleRatingSettings)));

  @Before
  public void setUp() throws Exception {
    periodsHolder.setPeriods(
      new Period(2, SOME_PERIOD_MODE, null, PERIOD_2_SNAPSHOT_DATE, SOME_SNAPSHOT_ID),
      new Period(4, SOME_PERIOD_MODE, null, PERIOD_5_SNAPSHOT_DATE, SOME_SNAPSHOT_ID));
  }

  @Test
  public void project_has_new_debt_ratio_variation_for_each_defined_period() {
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
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(LANGUAGE_1_FILE_REF);
    assertNoNewDebtRatioMeasure(ROOT_REF);
  }

  @Test
  public void file_has_0_new_debt_ratio_if_all_scm_dates_are_before_snapshot_dates() {
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
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 83.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 83.33, 0);
  }

  @Test
  public void new_debt_ratio_changes_with_language_cost() {
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST * 10);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 8.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 8.33, 0);
  }

  @Test
  public void new_debt_ratio_changes_with_new_technical_debt() {
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(500, 120, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 833.33, 0);
    assertNewDebtRatioValues(ROOT_REF, 833.33, 0);
  }

  @Test
  public void no_new_debt_ratio_when_file_is_unit_test() {
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.UT_FILE, Flag.WITH_NCLOC, Flag.WITH_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNoNewDebtRatioMeasure(LANGUAGE_1_FILE_REF);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_file_has_no_changesets() {
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.WITH_NCLOC, Flag.NO_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_there_is_no_ncloc_in_file() {
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.NO_NCLOC, Flag.WITH_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void new_debt_ratio_is_0_when_ncloc_measure_is_missing() {
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
    setupOneFileAloneInAProject(50, 12, Flag.SRC_FILE, Flag.MISSING_MEASURE_NCLOC, Flag.WITH_CHANGESET);

    underTest.visit(treeRootHolder.getRoot());

    assertNewDebtRatioValues(LANGUAGE_1_FILE_REF, 0, 0);
    assertNewDebtRatioValues(ROOT_REF, 0, 0);
  }

  @Test
  public void no_leaf_components_always_have_a_measure_when_at_least_one_period_exist() {
    when(sqaleRatingSettings.getDevCost(LANGUAGE_1_KEY)).thenReturn(LANGUAGE_1_DEV_COST);
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
    measureRepository.addRawMeasure(111, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    measureRepository.addRawMeasure(11, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
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
    measureRepository.addRawMeasure(ROOT_REF, NEW_TECHNICAL_DEBT_KEY, newDebtMeasure);
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
    assertThat(measureRepository.getAddedRawMeasure(componentRef, CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY))
      .isAbsent();
  }

  private void assertNewDebtRatioValues(int componentRef, double expectedPeriod2Value, double expectedPeriod4Value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY))
      .hasVariation2(expectedPeriod2Value, VARIATION_COMPARISON_OFFSET)
      .hasVariation4(expectedPeriod4Value, VARIATION_COMPARISON_OFFSET);
  }
}
