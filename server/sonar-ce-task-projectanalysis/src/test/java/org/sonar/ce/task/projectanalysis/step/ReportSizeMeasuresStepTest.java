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

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ReportSizeMeasuresStepTest {

  private static final String LANGUAGE_DOES_NOT_MATTER_HERE = null;
  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_1_REF = 1234;
  private static final int DIRECTORY_2_REF = 1235;
  private static final int DIRECTORY_3_REF = 1236;
  private static final int DIRECTORY_4_REF = 1237;
  private static final int DIRECTORY_4_1_REF = 1238;
  private static final int DIRECTORY_4_2_REF = 1239;
  private static final int DIRECTORY_4_3_REF = 1240;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12343;
  private static final int FILE_3_REF = 12351;
  private static final int FILE_4_REF = 12371;
  private static final int FILE_5_REF = 12372;
  private static final int UNIT_TEST_1_REF = 12352;
  private static final int UNIT_TEST_2_REF = 12361;
  private static final int UNIT_TEST_3_REF = 12362;
  private static final Integer NO_METRIC = null;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(
    builder(PROJECT, ROOT_REF)
      .addChildren(
        builder(DIRECTORY, DIRECTORY_1_REF)
          .addChildren(
            builder(FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_DOES_NOT_MATTER_HERE, 1)).build(),
            builder(FILE, FILE_2_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_DOES_NOT_MATTER_HERE, 2)).build())
          .build(),
        builder(DIRECTORY, DIRECTORY_2_REF)
          .addChildren(
            builder(FILE, FILE_3_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_DOES_NOT_MATTER_HERE, 7)).build(),
            builder(FILE, UNIT_TEST_1_REF).setFileAttributes(new FileAttributes(true, LANGUAGE_DOES_NOT_MATTER_HERE, 4)).build())
          .build(),
        builder(DIRECTORY, DIRECTORY_3_REF)
          .addChildren(
            builder(FILE, UNIT_TEST_2_REF).setFileAttributes(new FileAttributes(true, LANGUAGE_DOES_NOT_MATTER_HERE, 10)).build())
          .build(),
        builder(DIRECTORY, DIRECTORY_4_REF)
          .addChildren(
            builder(DIRECTORY, DIRECTORY_4_1_REF)
              .addChildren(
                builder(FILE, FILE_4_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_DOES_NOT_MATTER_HERE, 9)).build()).build(),
            builder(DIRECTORY, DIRECTORY_4_2_REF)
              .addChildren(
                builder(FILE, FILE_5_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_DOES_NOT_MATTER_HERE, 5)).build()).build(),
            builder(DIRECTORY, DIRECTORY_4_3_REF)
              .addChildren(
                builder(FILE, UNIT_TEST_3_REF).setFileAttributes(new FileAttributes(true, LANGUAGE_DOES_NOT_MATTER_HERE, 6)).build()).build()
          ).build()
      ).build());

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.FILES)
    .add(CoreMetrics.DIRECTORIES)
    .add(CoreMetrics.LINES)
    .add(CoreMetrics.GENERATED_LINES)
    .add(CoreMetrics.NCLOC)
    .add(CoreMetrics.GENERATED_NCLOC)
    .add(CoreMetrics.FUNCTIONS)
    .add(CoreMetrics.STATEMENTS)
    .add(CoreMetrics.CLASSES);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private SizeMeasuresStep underTest = new SizeMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  public void verify_LINES_and_FILE_and_DIRECTORY_computation_and_aggregation() {
    underTest.execute(new TestComputationStepContext());

    verifyMeasuresOnFile(FILE_1_REF, 1, 1);
    verifyMeasuresOnFile(FILE_2_REF, 2, 1);
    verifyMeasuresOnFile(FILE_3_REF, 7, 1);
    verifyMeasuresOnFile(FILE_4_REF, 9, 1);
    verifyMeasuresOnFile(FILE_5_REF, 5, 1);
    verifyNoMeasure(UNIT_TEST_1_REF);
    verifyNoMeasure(UNIT_TEST_2_REF);
    verifyMeasuresOnOtherComponent(DIRECTORY_1_REF, 3, 2, NO_METRIC);
    verifyMeasuresOnOtherComponent(DIRECTORY_2_REF, 7, 1, NO_METRIC);
    verifyMeasuresOnOtherComponent(DIRECTORY_3_REF, NO_METRIC, NO_METRIC, NO_METRIC);
    verifyMeasuresOnOtherComponent(DIRECTORY_4_REF, 14, 2, 2);
    verifyMeasuresOnOtherComponent(ROOT_REF, 24, 5, 5);
  }

  @Test
  public void verify_GENERATED_LINES_related_measures_aggregation() {
    verifyMetricAggregation(GENERATED_LINES_KEY);
  }

  @Test
  public void verify_NCLOC_measure_aggregation() {
    verifyMetricAggregation(NCLOC_KEY);
  }

  private void verifyMetricAggregation(String metricKey) {
    measureRepository.addRawMeasure(FILE_1_REF, metricKey, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, metricKey, newMeasureBuilder().create(6));
    measureRepository.addRawMeasure(UNIT_TEST_1_REF, metricKey, newMeasureBuilder().create(3));

    underTest.execute(new TestComputationStepContext());

    verifyMeasuresOnFile(FILE_1_REF, 1, 1);
    verifyMeasuresOnFile(FILE_2_REF, 2, 1);
    verifyMeasuresOnFile(FILE_3_REF, 7, 1);
    verifyMeasuresOnFile(FILE_4_REF, 9, 1);
    verifyMeasuresOnFile(FILE_5_REF, 5, 1);
    verifyNoMeasure(UNIT_TEST_1_REF);
    verifyNoMeasure(UNIT_TEST_2_REF);
    verifyMeasuresOnOtherComponent(DIRECTORY_1_REF, 3, 2, NO_METRIC, entryOf(metricKey, newMeasureBuilder().create(16)));
    verifyMeasuresOnOtherComponent(DIRECTORY_2_REF, 7, 1, NO_METRIC, entryOf(metricKey, newMeasureBuilder().create(3)));
    verifyMeasuresOnOtherComponent(DIRECTORY_3_REF, NO_METRIC, NO_METRIC, NO_METRIC);
    verifyMeasuresOnOtherComponent(DIRECTORY_4_REF, 14, 2, 2);
    verifyMeasuresOnOtherComponent(ROOT_REF, 24, 5, 5, entryOf(metricKey, newMeasureBuilder().create(19)));
  }

  private void verifyTwoMeasureAggregation(String metric1Key, String metric2Key) {
    measureRepository.addRawMeasure(FILE_1_REF, metric1Key, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(FILE_1_REF, metric2Key, newMeasureBuilder().create(10));
    // FILE_2_REF has no metric2 measure
    measureRepository.addRawMeasure(FILE_2_REF, metric1Key, newMeasureBuilder().create(6));
    // FILE_3_REF has no measure at all
    // UNIT_TEST_1_REF has no metric1
    measureRepository.addRawMeasure(UNIT_TEST_1_REF, metric2Key, newMeasureBuilder().create(90));
    measureRepository.addRawMeasure(FILE_4_REF, metric1Key, newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(FILE_4_REF, metric2Key, newMeasureBuilder().create(3));

    underTest.execute(new TestComputationStepContext());

    verifyMeasuresOnFile(FILE_1_REF, 1, 1);
    verifyMeasuresOnFile(FILE_2_REF, 2, 1);
    verifyMeasuresOnFile(FILE_3_REF, 7, 1);
    verifyNoMeasure(UNIT_TEST_1_REF);
    verifyNoMeasure(UNIT_TEST_2_REF);
    verifyMeasuresOnOtherComponent(DIRECTORY_1_REF, 3, 2, NO_METRIC,
      entryOf(metric1Key, newMeasureBuilder().create(7)), entryOf(metric2Key, newMeasureBuilder().create(10)));
    verifyMeasuresOnOtherComponent(DIRECTORY_2_REF, 7, 1, NO_METRIC,
      entryOf(metric2Key, newMeasureBuilder().create(90)));
    MeasureRepoEntry[] subModuleAndAboveEntries = {
      entryOf(metric1Key, newMeasureBuilder().create(9)),
      entryOf(metric2Key, newMeasureBuilder().create(103))
    };
    verifyMeasuresOnOtherComponent(DIRECTORY_3_REF, NO_METRIC, NO_METRIC, NO_METRIC);
    verifyMeasuresOnOtherComponent(ROOT_REF, 24, 5, 5, subModuleAndAboveEntries);
  }

  @Test
  public void verify_FUNCTIONS_and_STATEMENT_measure_aggregation() {
    verifyTwoMeasureAggregation(FUNCTIONS_KEY, STATEMENTS_KEY);
  }

  @Test
  public void verify_CLASSES_measure_aggregation() {
    verifyMetricAggregation(CLASSES_KEY);
  }

  private void verifyMeasuresOnFile(int componentRef, int linesCount, int fileCount) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(
        entryOf(LINES_KEY, newMeasureBuilder().create(linesCount)),
        entryOf(FILES_KEY, newMeasureBuilder().create(fileCount)));
  }

  private void verifyMeasuresOnOtherComponent(int componentRef, @Nullable Integer linesCount, @Nullable Integer fileCount, @Nullable Integer directoryCount,
    MeasureRepoEntry... otherMeasures) {
    MeasureRepoEntry[] measureRepoEntries = concatIntoArray(
      otherMeasures,
      linesCount == null ? null : entryOf(LINES_KEY, newMeasureBuilder().create(linesCount)),
      fileCount == null ? null : entryOf(FILES_KEY, newMeasureBuilder().create(fileCount)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(measureRepoEntries);
  }

  private static MeasureRepoEntry[] concatIntoArray(MeasureRepoEntry[] otherMeasures, MeasureRepoEntry... measureRepoEntries) {
    return from(concat(
      asList(otherMeasures),
      from(asList(measureRepoEntries)).filter(notNull())))
      .toArray(MeasureRepoEntry.class);
  }

  private void verifyNoMeasure(int componentRef) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).isEmpty();
  }
}
