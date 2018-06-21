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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.COMMENTED_OUT_CODE_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENTED_OUT_CODE_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ViewsComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class ViewsCommentMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_REF = 123;
  private static final int PROJECTVIEW_1_REF = 1231;
  private static final int PROJECTVIEW_2_REF = 1232;
  private static final int PROJECTVIEW_3_REF = 1233;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(COMMENT_LINES)
    .add(COMMENT_LINES_DENSITY)
    .add(COMMENTED_OUT_CODE_LINES)
    .add(PUBLIC_API)
    .add(PUBLIC_UNDOCUMENTED_API)
    .add(PUBLIC_DOCUMENTED_API_DENSITY);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputationStep underTest = new CommentMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(VIEW, ROOT_REF)
        .addChildren(
          builder(SUBVIEW, MODULE_REF)
            .addChildren(
                builder(SUBVIEW, SUB_MODULE_REF)
                    .addChildren(
                        builder(PROJECT_VIEW, PROJECTVIEW_1_REF).build(),
                        builder(PROJECT_VIEW, PROJECTVIEW_2_REF).build())
                    .build())
            .build(),
            builder(PROJECT_VIEW, PROJECTVIEW_3_REF).build())
        .build());
  }

  @Test
  public void aggregate_commented_out_code_lines() {
    addRawMeasure(PROJECTVIEW_1_REF, COMMENTED_OUT_CODE_LINES_KEY, 100);
    addRawMeasure(PROJECTVIEW_2_REF, COMMENTED_OUT_CODE_LINES_KEY, 400);
    addRawMeasure(PROJECTVIEW_3_REF, COMMENTED_OUT_CODE_LINES_KEY, 500);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, COMMENTED_OUT_CODE_LINES_KEY, 500);
    assertRawMeasureValue(MODULE_REF, COMMENTED_OUT_CODE_LINES_KEY, 500);
    assertRawMeasureValue(ROOT_REF, COMMENTED_OUT_CODE_LINES_KEY, 1000);
  }

  @Test
  public void aggregate_comment_lines() {
    addRawMeasure(PROJECTVIEW_1_REF, COMMENT_LINES_KEY, 100);
    addRawMeasure(PROJECTVIEW_2_REF, COMMENT_LINES_KEY, 400);
    addRawMeasure(PROJECTVIEW_3_REF, COMMENT_LINES_KEY, 500);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, COMMENT_LINES_KEY, 500);
    assertRawMeasureValue(MODULE_REF, COMMENT_LINES_KEY, 500);
    assertRawMeasureValue(ROOT_REF, COMMENT_LINES_KEY, 1000);
  }

  @Test
  public void compute_comment_density() {
    addRawMeasure(PROJECTVIEW_1_REF, NCLOC_KEY, 100);
    addRawMeasure(PROJECTVIEW_1_REF, COMMENT_LINES_KEY, 150);
    addRawMeasure(PROJECTVIEW_2_REF, NCLOC_KEY, 200);
    addRawMeasure(PROJECTVIEW_2_REF, COMMENT_LINES_KEY, 50);
    addRawMeasure(PROJECTVIEW_3_REF, NCLOC_KEY, 300);
    addRawMeasure(PROJECTVIEW_3_REF, COMMENT_LINES_KEY, 5);

    addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, 300);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 300);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 300);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, COMMENT_LINES_DENSITY_KEY, 40d);
    assertRawMeasureValue(MODULE_REF, COMMENT_LINES_DENSITY_KEY, 40d);
    assertRawMeasureValue(ROOT_REF, COMMENT_LINES_DENSITY_KEY, 40.6d);
  }

  @Test
  public void compute_zero_comment_density_when_zero_comment() {
    addRawMeasure(PROJECTVIEW_1_REF, NCLOC_KEY, 100);
    addRawMeasure(PROJECTVIEW_1_REF, COMMENT_LINES_KEY, 0);
    addRawMeasure(PROJECTVIEW_2_REF, NCLOC_KEY, 200);
    addRawMeasure(PROJECTVIEW_2_REF, COMMENT_LINES_KEY, 0);

    addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, 300);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 300);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 300);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, COMMENT_LINES_DENSITY_KEY, 0d);
    assertRawMeasureValue(MODULE_REF, COMMENT_LINES_DENSITY_KEY, 0d);
    assertRawMeasureValue(ROOT_REF, COMMENT_LINES_DENSITY_KEY, 0d);
  }

  @Test
  public void not_compute_comment_density_when_zero_ncloc_and_zero_comment() {
    addRawMeasure(PROJECTVIEW_1_REF, NCLOC_KEY, 0);
    addRawMeasure(PROJECTVIEW_1_REF, COMMENT_LINES_KEY, 0);
    addRawMeasure(PROJECTVIEW_2_REF, NCLOC_KEY, 0);
    addRawMeasure(PROJECTVIEW_2_REF, COMMENT_LINES_KEY, 0);

    addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, 0);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 0);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 0);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertNoRawMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void not_compute_comment_density_when_no_ncloc() {
    addRawMeasure(PROJECTVIEW_1_REF, COMMENT_LINES_KEY, 150);
    addRawMeasure(PROJECTVIEW_2_REF, COMMENT_LINES_KEY, 50);

    underTest.execute();

    assertNoRawMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void not_compute_comment_density_when_no_comment() {
    addRawMeasure(PROJECTVIEW_1_REF, NCLOC_KEY, 100);
    addRawMeasure(PROJECTVIEW_2_REF, NCLOC_KEY, 100);
    addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, 200);
    addRawMeasure(MODULE_REF, NCLOC_KEY, 200);
    addRawMeasure(ROOT_REF, NCLOC_KEY, 200);

    underTest.execute();

    assertNoRawMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void aggregate_public_api() {
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_API_KEY, 100);
    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_API_KEY, 400);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, PUBLIC_API_KEY, 500);
    assertRawMeasureValue(MODULE_REF, PUBLIC_API_KEY, 500);
    assertRawMeasureValue(ROOT_REF, PUBLIC_API_KEY, 500);
  }

  @Test
  public void aggregate_public_undocumented_api() {
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, 100);
    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, 400);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, PUBLIC_UNDOCUMENTED_API_KEY, 500);
    assertRawMeasureValue(MODULE_REF, PUBLIC_UNDOCUMENTED_API_KEY, 500);
    assertRawMeasureValue(ROOT_REF, PUBLIC_UNDOCUMENTED_API_KEY, 500);

  }

  @Test
  public void compute_public_documented_api_density() {
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_API_KEY, 100);
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, 50);

    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_API_KEY, 400);
    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, 100);

    addRawMeasure(PROJECTVIEW_3_REF, PUBLIC_API_KEY, 300);
    addRawMeasure(PROJECTVIEW_3_REF, PUBLIC_UNDOCUMENTED_API_KEY, 200);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY, 70d);
    assertRawMeasureValue(MODULE_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY, 70d);
    assertRawMeasureValue(ROOT_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY, 56.3d);
  }

  @Test
  public void not_compute_public_documented_api_density_when_no_public_api() {
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, 50);
    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, 100);

    underTest.execute();

    assertNoRawMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void not_compute_public_documented_api_density_when_no_public_undocumented_api() {
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_API_KEY, 50);
    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_API_KEY, 100);

    underTest.execute();

    assertNoRawMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void not_compute_public_documented_api_density_when_public_api_is_zero() {
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_API_KEY, 0);
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, 50);

    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_API_KEY, 0);
    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, 100);

    underTest.execute();

    assertNoRawMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void compute_100_percent_public_documented_api_density_when_public_undocumented_api_is_zero() {
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_API_KEY, 100);
    addRawMeasure(PROJECTVIEW_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, 0);

    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_API_KEY, 400);
    addRawMeasure(PROJECTVIEW_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, 0);

    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertRawMeasureValue(SUB_MODULE_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY, 100d);
    assertRawMeasureValue(MODULE_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY, 100d);
    assertRawMeasureValue(ROOT_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY, 100d);
  }

  @Test
  public void compute_nothing_when_no_data() {
    underTest.execute();

    assertProjectViewsHasNoNewRawMeasure();
    assertThat(measureRepository.getAddedRawMeasures(SUB_MODULE_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(MODULE_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF)).isEmpty();
  }

  private void assertNoRawMeasures(String metricKey) {
    assertNoRawMeasure(metricKey, SUB_MODULE_REF);
    assertNoRawMeasure(metricKey, MODULE_REF);
    assertNoRawMeasure(metricKey, ROOT_REF);
  }

  private void assertNoRawMeasure(String metricKey, int componentRef) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey)).isAbsent();
  }

  private void addRawMeasure(int componentRef, String metricKey, int value) {
    measureRepository.addRawMeasure(componentRef, metricKey, newMeasureBuilder().create(value));
  }

  private void assertProjectViewsHasNoNewRawMeasure() {
    assertThat(measureRepository.getAddedRawMeasures(PROJECTVIEW_1_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(PROJECTVIEW_2_REF)).isEmpty();
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, int value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getIntValue()).isEqualTo(value);
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, double value) {
    assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getDoubleValue()).isEqualTo(value);
  }

}
