/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.duplication;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class DuplicationMeasuresTest {
  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12342;
  private static final int FILE_3_REF = 1261;
  private static final int FILE_4_REF = 1262;
  private static final int FILE_5_REF = 1263;

  private static final FileAttributes FILE_1_ATTRS = mock(FileAttributes.class);
  private static final FileAttributes FILE_2_ATTRS = mock(FileAttributes.class);
  private static final FileAttributes FILE_3_ATTRS = mock(FileAttributes.class);
  private static final FileAttributes FILE_4_ATTRS = mock(FileAttributes.class);
  private static final FileAttributes FILE_5_ATTRS = mock(FileAttributes.class);

  private static final String SOME_FILE_KEY = "some file key";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF)
            .addChildren(
              builder(FILE, FILE_1_REF).setFileAttributes(FILE_1_ATTRS).build(),
              builder(FILE, FILE_2_REF).setFileAttributes(FILE_2_ATTRS).build())
            .build(),
          builder(FILE, FILE_3_REF).setFileAttributes(FILE_3_ATTRS).build(),
          builder(FILE, FILE_4_REF).setFileAttributes(FILE_4_ATTRS).build(),
          builder(FILE, FILE_5_REF).setFileAttributes(FILE_5_ATTRS).build())
        .build());
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(DUPLICATED_BLOCKS)
    .add(DUPLICATED_FILES)
    .add(DUPLICATED_LINES)
    .add(DUPLICATED_LINES_DENSITY);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);

  private DuplicationMeasures underTest = new DuplicationMeasures(treeRootHolder, metricRepository, measureRepository, duplicationRepository);

  @Before
  public void before() {
    when(FILE_5_ATTRS.isUnitTest()).thenReturn(true);
  }

  @Test
  public void compute_duplicated_blocks_one_for_original_one_for_each_InnerDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(2, 2), new TextBlock(3, 3), new TextBlock(2, 3));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_BLOCKS_KEY, 4);
  }

  @Test
  public void dont_compute_duplicated_blocks_for_test_files() {
    duplicationRepository.addDuplication(FILE_5_REF, new TextBlock(1, 1), new TextBlock(3, 3));
    duplicationRepository.addDuplication(FILE_5_REF, new TextBlock(2, 2), new TextBlock(3, 3));

    underTest.execute();

    assertRawMeasureValue(FILE_5_REF, DUPLICATED_BLOCKS_KEY, 0);
    assertRawMeasureValue(FILE_5_REF, DUPLICATED_FILES_KEY, 0);

  }

  @Test
  public void compute_duplicated_blocks_does_not_count_blocks_only_once_it_assumes_consistency_from_duplication_data() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 1), new TextBlock(3, 3));
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(2, 2), new TextBlock(3, 3));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_BLOCKS_KEY, 4);
  }

  @Test
  public void compute_duplicated_blocks_one_for_original_and_ignores_InProjectDuplicate() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 1), FILE_2_REF, new TextBlock(2, 2));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_BLOCKS_KEY, 1);
  }

  @Test
  public void compute_duplicated_blocks_one_for_original_and_ignores_CrossProjectDuplicate() {
    duplicationRepository.addCrossProjectDuplication(FILE_1_REF, new TextBlock(1, 1), SOME_FILE_KEY, new TextBlock(2, 2));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_BLOCKS_KEY, 1);
  }

  @Test
  public void compute_and_aggregate_duplicated_blocks_from_single_duplication() {
    addDuplicatedBlock(FILE_1_REF, 10);
    addDuplicatedBlock(FILE_2_REF, 40);
    addDuplicatedBlock(FILE_4_REF, 5);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_BLOCKS_KEY, 10);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_BLOCKS_KEY, 40);
    assertRawMeasureValue(FILE_3_REF, DUPLICATED_BLOCKS_KEY, 0);
    assertRawMeasureValue(FILE_4_REF, DUPLICATED_BLOCKS_KEY, 5);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_BLOCKS_KEY, 50);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_BLOCKS_KEY, 55);
  }

  @Test
  public void compute_and_aggregate_duplicated_blocks_to_zero_when_no_duplication() {
    underTest.execute();

    assertComputedAndAggregatedToZeroInt(DUPLICATED_BLOCKS_KEY);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate_of_a_single_line() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(2, 2));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_KEY, 2);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_ignores_InProjectDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, FILE_2_REF, new TextBlock(2, 2));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_KEY, 1);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_ignores_CrossProjectDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addCrossProjectDuplication(FILE_1_REF, original, SOME_FILE_KEY, new TextBlock(2, 2));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_KEY, 1);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate() {
    TextBlock original = new TextBlock(1, 5);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(10, 11));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_KEY, 7);
  }

  @Test
  public void compute_duplicated_lines_counts_lines_from_original_and_InnerDuplicate_only_once() {
    TextBlock original = new TextBlock(1, 12);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(10, 11), new TextBlock(11, 15));
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(2, 2), new TextBlock(96, 96));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_KEY, 16);
  }

  @Test
  public void compute_and_aggregate_duplicated_files() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_3_REF, 10);
    addDuplicatedBlock(FILE_4_REF, 50);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_FILES_KEY, 1);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_FILES_KEY, 0);
    assertRawMeasureValue(FILE_3_REF, DUPLICATED_FILES_KEY, 1);
    assertRawMeasureValue(FILE_4_REF, DUPLICATED_FILES_KEY, 1);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_FILES_KEY, 1);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_FILES_KEY, 3);
  }

  @Test
  public void compute_and_aggregate_zero_duplicated_files_when_no_duplication_data() {
    underTest.execute();

    assertComputedAndAggregatedToZeroInt(DUPLICATED_FILES_KEY);
  }

  @Test
  public void compute_and_aggregate_duplicated_lines() {
    addDuplicatedBlock(FILE_1_REF, 10);
    addDuplicatedBlock(FILE_2_REF, 9);
    addDuplicatedBlock(FILE_4_REF, 7);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_KEY, 10);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_KEY, 9);
    assertRawMeasureValue(FILE_3_REF, DUPLICATED_LINES_KEY, 0);
    assertRawMeasureValue(FILE_4_REF, DUPLICATED_LINES_KEY, 7);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_KEY, 19);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_KEY, 26);
  }

  @Test
  public void compute_and_aggregate_zero_duplicated_line_when_no_duplication() {
    underTest.execute();

    assertComputedAndAggregatedToZeroInt(DUPLICATED_LINES_KEY);
  }

  @Test
  public void compute_and_aggregate_duplicated_lines_density_using_lines() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_2_REF, 3);

    when(FILE_1_ATTRS.getLines()).thenReturn(10);
    when(FILE_2_ATTRS.getLines()).thenReturn(40);

    // this should have no effect as it's a test file
    when(FILE_5_ATTRS.getLines()).thenReturn(1_000_000);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 20d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 7.5d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
  }

  @Test
  public void compute_zero_percent_duplicated_lines_density_when_there_is_no_duplication() {
    when(FILE_1_ATTRS.getLines()).thenReturn(10);
    when(FILE_2_ATTRS.getLines()).thenReturn(40);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
  }

  @Test
  public void dont_compute_duplicated_lines_density_when_lines_is_zero() {
    when(FILE_1_ATTRS.getLines()).thenReturn(0);
    when(FILE_2_ATTRS.getLines()).thenReturn(0);
    underTest.execute();
    assertNoRawMeasures(DUPLICATED_LINES_DENSITY_KEY);
  }

  @Test
  public void compute_100_percent_duplicated_lines_density() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_2_REF, 3);

    when(FILE_1_ATTRS.getLines()).thenReturn(2);
    when(FILE_2_ATTRS.getLines()).thenReturn(3);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
  }

  /**
   * Adds duplication blocks of a single line (each line is specific to its block).
   * This is a very simple use case, convenient for unit tests but more realistic and complex use cases must be tested separately.
   */
  private void addDuplicatedBlock(int fileRef, int blockCount) {
    checkArgument(blockCount > 1, "BlockCount can not be less than 2");
    TextBlock original = new TextBlock(1, 1);
    TextBlock[] duplicates = new TextBlock[blockCount - 1];
    for (int i = 10; i < blockCount + 9; i++) {
      duplicates[i - 10] = new TextBlock(i, i);
    }
    duplicationRepository.addDuplication(fileRef, original, duplicates);
  }

  private void assertNoRawMeasures(String metricKey) {
    assertThat(measureRepository.getAddedRawMeasures(FILE_1_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(FILE_2_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(DIRECTORY_REF).get(metricKey)).isNull();
    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF).get(metricKey)).isNull();
  }

  private void assertNoRawMeasure(int componentRef, String metricKey) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey)).isNotPresent();
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, int value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getIntValue()).isEqualTo(value);
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, double value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getDoubleValue()).isEqualTo(value);
  }

  private void assertComputedAndAggregatedToZeroInt(String metricKey) {
    assertRawMeasureValue(FILE_1_REF, metricKey, 0);
    assertRawMeasureValue(FILE_2_REF, metricKey, 0);
    assertRawMeasureValue(FILE_3_REF, metricKey, 0);
    assertRawMeasureValue(FILE_4_REF, metricKey, 0);
    assertRawMeasureValue(DIRECTORY_REF, metricKey, 0);
    assertRawMeasureValue(ROOT_REF, metricKey, 0);
  }

}
