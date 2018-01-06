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
package org.sonar.server.computation.task.projectanalysis.step;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.ACCESSORS;
import static org.sonar.api.measures.CoreMetrics.CLASSES;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.DIRECTORIES;
import static org.sonar.api.measures.CoreMetrics.DIRECTORIES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC;
import static org.sonar.api.measures.CoreMetrics.LINES;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

public class ViewsSizeMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_1_REF = 12;
  private static final int SUBVIEW_2_REF = 13;
  private static final int SUB_SUBVIEW_1_REF = 121;
  private static final int SUB_SUBVIEW_2_REF = 122;
  private static final int SUB_SUBVIEW_3_REF = 123;
  private static final int PROJECTVIEW_1_REF = 1231;
  private static final int PROJECTVIEW_2_REF = 1232;
  private static final int PROJECTVIEW_3_REF = 1241;
  private static final int PROJECTVIEW_4_REF = 1251;
  private static final int PROJECTVIEW_5_REF = 14;
  private static final Integer NO_METRIC = null;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(
    builder(VIEW, ROOT_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            builder(SUBVIEW, SUB_SUBVIEW_1_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECTVIEW_1_REF).build(),
                builder(PROJECT_VIEW, PROJECTVIEW_2_REF).build())
              .build(),
            builder(SUBVIEW, SUB_SUBVIEW_2_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECTVIEW_3_REF).build())
              .build(),
            builder(SUBVIEW, SUB_SUBVIEW_3_REF).addChildren(
              builder(PROJECT_VIEW, PROJECTVIEW_4_REF).build())
              .build())
          .build(),
        builder(SUBVIEW, SUBVIEW_2_REF).build(),
        builder(PROJECT_VIEW, PROJECTVIEW_5_REF).build())
      .build());
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(FILES)
    .add(DIRECTORIES)
    .add(LINES)
    .add(GENERATED_LINES)
    .add(NCLOC)
    .add(GENERATED_NCLOC)
    .add(FUNCTIONS)
    .add(STATEMENTS)
    .add(CLASSES)
    .add(ACCESSORS);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository)
    .addRawMeasure(PROJECTVIEW_1_REF, LINES_KEY, newMeasureBuilder().create(1))
    .addRawMeasure(PROJECTVIEW_2_REF, LINES_KEY, newMeasureBuilder().create(2))
    .addRawMeasure(PROJECTVIEW_3_REF, LINES_KEY, newMeasureBuilder().create(5))
    // PROJECTVIEW_4_REF has no lines metric
    .addRawMeasure(PROJECTVIEW_5_REF, LINES_KEY, newMeasureBuilder().create(5))

    .addRawMeasure(PROJECTVIEW_1_REF, FILES_KEY, newMeasureBuilder().create(1))
    .addRawMeasure(PROJECTVIEW_2_REF, FILES_KEY, newMeasureBuilder().create(2))
    .addRawMeasure(PROJECTVIEW_3_REF, FILES_KEY, newMeasureBuilder().create(3))
    // PROJECTVIEW_4_REF has no file metric
    .addRawMeasure(PROJECTVIEW_5_REF, FILES_KEY, newMeasureBuilder().create(5))
    .addRawMeasure(PROJECTVIEW_1_REF, DIRECTORIES_KEY, newMeasureBuilder().create(1))
    .addRawMeasure(PROJECTVIEW_2_REF, DIRECTORIES_KEY, newMeasureBuilder().create(2))
    // PROJECTVIEW_3_REF has no directory metric
    .addRawMeasure(PROJECTVIEW_4_REF, DIRECTORIES_KEY, newMeasureBuilder().create(4))
    .addRawMeasure(PROJECTVIEW_5_REF, DIRECTORIES_KEY, newMeasureBuilder().create(5));

  private SizeMeasuresStep underTest = new SizeMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  public void verify_FILE_and_DIRECTORY_computation_and_aggregation() {
    underTest.execute();

    verifyNoMeasure(PROJECTVIEW_1_REF);
    verifyNoMeasure(PROJECTVIEW_2_REF);
    verifyNoMeasure(PROJECTVIEW_3_REF);
    verifyNoMeasure(PROJECTVIEW_4_REF);
    verifyNoMeasure(PROJECTVIEW_5_REF);
    verifyMeasures(SUB_SUBVIEW_1_REF, 3, 3, 3);
    verifyMeasures(SUB_SUBVIEW_2_REF, 5, 3, 0);
    verifyMeasures(SUB_SUBVIEW_3_REF, NO_METRIC, NO_METRIC, NO_METRIC);
    verifyMeasures(SUBVIEW_1_REF, 8, 6, 7);
    verifyMeasures(SUBVIEW_2_REF, NO_METRIC, NO_METRIC, NO_METRIC);
    verifyMeasures(ROOT_REF, 13, 11, 12);
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
    measureRepository.addRawMeasure(PROJECTVIEW_1_REF, metricKey, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(PROJECTVIEW_2_REF, metricKey, newMeasureBuilder().create(6));
    measureRepository.addRawMeasure(PROJECTVIEW_4_REF, metricKey, newMeasureBuilder().create(3));
    measureRepository.addRawMeasure(PROJECTVIEW_5_REF, metricKey, newMeasureBuilder().create(7));

    underTest.execute();

    verifyNoMeasure(PROJECTVIEW_1_REF);
    verifyNoMeasure(PROJECTVIEW_2_REF);
    verifyNoMeasure(PROJECTVIEW_3_REF);
    verifyNoMeasure(PROJECTVIEW_4_REF);
    verifyNoMeasure(PROJECTVIEW_5_REF);
    verifyMeasures(SUB_SUBVIEW_1_REF, 3, 3, 3, entryOf(metricKey, newMeasureBuilder().create(16)));
    verifyMeasures(SUB_SUBVIEW_2_REF, 5, 3, 0);
    verifyMeasures(SUB_SUBVIEW_3_REF, NO_METRIC, NO_METRIC, NO_METRIC, entryOf(metricKey, newMeasureBuilder().create(3)));
    verifyMeasures(SUBVIEW_1_REF, 8, 6, 7, entryOf(metricKey, newMeasureBuilder().create(19)));
    verifyMeasures(SUBVIEW_2_REF, NO_METRIC, NO_METRIC, NO_METRIC);
    verifyMeasures(ROOT_REF, 13, 11, 12, entryOf(metricKey, newMeasureBuilder().create(26)));
  }

  private void verifyTwoMeasureAggregation(String metric1Key, String metric2Key) {
    measureRepository.addRawMeasure(PROJECTVIEW_1_REF, metric1Key, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(PROJECTVIEW_1_REF, metric2Key, newMeasureBuilder().create(10));
    // PROJECTVIEW_2_REF has no metric2 measure
    measureRepository.addRawMeasure(PROJECTVIEW_2_REF, metric1Key, newMeasureBuilder().create(6));
    // PROJECTVIEW_3_REF has no measure at all
    // PROJECTVIEW_4_REF has no metric1
    measureRepository.addRawMeasure(PROJECTVIEW_4_REF, metric2Key, newMeasureBuilder().create(90));
    measureRepository.addRawMeasure(PROJECTVIEW_5_REF, metric1Key, newMeasureBuilder().create(3));
    measureRepository.addRawMeasure(PROJECTVIEW_5_REF, metric2Key, newMeasureBuilder().create(7));

    underTest.execute();

    verifyNoMeasure(PROJECTVIEW_1_REF);
    verifyNoMeasure(PROJECTVIEW_2_REF);
    verifyNoMeasure(PROJECTVIEW_3_REF);
    verifyNoMeasure(PROJECTVIEW_4_REF);
    verifyNoMeasure(PROJECTVIEW_5_REF);
    verifyNoMeasure(PROJECTVIEW_4_REF);
    verifyMeasures(SUB_SUBVIEW_1_REF, 3, 3, 3,
      entryOf(metric1Key, newMeasureBuilder().create(7)), entryOf(metric2Key, newMeasureBuilder().create(10)));
    verifyMeasures(SUB_SUBVIEW_2_REF, 5, 3, 0);
    verifyMeasures(SUB_SUBVIEW_3_REF, NO_METRIC, NO_METRIC, NO_METRIC,
      entryOf(metric2Key, newMeasureBuilder().create(90)));
    verifyMeasures(SUBVIEW_1_REF, 8, 6, 7,
      entryOf(metric1Key, newMeasureBuilder().create(7)), entryOf(metric2Key, newMeasureBuilder().create(100)));
    verifyMeasures(SUBVIEW_2_REF, NO_METRIC, NO_METRIC, NO_METRIC);
    verifyMeasures(ROOT_REF, 13, 11, 12,
      entryOf(metric1Key, newMeasureBuilder().create(10)), entryOf(metric2Key, newMeasureBuilder().create(107)));
  }

  @Test
  public void verify_FUNCTIONS_and_STATEMENT_measure_aggregation() {
    verifyTwoMeasureAggregation(FUNCTIONS_KEY, STATEMENTS_KEY);
  }

  @Test
  public void verify_CLASSES_measure_aggregation() {
    verifyMetricAggregation(CLASSES_KEY);
  }

  private void verifyMeasures(int componentRef, @Nullable Integer linesCount, @Nullable Integer fileCount, @Nullable Integer directoryCount, MeasureRepoEntry... otherMeasures) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(
        concatIntoArray(otherMeasures, createFileAndDirectoryEntries(linesCount, fileCount, directoryCount)));
  }

  private static MeasureRepoEntry[] createFileAndDirectoryEntries(@Nullable Integer linesCount, @Nullable Integer fileCount, @Nullable Integer directoryCount) {
    return new MeasureRepoEntry[] {
      linesCount == null ? null : entryOf(LINES_KEY, newMeasureBuilder().create(linesCount)),
      fileCount == null ? null : entryOf(FILES_KEY, newMeasureBuilder().create(fileCount)),
      directoryCount == null ? null : entryOf(DIRECTORIES_KEY, newMeasureBuilder().create(directoryCount))
    };
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
