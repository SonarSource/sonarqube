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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
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
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class ReportCommentMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12342;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(COMMENT_LINES)
    .add(COMMENT_LINES_DENSITY)
    .add(PUBLIC_API)
    .add(PUBLIC_UNDOCUMENTED_API)
    .add(PUBLIC_DOCUMENTED_API_DENSITY);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputationStep underTest = new CommentMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF)
            .addChildren(
              builder(FILE, FILE_1_REF).build(),
              builder(FILE, FILE_2_REF).build())
            .build())
        .build());
  }

  @Test
  public void aggregate_comment_lines() {
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(400));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, COMMENT_LINES_KEY)).isNotPresent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, COMMENT_LINES_KEY)).isNotPresent();
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY).get().getIntValue()).isEqualTo(500);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, COMMENT_LINES_KEY).get().getIntValue()).isEqualTo(500);
  }

  @Test
  public void compute_comment_density() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(150));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(50));

    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(300));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(60d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(20d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(40d);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(40d);
  }

  @Test
  public void compute_zero_comment_density_when_zero_comment() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));

    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(300));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, COMMENT_LINES_DENSITY_KEY).get().getDoubleValue()).isEqualTo(0d);
  }

  @Test
  public void not_compute_comment_density_when_zero_ncloc_and_zero_comment() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));

    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(0));

    underTest.execute(new TestComputationStepContext());

    assertNoNewMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void not_compute_comment_density_when_no_ncloc() {
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(150));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(50));

    underTest.execute(new TestComputationStepContext());

    assertNoNewMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void not_compute_comment_density_when_no_comment() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(200));

    underTest.execute(new TestComputationStepContext());

    assertNoNewMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void aggregate_public_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(400));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, PUBLIC_API_KEY)).isNotPresent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, PUBLIC_API_KEY)).isNotPresent();
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, PUBLIC_API_KEY).get().getIntValue()).isEqualTo(500);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, PUBLIC_API_KEY).get().getIntValue()).isEqualTo(500);
  }

  @Test
  public void aggregate_public_undocumented_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(400));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY)).isNotPresent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY)).isNotPresent();
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, PUBLIC_UNDOCUMENTED_API_KEY).get().getIntValue()).isEqualTo(500);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, PUBLIC_UNDOCUMENTED_API_KEY).get().getIntValue()).isEqualTo(500);

  }

  @Test
  public void compute_public_documented_api_density() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(50));

    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(400));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(50d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(75d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(70d);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(70d);
  }

  @Test
  public void not_compute_public_documented_api_density_when_no_public_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));

    underTest.execute(new TestComputationStepContext());

    assertNoNewMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void not_compute_public_documented_api_density_when_no_public_undocumented_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));

    underTest.execute(new TestComputationStepContext());

    assertNoNewMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void not_compute_public_documented_api_density_when_public_api_is_zero() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(50));

    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));

    underTest.execute(new TestComputationStepContext());

    assertNoNewMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void compute_100_percent_public_documented_api_density_when_public_undocumented_api_is_zero() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(0));

    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(400));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(0));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(DIRECTORY_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
    assertThat(measureRepository.getAddedRawMeasure(ROOT_REF, PUBLIC_DOCUMENTED_API_DENSITY_KEY).get().getDoubleValue()).isEqualTo(100d);
  }

  @Test
  public void compute_nothing_when_no_data() {
    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasures(FILE_1_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(FILE_2_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(DIRECTORY_REF)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF)).isEmpty();
  }

  private void assertNoNewMeasures(String metric) {
    assertThat(measureRepository.getAddedRawMeasures(FILE_1_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(FILE_2_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(DIRECTORY_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF).get(metric)).isEmpty();
  }
}
