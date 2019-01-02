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
package org.sonar.db.duplication;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  final DbSession dbSession = db.getSession();

  DuplicationDao dao = db.getDbClient().duplicationDao();

  @Test
  public void select_candidates() {
    db.prepareDbUnit(getClass(), "select_candidates.xml");
    dbSession.commit();

    List<DuplicationUnitDto> blocks = dao.selectCandidates(dbSession, "u7", "java", singletonList("aa"));
    assertThat(blocks).hasSize(1);

    DuplicationUnitDto block = blocks.get(0);
    assertThat(block.getComponentKey()).isEqualTo("bar-last");
    assertThat(block.getComponentUuid()).isEqualTo("uuid_2");
    assertThat(block.getHash()).isEqualTo("aa");
    assertThat(block.getIndexInFile()).isEqualTo(0);
    assertThat(block.getStartLine()).isEqualTo(1);
    assertThat(block.getEndLine()).isEqualTo(2);

    // check null for lastSnapshotId
    blocks = dao.selectCandidates(dbSession, null, "java", singletonList("aa"));
    assertThat(blocks).hasSize(2);
  }

  @Test
  public void select_component() {
    db.prepareDbUnit(getClass(), "select_component.xml");
    dbSession.commit();

    List<DuplicationUnitDto> blocks = dao.selectComponent(dbSession, "uuid_3", "u5");
    assertThat(blocks).hasSize(1);

    DuplicationUnitDto block = blocks.get(0);
    assertThat(block.getComponentKey()).isNull();
    assertThat(block.getComponentUuid()).isEqualTo("uuid_3");
    assertThat(block.getHash()).isEqualTo("bb");
    assertThat(block.getAnalysisUuid()).isEqualTo("u5");
    assertThat(block.getIndexInFile()).isEqualTo(0);
    assertThat(block.getStartLine()).isEqualTo(0);
    assertThat(block.getEndLine()).isEqualTo(0);

  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "insert.xml");
    dbSession.commit();

    dao.insert(dbSession, new DuplicationUnitDto()
      .setAnalysisUuid("u1")
      .setComponentUuid("uuid_1")
      .setHash("bb")
      .setIndexInFile(0)
      .setStartLine(1)
      .setEndLine(2));
    dbSession.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", "duplications_index");
  }

}
