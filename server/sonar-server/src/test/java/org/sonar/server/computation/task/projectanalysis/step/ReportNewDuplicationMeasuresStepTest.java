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

package org.sonar.server.computation.task.projectanalysis.step;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.duplication.TextBlock;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolderRule;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepositoryRule;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_DUPLICATED;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_DUPLICATED_KEY;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations.newMeasureVariationsBuilder;

public class ReportNewDuplicationMeasuresStepTest {
  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_1_REF = 123;
  private static final int SUB_MODULE_2_REF = 126;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12342;
  private static final int FILE_3_REF = 1261;
  private static final int FILE_4_REF = 1262;
  private static final String SOME_FILE_KEY = "some file key";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(MODULE, SUB_MODULE_1_REF)
                .addChildren(
                  builder(DIRECTORY, DIRECTORY_REF)
                    .addChildren(
                      builder(FILE, FILE_1_REF).build(),
                      builder(FILE, FILE_2_REF).build())
                    .build())
                .build(),
              builder(MODULE, SUB_MODULE_2_REF)
                .addChildren(
                  builder(FILE, FILE_3_REF).build(),
                  builder(FILE, FILE_4_REF).build())
                .build())
            .build())
        .build());
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();
  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_LINES_DUPLICATED);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);

  @Before
  public void setUp() {
    periodsHolder.setPeriods(
      new Period(2, "mode_p_1", null, parseDate("2009-12-25").getTime(), "u1"),
      new Period(5, "mode_p_5", null, parseDate("2011-02-18").getTime(), "u2"));
  }

  NewDuplicationMeasuresStep underTest = new NewDuplicationMeasuresStep(treeRootHolder, periodsHolder, metricRepository, measureRepository, scmInfoRepository,
    duplicationRepository);

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate_of_a_single_line() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(2, 2));
    setChangesets(FILE_1_REF);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, 2d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_ignores_InProjectDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, FILE_2_REF, new TextBlock(2, 2));
    setChangesets(FILE_1_REF);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, 1d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_ignores_CrossProjectDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, SOME_FILE_KEY, new TextBlock(2, 2));
    setChangesets(FILE_1_REF);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, 1d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate() {
    TextBlock original = new TextBlock(1, 5);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(10, 11));
    setChangesets(FILE_1_REF);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, 6d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate_only_once() {
    TextBlock original = new TextBlock(1, 10);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(10, 11), new TextBlock(11, 12));
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(2, 2), new TextBlock(4, 4));
    setChangesets(FILE_1_REF);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, 11d);
  }

  @Test
  public void compute_new_duplicated_lines_on_different_periods() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(2, 2));
    scmInfoRepository.setScmInfo(FILE_1_REF,
      Changeset.newChangesetBuilder().setDate(parseDate("2012-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-2").build());

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, 2d, 1d);
  }

  @Test
  public void compute_and_aggregate_duplicated_lines() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_3_REF, 10);
    addDuplicatedBlock(FILE_4_REF, 12);
    setChangesets(FILE_1_REF);
    setChangesets(FILE_2_REF);
    setChangesets(FILE_3_REF);
    setChangesets(FILE_4_REF);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, 2d);
    assertRawMeasureValue(FILE_2_REF, 0d);
    assertRawMeasureValue(FILE_3_REF, 9d);
    assertRawMeasureValue(FILE_4_REF, 11d);
    assertRawMeasureValue(DIRECTORY_REF, 2d);
    assertRawMeasureValue(SUB_MODULE_1_REF, 2d);
    assertRawMeasureValue(SUB_MODULE_2_REF, 20d);
    assertRawMeasureValue(MODULE_REF, 22d);
    assertRawMeasureValue(ROOT_REF, 22d);
  }

  @Test
  public void compute_and_aggregate_zero_duplicated_line_when_no_duplication() {
    setChangesets(FILE_1_REF);
    setChangesets(FILE_2_REF);
    setChangesets(FILE_3_REF);
    setChangesets(FILE_4_REF);

    underTest.execute();

    assertComputedAndAggregatedToZeroInt();
  }

  /**
   * Adds duplication blocks of a single line (each line is specific to its block).
   *
   * This is a very simple use case, convenient for unit tests but more realistic and complex use cases must be tested separately.
   */
  private void addDuplicatedBlock(int fileRef, int blockCount) {
    checkArgument(blockCount > 1, "BlockCount can not be less than 2");
    TextBlock original = new TextBlock(1, 1);
    TextBlock[] duplicates = new TextBlock[blockCount - 1];
    for (int i = 2; i < blockCount + 1; i++) {
      duplicates[i - 2] = new TextBlock(i, i);
    }
    duplicationRepository.addDuplication(fileRef, original, duplicates);
  }

  private void setChangesets(int componentRef) {
    scmInfoRepository.setScmInfo(componentRef,
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      // line 3 is older, part of no period
      Changeset.newChangesetBuilder().setDate(parseDate("2007-01-15").getTime()).setRevision("rev-2").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
      Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build());
  }

  private void assertRawMeasureValue(int componentRef, double period2Value) {
    assertRawMeasureValue(componentRef, period2Value, 0d);
  }

  private void assertRawMeasureValue(int componentRef, double period2Value, double period5Value) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnlyOnce(
      entryOf(NEW_LINES_DUPLICATED_KEY, createMeasure(period2Value, period5Value)));
  }

  private void assertComputedAndAggregatedToZeroInt() {
    assertRawMeasureValue(FILE_1_REF, 0);
    assertRawMeasureValue(FILE_2_REF, 0);
    assertRawMeasureValue(FILE_3_REF, 0);
    assertRawMeasureValue(FILE_4_REF, 0);
    assertRawMeasureValue(DIRECTORY_REF, 0);
    assertRawMeasureValue(SUB_MODULE_1_REF, 0);
    assertRawMeasureValue(SUB_MODULE_2_REF, 0);
    assertRawMeasureValue(MODULE_REF, 0);
    assertRawMeasureValue(ROOT_REF, 0);
  }

  private static Measure createMeasure(@Nullable Double variationPeriod2, @Nullable Double variationPeriod5) {
    MeasureVariations.Builder variationBuilder = newMeasureVariationsBuilder();
    if (variationPeriod2 != null) {
      variationBuilder.setVariation(new Period(2, "", null, 1L, "u2"), variationPeriod2);
    }
    if (variationPeriod5 != null) {
      variationBuilder.setVariation(new Period(5, "", null, 1L, "u2"), variationPeriod5);
    }
    return newMeasureBuilder()
      .setVariations(variationBuilder.build())
      .createNoValue();
  }

}
