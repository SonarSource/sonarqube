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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class ViewsLanguageDistributionMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int SUBVIEW_1_REF = 12;
  private static final int SUB_SUBVIEW_1_REF = 121;
  private static final int PROJECT_VIEW_1_REF = 1211;
  private static final int PROJECT_VIEW_2_REF = 1212;
  private static final int PROJECT_VIEW_3_REF = 1213;
  private static final int SUB_SUBVIEW_2_REF = 122;
  private static final int SUBVIEW_2_REF = 13;
  private static final int PROJECT_VIEW_4_REF = 131;
  private static final int PROJECT_VIEW_5_REF = 14;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(VIEW, ROOT_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            builder(SUBVIEW, SUB_SUBVIEW_1_REF)
              .addChildren(
                  builder(PROJECT_VIEW, PROJECT_VIEW_1_REF).build(),
                  builder(PROJECT_VIEW, PROJECT_VIEW_2_REF).build(),
                  builder(PROJECT_VIEW, PROJECT_VIEW_3_REF).build())
              .build(),
            builder(SUBVIEW, SUB_SUBVIEW_2_REF).build())
          .build(),
        builder(SUBVIEW, SUBVIEW_2_REF)
          .addChildren(
            builder(PROJECT_VIEW, PROJECT_VIEW_4_REF).build())
          .build(),
        builder(PROJECT_VIEW, PROJECT_VIEW_5_REF).build())
      .build());
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC_LANGUAGE_DISTRIBUTION);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputationStep underTest = new LanguageDistributionMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  public void compute_ncloc_language_distribution() {
    addRawMeasure(PROJECT_VIEW_1_REF, NCLOC_LANGUAGE_DISTRIBUTION_KEY, "xoo=10");
    addRawMeasure(PROJECT_VIEW_2_REF, NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=6");
    addRawMeasure(PROJECT_VIEW_3_REF, NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=10;xoo=5");
    // no raw measure on PROJECT_VIEW_4_REF
    addRawMeasure(PROJECT_VIEW_5_REF, NCLOC_LANGUAGE_DISTRIBUTION_KEY, "<null>=3;foo=10");

    underTest.execute();

    assertNoAddedRawMeasureOnProjectViews();
    assertAddedRawMeasure(SUB_SUBVIEW_1_REF, NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=16;xoo=15");
    assertNoAddedRawMeasures(SUB_SUBVIEW_2_REF);
    assertNoAddedRawMeasures(SUBVIEW_2_REF);
    assertAddedRawMeasure(SUBVIEW_1_REF, NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=16;xoo=15");
    assertAddedRawMeasure(ROOT_REF, NCLOC_LANGUAGE_DISTRIBUTION_KEY, "<null>=3;foo=10;java=16;xoo=15");
  }

  private void assertAddedRawMeasure(int componentRef, String metricKey, String value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getStringValue()).isEqualTo(value);
  }

  private void addRawMeasure(int componentRef, String metricKey, String value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void assertNoAddedRawMeasureOnProjectViews() {
    assertNoAddedRawMeasures(PROJECT_VIEW_1_REF);
    assertNoAddedRawMeasures(PROJECT_VIEW_2_REF);
    assertNoAddedRawMeasures(PROJECT_VIEW_3_REF);
    assertNoAddedRawMeasures(PROJECT_VIEW_4_REF);
    assertNoAddedRawMeasures(PROJECT_VIEW_5_REF);
  }

  private void assertNoAddedRawMeasures(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef)).isEmpty();
  }

}
