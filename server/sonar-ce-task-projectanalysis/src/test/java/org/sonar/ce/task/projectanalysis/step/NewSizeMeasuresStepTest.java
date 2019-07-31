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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepositoryRule;
import org.sonar.ce.task.projectanalysis.duplication.TextBlock;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.ce.task.step.TestComputationStepContext;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKS_DUPLICATED;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class NewSizeMeasuresStepTest {

  private static final Offset<Double> DEFAULT_OFFSET = Offset.offset(0.1d);

  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_REF = 1234;
  private static final int DIRECTORY_2_REF = 1235;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12342;
  private static final int FILE_3_REF = 1261;
  private static final int FILE_4_REF = 1262;
  private static final Component FILE_1 = builder(FILE, FILE_1_REF).build();
  private static final Component FILE_2 = builder(FILE, FILE_2_REF).build();
  private static final Component FILE_3 = builder(FILE, FILE_3_REF).build();
  private static final Component FILE_4 = builder(FILE, FILE_4_REF).build();

  private static final String SOME_FILE_KEY = "some file key";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF)
            .addChildren(FILE_1, FILE_2)
            .build(),
          builder(DIRECTORY, DIRECTORY_2_REF).build(),
          FILE_3, FILE_4)
        .build());

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_LINES)
    .add(NEW_DUPLICATED_LINES)
    .add(NEW_DUPLICATED_LINES_DENSITY)
    .add(NEW_BLOCKS_DUPLICATED);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);
  private NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);

  private NewSizeMeasuresStep underTest = new NewSizeMeasuresStep(treeRootHolder, metricRepository, measureRepository, newLinesRepository, duplicationRepository);

  @Test
  public void compute_new_lines() {
    setNewLines(FILE_1, FILE_2, FILE_4);
    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_LINES_KEY, 11);
    assertRawMeasureValueOnPeriod(FILE_2_REF, NEW_LINES_KEY, 11);
    assertNoRawMeasure(FILE_3_REF, NEW_LINES_KEY);
    assertRawMeasureValueOnPeriod(FILE_4_REF, NEW_LINES_KEY, 11);
    assertRawMeasureValueOnPeriod(DIRECTORY_REF, NEW_LINES_KEY, 22);
    assertNoRawMeasure(DIRECTORY_2_REF, NEW_LINES_KEY);
    assertRawMeasureValueOnPeriod(ROOT_REF, NEW_LINES_KEY, 33);
  }

  @Test
  public void compute_new_lines_with_only_some_lines_having_changesets() {
    setFirstTwoLinesAsNew(FILE_1, FILE_2, FILE_4);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_LINES_KEY, 2);
    assertRawMeasureValueOnPeriod(FILE_2_REF, NEW_LINES_KEY, 2);
    assertNoRawMeasure(FILE_3_REF, NEW_LINES_KEY);
    assertRawMeasureValueOnPeriod(FILE_4_REF, NEW_LINES_KEY, 2);
    assertRawMeasureValueOnPeriod(DIRECTORY_REF, NEW_LINES_KEY, 4);
    assertNoRawMeasure(DIRECTORY_2_REF, NEW_LINES_KEY);
    assertRawMeasureValueOnPeriod(ROOT_REF, NEW_LINES_KEY, 6);
  }

  @Test
  public void does_not_compute_new_lines_when_no_changeset() {
    underTest.execute(new TestComputationStepContext());

    assertNoRawMeasures(NEW_LINES_KEY);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate_of_a_single_line() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 1), new TextBlock(2, 2));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_DUPLICATED_LINES_KEY, 2d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_ignores_InProjectDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, FILE_2_REF, new TextBlock(2, 2));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_DUPLICATED_LINES_KEY, 1d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_ignores_CrossProjectDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addCrossProjectDuplication(FILE_1_REF, original, SOME_FILE_KEY, new TextBlock(2, 2));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_DUPLICATED_LINES_KEY, 1d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate() {
    TextBlock original = new TextBlock(1, 5);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(10, 11));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_DUPLICATED_LINES_KEY, 6d);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate_only_once() {
    TextBlock original = new TextBlock(1, 10);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(10, 11), new TextBlock(11, 12));
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(2, 2), new TextBlock(4, 4));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_DUPLICATED_LINES_KEY, 11d);
  }

  @Test
  public void compute_and_aggregate_duplicated_lines() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_3_REF, 10);
    addDuplicatedBlock(FILE_4_REF, 12);
    setNewLines(FILE_1, FILE_2, FILE_3, FILE_4);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_DUPLICATED_LINES_KEY, 2d);
    assertRawMeasureValueOnPeriod(FILE_2_REF, NEW_DUPLICATED_LINES_KEY, 0d);
    assertRawMeasureValueOnPeriod(FILE_3_REF, NEW_DUPLICATED_LINES_KEY, 9d);
    assertRawMeasureValueOnPeriod(FILE_4_REF, NEW_DUPLICATED_LINES_KEY, 11d);
    assertRawMeasureValueOnPeriod(DIRECTORY_REF, NEW_DUPLICATED_LINES_KEY, 2d);
    assertNoRawMeasure(DIRECTORY_2_REF, NEW_DUPLICATED_LINES_KEY);
    assertRawMeasureValueOnPeriod(ROOT_REF, NEW_DUPLICATED_LINES_KEY, 22d);
  }

  @Test
  public void compute_and_aggregate_duplicated_lines_when_only_some_lines_have_changesets() {
    // 2 new duplicated lines in each, since only the first 2 lines are new
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_3_REF, 10);
    addDuplicatedBlock(FILE_4_REF, 12);
    setFirstTwoLinesAsNew(FILE_1, FILE_2, FILE_3, FILE_4);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_DUPLICATED_LINES_KEY, 2d);
    assertRawMeasureValueOnPeriod(FILE_2_REF, NEW_DUPLICATED_LINES_KEY, 0d);
    assertRawMeasureValueOnPeriod(FILE_3_REF, NEW_DUPLICATED_LINES_KEY, 2d);
    assertRawMeasureValueOnPeriod(FILE_4_REF, NEW_DUPLICATED_LINES_KEY, 2d);
    assertRawMeasureValueOnPeriod(DIRECTORY_REF, NEW_DUPLICATED_LINES_KEY, 2d);
    assertNoRawMeasure(DIRECTORY_2_REF, NEW_DUPLICATED_LINES_KEY);
    assertRawMeasureValueOnPeriod(ROOT_REF, NEW_DUPLICATED_LINES_KEY, 6d);
  }

  @Test
  public void compute_and_aggregate_zero_duplicated_line_when_no_duplication() {
    setNewLines(FILE_1, FILE_2, FILE_3, FILE_4);

    underTest.execute(new TestComputationStepContext());

    assertComputedAndAggregatedToZeroInt(NEW_DUPLICATED_LINES_KEY);
  }

  @Test
  public void compute_duplicated_blocks_one_for_original_one_for_each_InnerDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(2, 2), new TextBlock(4, 4), new TextBlock(3, 4));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_BLOCKS_DUPLICATED_KEY, 4);
  }

  @Test
  public void compute_duplicated_blocks_does_not_count_blocks_only_once_it_assumes_consistency_from_duplication_data() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 1), new TextBlock(4, 4));
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(2, 2), new TextBlock(4, 4));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_BLOCKS_DUPLICATED_KEY, 4);
  }

  @Test
  public void compute_duplicated_blocks_one_for_original_and_ignores_InProjectDuplicate() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 1), FILE_2_REF, new TextBlock(2, 2));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_BLOCKS_DUPLICATED_KEY, 1);
  }

  @Test
  public void compute_duplicated_blocks_one_for_original_and_ignores_CrossProjectDuplicate() {
    duplicationRepository.addCrossProjectDuplication(FILE_1_REF, new TextBlock(1, 1), SOME_FILE_KEY, new TextBlock(2, 2));
    setNewLines(FILE_1);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_BLOCKS_DUPLICATED_KEY, 1);
  }

  @Test
  public void compute_and_aggregate_duplicated_blocks_from_single_duplication() {
    addDuplicatedBlock(FILE_1_REF, 11);
    addDuplicatedBlock(FILE_2_REF, 2);
    addDuplicatedBlock(FILE_4_REF, 7);
    setNewLines(FILE_1, FILE_2, FILE_3, FILE_4);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValueOnPeriod(FILE_1_REF, NEW_BLOCKS_DUPLICATED_KEY, 10);
    assertRawMeasureValueOnPeriod(FILE_2_REF, NEW_BLOCKS_DUPLICATED_KEY, 2);
    assertRawMeasureValueOnPeriod(FILE_3_REF, NEW_BLOCKS_DUPLICATED_KEY, 0);
    assertRawMeasureValueOnPeriod(FILE_4_REF, NEW_BLOCKS_DUPLICATED_KEY, 6);
    assertRawMeasureValueOnPeriod(DIRECTORY_REF, NEW_BLOCKS_DUPLICATED_KEY, 12);
    assertRawMeasureValueOnPeriod(ROOT_REF, NEW_BLOCKS_DUPLICATED_KEY, 18);
  }

  @Test
  public void compute_and_aggregate_duplicated_blocks_to_zero_when_no_duplication() {
    setNewLines(FILE_1, FILE_2, FILE_3, FILE_4);

    underTest.execute(new TestComputationStepContext());

    assertComputedAndAggregatedToZeroInt(NEW_BLOCKS_DUPLICATED_KEY);
  }

  @Test
  public void compute_new_duplicated_lines_density() {
    setNewLines(FILE_1, FILE_2, FILE_4);
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_3_REF, 10);
    addDuplicatedBlock(FILE_4_REF, 12);

    underTest.execute(new TestComputationStepContext());

    assertRawMeasureValue(FILE_1_REF, NEW_DUPLICATED_LINES_DENSITY_KEY, 18.2d);
    assertRawMeasureValue(FILE_2_REF, NEW_DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertNoRawMeasure(FILE_3_REF, NEW_DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(FILE_4_REF, NEW_DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertRawMeasureValue(DIRECTORY_REF, NEW_DUPLICATED_LINES_DENSITY_KEY, 9.1d);
    assertNoRawMeasure(DIRECTORY_2_REF, NEW_DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(ROOT_REF, NEW_DUPLICATED_LINES_DENSITY_KEY, 39.4d);
  }

  @Test
  public void compute_no_new_duplicated_lines_density_when_no_lines() {
    underTest.execute(new TestComputationStepContext());

    assertNoRawMeasures(NEW_DUPLICATED_LINES_DENSITY_KEY);
  }

  /**
   * Adds duplication blocks of a single line (each line is specific to its block).
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

  private void setFirstTwoLinesAsNew(Component... components) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    Set<Integer> newLines = new HashSet<>(Arrays.asList(1, 2));
    for (Component c : components) {
      when(newLinesRepository.getNewLines(c)).thenReturn(Optional.of(newLines));
    }
  }

  private void setNewLines(Component... components) {
    when(newLinesRepository.newLinesAvailable()).thenReturn(true);
    Set<Integer> newLines = new HashSet<>(Arrays.asList(1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12));
    for (Component c : components) {
      when(newLinesRepository.getNewLines(c)).thenReturn(Optional.of(newLines));
    }
  }

  private void assertRawMeasureValueOnPeriod(int componentRef, String metricKey, double expectedVariation) {
    assertRawMeasureValue(componentRef, metricKey, expectedVariation);
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, double expectedVariation) {
    double variation = measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getVariation();
    assertThat(variation).isEqualTo(expectedVariation, DEFAULT_OFFSET);
  }

  private void assertComputedAndAggregatedToZeroInt(String metricKey) {
    assertRawMeasureValueOnPeriod(FILE_1_REF, metricKey, 0);
    assertRawMeasureValueOnPeriod(FILE_2_REF, metricKey, 0);
    assertRawMeasureValueOnPeriod(FILE_3_REF, metricKey, 0);
    assertRawMeasureValueOnPeriod(FILE_4_REF, metricKey, 0);
    assertRawMeasureValueOnPeriod(DIRECTORY_REF, metricKey, 0);
    assertRawMeasureValueOnPeriod(ROOT_REF, metricKey, 0);
  }

  private void assertNoRawMeasure(int componentRef, String metricKey) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey)).isNotPresent();
  }

  private void assertNoRawMeasures(String metricKey) {
    assertThat(measureRepository.getAddedRawMeasures(FILE_1_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(FILE_2_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(FILE_3_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(FILE_4_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(DIRECTORY_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF).get(metricKey)).isNull();
  }
}
