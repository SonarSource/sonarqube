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
package org.sonar.server.computation.task.projectanalysis.duplication;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class ReportDuplicationMeasuresTest {
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
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(LINES)
    .add(NCLOC)
    .add(COMMENT_LINES)
    .add(DUPLICATED_BLOCKS)
    .add(DUPLICATED_FILES)
    .add(DUPLICATED_LINES)
    .add(DUPLICATED_LINES_DENSITY);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);

  private DuplicationMeasures underTest = new DuplicationMeasures(treeRootHolder, metricRepository, measureRepository, duplicationRepository);

  @Test
  public void compute_duplicated_blocks_one_for_original_one_for_each_InnerDuplicate() {
    TextBlock original = new TextBlock(1, 1);
    duplicationRepository.addDuplication(FILE_1_REF, original, new TextBlock(2, 2), new TextBlock(3, 3), new TextBlock(2, 3));

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_BLOCKS_KEY, 4);
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
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 1), SOME_FILE_KEY, new TextBlock(2, 2));

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
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_BLOCKS_KEY, 50);
    assertRawMeasureValue(SUB_MODULE_2_REF, DUPLICATED_BLOCKS_KEY, 5);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_BLOCKS_KEY, 55);
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
    duplicationRepository.addDuplication(FILE_1_REF, original, SOME_FILE_KEY, new TextBlock(2, 2));

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
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_FILES_KEY, 1);
    assertRawMeasureValue(SUB_MODULE_2_REF, DUPLICATED_FILES_KEY, 2);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_FILES_KEY, 3);
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
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_LINES_KEY, 19);
    assertRawMeasureValue(SUB_MODULE_2_REF, DUPLICATED_LINES_KEY, 7);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_LINES_KEY, 26);
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

    addRawMeasure(FILE_1_REF, LINES_KEY, 10);
    addRawMeasure(FILE_2_REF, LINES_KEY, 40);
    addRawMeasure(DIRECTORY_REF, LINES_KEY, 50);
    addRawMeasure(SUB_MODULE_1_REF, LINES_KEY, 50);
    addRawMeasure(MODULE_REF, LINES_KEY, 50);
    addRawMeasure(ROOT_REF, LINES_KEY, 50);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 20d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 7.5d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertNoRawMeasure(SUB_MODULE_2_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
  }

  @Test
  public void compute_and_aggregate_duplicated_lines_density_using_nclocs_and_comment_lines() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_2_REF, 3);

    addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, 2);
    addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, 10);
    addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, 12);
    addRawMeasure(SUB_MODULE_1_REF, COMMENT_LINES_KEY, 12);
    addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, 12);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 12);

    addRawMeasure(FILE_1_REF, NCLOC_KEY, 8);
    addRawMeasure(FILE_2_REF, NCLOC_KEY, 30);
    addRawMeasure(DIRECTORY_REF, NCLOC_KEY, 38);
    addRawMeasure(SUB_MODULE_1_REF, NCLOC_KEY, 38);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 38);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 38);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 20d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 7.5d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertNoRawMeasure(SUB_MODULE_2_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
  }

  @Test
  public void compute_duplicated_lines_density_using_only_nclocs() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_2_REF, 3);

    addRawMeasure(FILE_1_REF, NCLOC_KEY, 10);
    addRawMeasure(FILE_2_REF, NCLOC_KEY, 40);
    addRawMeasure(DIRECTORY_REF, NCLOC_KEY, 50);
    addRawMeasure(SUB_MODULE_1_REF, NCLOC_KEY, 50);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 50);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 50);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 20d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 7.5d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertNoRawMeasure(SUB_MODULE_2_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 10d);
  }

  @Test
  public void compute_zero_percent_duplicated_lines_density_when_there_is_no_duplication() {
    addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, 2);
    addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, 10);
    addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, 12);
    addRawMeasure(SUB_MODULE_1_REF, COMMENT_LINES_KEY, 12);
    addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, 12);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 12);

    addRawMeasure(FILE_1_REF, NCLOC_KEY, 8);
    addRawMeasure(FILE_2_REF, NCLOC_KEY, 30);
    addRawMeasure(DIRECTORY_REF, NCLOC_KEY, 38);
    addRawMeasure(SUB_MODULE_1_REF, NCLOC_KEY, 38);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 38);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 38);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertNoRawMeasure(SUB_MODULE_2_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 0d);
  }

  @Test
  public void not_compute_duplicated_lines_density_when_lines_is_zero() {
    addRawMeasure(FILE_1_REF, LINES_KEY, 0);
    addRawMeasure(FILE_2_REF, LINES_KEY, 0);
    addRawMeasure(DIRECTORY_REF, LINES_KEY, 0);
    addRawMeasure(SUB_MODULE_1_REF, LINES_KEY, 0);
    addRawMeasure(MODULE_REF, LINES_KEY, 0);
    addRawMeasure(ROOT_REF, LINES_KEY, 0);

    underTest.execute();

    assertNoRawMeasures(DUPLICATED_LINES_DENSITY_KEY);
  }

  @Test
  public void not_compute_duplicated_lines_density_when_ncloc_and_comment_are_zero() {
    addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, 0);
    addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, 0);
    addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, 0);
    addRawMeasure(SUB_MODULE_1_REF, COMMENT_LINES_KEY, 0);
    addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, 0);
    addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, 0);

    addRawMeasure(FILE_1_REF, NCLOC_KEY, 0);
    addRawMeasure(FILE_2_REF, NCLOC_KEY, 0);
    addRawMeasure(DIRECTORY_REF, NCLOC_KEY, 0);
    addRawMeasure(SUB_MODULE_1_REF, NCLOC_KEY, 0);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 0);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 0);

    underTest.execute();

    assertNoRawMeasures(DUPLICATED_LINES_DENSITY_KEY);
  }

  @Test
  public void compute_100_percent_duplicated_lines_density() {
    addDuplicatedBlock(FILE_1_REF, 2);
    addDuplicatedBlock(FILE_2_REF, 3);

    addRawMeasure(FILE_1_REF, LINES_KEY, 2);
    addRawMeasure(FILE_2_REF, LINES_KEY, 3);
    addRawMeasure(DIRECTORY_REF, LINES_KEY, 5);
    addRawMeasure(SUB_MODULE_1_REF, LINES_KEY, 5);
    addRawMeasure(MODULE_REF, LINES_KEY, 5);
    addRawMeasure(ROOT_REF, LINES_KEY, 5);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertRawMeasureValue(FILE_2_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertNoRawMeasure(FILE_3_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertNoRawMeasure(FILE_4_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(DIRECTORY_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertRawMeasureValue(SUB_MODULE_1_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertNoRawMeasure(SUB_MODULE_2_REF, DUPLICATED_LINES_DENSITY_KEY);
    assertRawMeasureValue(MODULE_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
    assertRawMeasureValue(ROOT_REF, DUPLICATED_LINES_DENSITY_KEY, 100d);
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
    for (int i = 10; i < blockCount + 9; i++) {
      duplicates[i - 10] = new TextBlock(i, i);
    }
    duplicationRepository.addDuplication(fileRef, original, duplicates);
  }

  private void addRawMeasure(int componentRef, String metricKey, int value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void assertNoRawMeasures(String metricKey) {
    assertThat(measureRepository.getAddedRawMeasures(FILE_1_REF).get(metricKey)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(FILE_2_REF).get(metricKey)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(DIRECTORY_REF).get(metricKey)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(SUB_MODULE_1_REF).get(metricKey)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(MODULE_REF).get(metricKey)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF).get(metricKey)).isEmpty();
  }

  private void assertNoRawMeasure(int componentRef, String metricKey) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey)).isAbsent();
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
    assertRawMeasureValue(SUB_MODULE_1_REF, metricKey, 0);
    assertRawMeasureValue(SUB_MODULE_2_REF, metricKey, 0);
    assertRawMeasureValue(MODULE_REF, metricKey, 0);
    assertRawMeasureValue(ROOT_REF, metricKey, 0);
  }

}
