/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.duplication;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

class DuplicationDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final DuplicationDao dao = db.getDbClient().duplicationDao();

  @Test
  void selectCandidates_returns_block_from_last_snapshot_only_of_component_with_language_and_if_not_specified_analysis() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto fooFile = db.components().insertComponent(ComponentTesting.newFileDto(project1).setLanguage("foo").setEnabled(true));
    ComponentDto fooFile1 = db.components().insertComponent(ComponentTesting.newFileDto(project1).setLanguage("foo").setEnabled(true));
    ComponentDto disabledFooFile =
      db.components().insertComponent(ComponentTesting.newFileDto(project1).setLanguage("foo").setEnabled(false));
    ComponentDto barFile = db.components().insertComponent(ComponentTesting.newFileDto(project1).setLanguage("bar").setEnabled(true));
    ComponentDto noLanguageFile = db.components().insertComponent(ComponentTesting.newFileDto(project1).setLanguage(null).setEnabled(true));
    SnapshotDto newAnalysis = db.components().insertSnapshot(project1, t -> t.setLast(false));
    SnapshotDto lastAnalysis = db.components().insertSnapshot(project1, t -> t.setLast(true));
    SnapshotDto notLastAnalysis = db.components().insertSnapshot(project1, t -> t.setLast(false));
    for (String hash : Arrays.asList("aa", "bb")) {
      for (ComponentDto component : Arrays.asList(project1, fooFile, fooFile1, disabledFooFile, barFile, noLanguageFile)) {
        insert(component, lastAnalysis, hash, 0, 1, 2);
        insert(component, notLastAnalysis, hash, 0, 4, 5);
        insert(component, newAnalysis, hash, 0, 6, 7);
      }
    }

    for (String hash : Arrays.asList("aa", "bb")) {
      assertThat(dao.selectCandidates(dbSession, newAnalysis.getUuid(), "foo", singletonList(hash)))
        .containsOnly(
          tuple(fooFile.uuid(), fooFile.getKey(), lastAnalysis.getUuid(), hash),
          tuple(fooFile1.uuid(), fooFile1.getKey(), lastAnalysis.getUuid(), hash));
      assertThat(dao.selectCandidates(dbSession, newAnalysis.getUuid(), "bar", singletonList(hash)))
        .containsOnly(
          tuple(barFile.uuid(), barFile.getKey(), lastAnalysis.getUuid(), hash));
      assertThat(dao.selectCandidates(dbSession, newAnalysis.getUuid(), "donut", singletonList(hash)))
        .isEmpty();
    }
    for (List<String> hashes : Arrays.asList(List.of("aa", "bb"), List.of("bb", "aa"), List.of("aa", "bb", "cc"))) {
      assertThat(dao.selectCandidates(dbSession, newAnalysis.getUuid(), "foo", hashes))
        .containsOnly(
          tuple(fooFile.uuid(), fooFile.getKey(), lastAnalysis.getUuid(), "aa"),
          tuple(fooFile.uuid(), fooFile.getKey(), lastAnalysis.getUuid(), "bb"),
          tuple(fooFile1.uuid(), fooFile1.getKey(), lastAnalysis.getUuid(), "aa"),
          tuple(fooFile1.uuid(), fooFile1.getKey(), lastAnalysis.getUuid(), "bb"));
      assertThat(dao.selectCandidates(dbSession, newAnalysis.getUuid(), "bar", hashes))
        .containsOnly(
          tuple(barFile.uuid(), barFile.getKey(), lastAnalysis.getUuid(), "aa"),
          tuple(barFile.uuid(), barFile.getKey(), lastAnalysis.getUuid(), "bb"));
      assertThat(dao.selectCandidates(dbSession, newAnalysis.getUuid(), "donut", hashes))
        .isEmpty();
    }

    assertThat(dao.selectCandidates(dbSession, lastAnalysis.getUuid(), "foo", singletonList("aa")))
      .isEmpty();
    assertThat(dao.selectCandidates(dbSession, lastAnalysis.getUuid(), "bar", singletonList("aa")))
      .isEmpty();
    assertThat(dao.selectCandidates(dbSession, lastAnalysis.getUuid(), "donut", singletonList("aa")))
      .isEmpty();

  }

  private AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThat(List<DuplicationUnitDto> blocks) {
    return Assertions.assertThat(blocks)
      .extracting(DuplicationUnitDto::getComponentUuid, DuplicationUnitDto::getComponentKey, DuplicationUnitDto::getAnalysisUuid,
        DuplicationUnitDto::getHash);
  }

  @Test
  void select_component() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto analysis1 = db.components().insertSnapshot(project1);
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto analysis2 = db.components().insertSnapshot(project2);
    ComponentDto project3 = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto analysis3 = db.components().insertSnapshot(project3);
    ComponentDto project4 = db.components().insertPrivateProject().getMainBranchComponent();
    insert(project1, analysis1, "bb", 0, 0, 0);
    insert(project2, analysis2, "aa", 0, 1, 2);
    insert(project3, analysis3, "bb", 0, 0, 0);
    // irrealistic case but allow to test the SQL code
    insert(project4, analysis3, "aa", 0, 0, 0);

    List<DuplicationUnitDto> blocks = dao.selectComponent(dbSession, project3.uuid(), analysis3.getUuid());
    assertThat(blocks).hasSize(1);

    DuplicationUnitDto block = blocks.get(0);
    Assertions.assertThat(block.getUuid()).isNotNull();
    Assertions.assertThat(block.getComponentKey()).isNull();
    Assertions.assertThat(block.getComponentUuid()).isEqualTo(project3.uuid());
    Assertions.assertThat(block.getHash()).isEqualTo("bb");
    Assertions.assertThat(block.getAnalysisUuid()).isEqualTo(analysis3.getUuid());
    Assertions.assertThat(block.getIndexInFile()).isZero();
    Assertions.assertThat(block.getStartLine()).isZero();
    Assertions.assertThat(block.getEndLine()).isZero();
  }

  @Test
  void insert() {
    ComponentDto project = newPrivateProjectDto();
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);

    insert(project, analysis, "bb", 0, 1, 2);

    List<Map<String, Object>> rows = db.select("select " +
      "uuid as \"UUID\", analysis_uuid as \"ANALYSIS\", component_uuid as \"COMPONENT\", hash as \"HASH\", " +
      "index_in_file as \"INDEX\", start_line as \"START\", end_line as \"END\"" +
      " from duplications_index");
    Assertions.assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    Assertions.assertThat(row.get("UUID")).isNotNull();
    Assertions.assertThat(row)
      .containsEntry("ANALYSIS", analysis.getUuid())
      .containsEntry("COMPONENT", project.uuid())
      .containsEntry("HASH", "bb")
      .containsEntry("INDEX", 0L)
      .containsEntry("START", 1L)
      .containsEntry("END", 2L);
  }

  private void insert(ComponentDto project, SnapshotDto analysis, String hash, int indexInFile, int startLine, int endLine) {
    dao.insert(dbSession, new DuplicationUnitDto()
      .setAnalysisUuid(analysis.getUuid())
      .setComponentUuid(project.uuid())
      .setHash(hash)
      .setIndexInFile(indexInFile)
      .setStartLine(startLine)
      .setEndLine(endLine));
    dbSession.commit();
  }

}
