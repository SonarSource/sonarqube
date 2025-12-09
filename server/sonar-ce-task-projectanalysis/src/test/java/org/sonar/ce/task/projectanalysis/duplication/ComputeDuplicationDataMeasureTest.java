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
package org.sonar.ce.task.projectanalysis.duplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

class ComputeDuplicationDataMeasureTest {

  private static final int ROOT_REF = 1;
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String PROJECT_UUID = "u1";

  private static final int FILE_1_REF = 2;
  private static final String FILE_1_KEY = "FILE_1_KEY";
  private static final String FILE_1_UUID = "u2";

  private static final int FILE_2_REF = 3;
  private static final String FILE_2_KEY = "FILE_2_KEY";
  private static final String FILE_2_UUID = "u3";

  @RegisterExtension
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final Component file1 = builder(FILE, FILE_1_REF).setKey(FILE_1_KEY).setUuid(FILE_1_UUID).build();
  private final Component file2 = builder(FILE, FILE_2_REF).setKey(FILE_2_KEY).setUuid(FILE_2_UUID).build();

  @RegisterExtension
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF).setKey(PROJECT_KEY).setUuid(PROJECT_UUID)
        .addChildren(file1, file2)
        .build());

  @RegisterExtension
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);

  @Test
  void nothing_to_persist_when_no_duplication() {
    assertThat(underTest().compute(file1)).isEmpty();
  }

  @Test
  void compute_duplications_on_same_file() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 5), new TextBlock(6, 10));

    assertThat(underTest().compute(file1))
      .contains("<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"false\" r=\""
        + FILE_1_KEY + "\"/></g></duplications>");
  }

  @Test
  void compute_duplications_on_different_files() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 5), FILE_2_REF, new TextBlock(6, 10));

    assertThat(underTest().compute(file1))
      .contains("<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"false\" r=\""
        + FILE_2_KEY + "\"/></g></duplications>");
    assertThat(underTest().compute(file2)).isEmpty();
  }

  @Test
  void compute_duplications_on_unchanged_file() {
    duplicationRepository.addExtendedProjectDuplication(FILE_1_REF, new TextBlock(1, 5), FILE_2_REF, new TextBlock(6, 10));

    assertThat(underTest().compute(file1))
      .contains("<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"true\" r=\""
        + FILE_2_KEY + "\"/></g></duplications>");
    assertThat(underTest().compute(file2)).isEmpty();
  }

  @Test
  void compute_duplications_on_different_projects() {
    String fileKeyFromOtherProject = "PROJECT2_KEY:file2";
    duplicationRepository.addCrossProjectDuplication(FILE_1_REF, new TextBlock(1, 5), fileKeyFromOtherProject, new TextBlock(6, 10));

    assertThat(underTest().compute(file1))
      .contains("<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"false\" r=\""
        + fileKeyFromOtherProject + "\"/></g></duplications>");
    assertThat(underTest().compute(file2)).isEmpty();
  }

  private ComputeDuplicationDataMeasure underTest() {
    return new ComputeDuplicationDataMeasure(duplicationRepository);
  }
}
