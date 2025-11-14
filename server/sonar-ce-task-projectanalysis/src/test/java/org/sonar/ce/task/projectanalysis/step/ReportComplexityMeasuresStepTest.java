/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CLASSES;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

class ReportComplexityMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_REF = 1111;
  private static final int FILE_1_REF = 11111;
  private static final int FILE_2_REF = 11121;

  @RegisterExtension
  private final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(PROJECT, ROOT_REF)
      .addChildren(
        builder(DIRECTORY, DIRECTORY_REF)
          .addChildren(
            builder(FILE, FILE_1_REF).build(),
            builder(FILE, FILE_2_REF).build())
          .build())
      .build());
  @RegisterExtension
  private final MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(COMPLEXITY)
    .add(FILES)
    .add(CLASSES)
    .add(FUNCTIONS)
    .add(COGNITIVE_COMPLEXITY);

  @RegisterExtension
  private final MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private final ComputationStep underTest = new ComplexityMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  void aggregate_complexity() {
    verify_sum_aggregation(COMPLEXITY_KEY);
  }

  @Test
  void aggregate_cognitive_complexity() {
    verify_sum_aggregation(COGNITIVE_COMPLEXITY_KEY);
  }

  private void verify_sum_aggregation(String metricKey) {
    measureRepository.addRawMeasure(FILE_1_REF, metricKey, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, metricKey, newMeasureBuilder().create(40));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, metricKey)).isNotPresent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, metricKey)).isNotPresent();

    int expectedNonFileValue = 50;
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
  }

}
