/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.DIRECTORIES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class FileAndDirectoryMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_REF = 123;
  private static final int DIRECTORY_1_REF = 1234;
  private static final int DIRECTORY_2_REF = 1235;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12343;
  private static final int FILE_3_REF = 12351;
  private static final int UNIT_TEST_REF = 12352;
  public static final String LANGUAGE_KEY_DOES_NOT_MATTER_HERE = null;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.FILES)
    .add(CoreMetrics.DIRECTORIES);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private FileAndDirectoryMeasuresStep underTest = new FileAndDirectoryMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  public void verify_FILE_and_DIRECTORY_computation_and_aggregation() {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(MODULE, SUB_MODULE_REF)
                .addChildren(
                  builder(DIRECTORY, DIRECTORY_1_REF)
                    .addChildren(
                      builder(FILE, FILE_1_REF).build(),
                      builder(FILE, FILE_2_REF).build())
                    .build(),
                  builder(DIRECTORY, DIRECTORY_2_REF)
                    .addChildren(
                      builder(FILE, FILE_3_REF).build(),
                      builder(FILE, UNIT_TEST_REF).setFileAttributes(new FileAttributes(true, LANGUAGE_KEY_DOES_NOT_MATTER_HERE)).build())
                    .build())
                .build())
            .build())
        .build());

    underTest.execute();

    verifyMeasuresOnFile(FILE_1_REF, 1);
    verifyMeasuresOnFile(FILE_2_REF, 1);
    verifyMeasuresOnFile(FILE_3_REF, 1);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(UNIT_TEST_REF))).isEmpty();
    verifyMeasuresOnOtherComponent(DIRECTORY_1_REF, 2, 1);
    verifyMeasuresOnOtherComponent(DIRECTORY_2_REF, 1, 1);
    verifyMeasuresOnOtherComponent(SUB_MODULE_REF, 3, 2);
    verifyMeasuresOnOtherComponent(MODULE_REF, 3, 2);
    verifyMeasuresOnOtherComponent(ROOT_REF, 3, 2);
  }

  private void verifyMeasuresOnFile(int componentRef, int fileCount) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(entryOf(FILES_KEY, newMeasureBuilder().create(fileCount)));
  }

  private void verifyMeasuresOnOtherComponent(int componentRef, int fileCount, int directoryCount) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef)))
      .containsOnly(entryOf(FILES_KEY, newMeasureBuilder().create(fileCount)), entryOf(DIRECTORIES_KEY, newMeasureBuilder().create(directoryCount)));
  }
}
