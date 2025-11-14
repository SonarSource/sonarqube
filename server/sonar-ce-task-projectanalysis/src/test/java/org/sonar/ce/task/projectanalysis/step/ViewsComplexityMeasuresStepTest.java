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
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;

class ViewsComplexityMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_REF = 11;
  private static final int SUB_SUBVIEW_1_REF = 111;
  private static final int PROJECT_VIEW_1_REF = 11111;
  private static final int PROJECT_VIEW_2_REF = 11121;
  private static final int SUB_SUBVIEW_2_REF = 112;
  private static final int PROJECT_VIEW_3_REF = 12;

  @RegisterExtension
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(VIEW, ROOT_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_REF)
          .addChildren(
            builder(SUBVIEW, SUB_SUBVIEW_1_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
                builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build())
              .build(),
            builder(SUBVIEW, SUB_SUBVIEW_2_REF).build())
          .build(),
        builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
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
  void aggregate_cognitive_complexity_in_functions() {
    verify_sum_aggregation(COGNITIVE_COMPLEXITY_KEY);
  }

  private void verify_sum_aggregation(String metricKey) {
    addRawMeasureValue(PROJECT_VIEW_1_REF, metricKey, 10);
    addRawMeasureValue(PROJECT_VIEW_2_REF, metricKey, 40);
    addRawMeasureValue(PROJECT_VIEW_3_REF, metricKey, 20);

    underTest.execute(new TestComputationStepContext());

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasures(SUB_SUBVIEW_1_REF, metricKey, 50);
    assertNoAddedRawMeasure(SUB_SUBVIEW_2_REF);
    assertAddedRawMeasures(SUBVIEW_REF, metricKey, 50);
    assertAddedRawMeasures(ROOT_REF, metricKey, 70);
  }

  private void assertNoAddedRawMeasureOnProjectViews() {
    assertNoAddedRawMeasure(PROJECT_VIEW_1_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_2_REF);
    assertNoAddedRawMeasure(PROJECT_VIEW_3_REF);
  }

  private void assertNoAddedRawMeasure(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef)).isEmpty();
  }

  private void assertAddedRawMeasures(int componentRef, String metricKey, int expected) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(expected)));
  }

  private void addRawMeasureValue(int componentRef, String metricKey, int value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

}
