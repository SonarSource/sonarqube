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
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.duplication.TextBlock;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA;
import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class DuplicationDataMeasuresStepTest extends BaseStepTest {

  private static final int ROOT_REF = 1;
  private static final String PROJECT_KEY = "PROJECT_KEY";

  private static final int FILE_1_REF = 2;
  private static final String FILE_1_KEY = "FILE_1_KEY";

  private static final int FILE_2_REF = 3;
  private static final String FILE_2_KEY = "FILE_2_KEY";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
    builder(PROJECT, ROOT_REF).setKey(PROJECT_KEY)
      .addChildren(
        builder(FILE, FILE_1_REF).setKey(FILE_1_KEY)
          .build(),
        builder(FILE, FILE_2_REF).setKey(FILE_2_KEY)
          .build())
      .build());

  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(DUPLICATIONS_DATA);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private DuplicationDataMeasuresStep underTest = new DuplicationDataMeasuresStep(treeRootHolder, metricRepository, measureRepository, duplicationRepository);

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void nothing_to_do_when_no_duplication() {
    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, DUPLICATIONS_DATA_KEY)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, DUPLICATIONS_DATA_KEY)).isAbsent();
  }

  @Test
  public void compute_duplications_on_same_file() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 5), new TextBlock(6, 10));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, DUPLICATIONS_DATA_KEY)).isPresent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, DUPLICATIONS_DATA_KEY).get().getData()).isEqualTo(
      "<duplications><g><b s=\"1\" l=\"5\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" r=\"" + FILE_1_KEY + "\"/></g></duplications>"
      );
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, DUPLICATIONS_DATA_KEY)).isAbsent();
  }

  @Test
  public void compute_duplications_on_different_files() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 5), FILE_2_REF, new TextBlock(6, 10));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, DUPLICATIONS_DATA_KEY)).isPresent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, DUPLICATIONS_DATA_KEY).get().getData()).isEqualTo(
      "<duplications><g><b s=\"1\" l=\"5\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" r=\"" + FILE_2_KEY + "\"/></g></duplications>"
      );
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, DUPLICATIONS_DATA_KEY)).isAbsent();
  }

  @Test
  public void compute_duplications_on_different_projects() {
    String fileKeyFromOtherProject = "PROJECT2_KEY:file2";
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 5), fileKeyFromOtherProject, new TextBlock(6, 10));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, DUPLICATIONS_DATA_KEY)).isPresent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, DUPLICATIONS_DATA_KEY).get().getData()).isEqualTo(
      "<duplications><g><b s=\"1\" l=\"5\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" r=\"" + fileKeyFromOtherProject + "\"/></g></duplications>"
      );
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, DUPLICATIONS_DATA_KEY)).isAbsent();
  }

}
