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

package org.sonar.server.design.db;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.design.FileDependencyDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.test.DbTests;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class FileDependencyDaoTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  FileDependencyDao dao;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dao = new FileDependencyDao();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void select_from_parents() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<FileDependencyDto> dtos = dao.selectFromParents(session, "MNOP", "QRST", 1L);
    assertThat(dtos).hasSize(1);

    assertThat(dtos.get(0).getId()).isEqualTo(1);
    assertThat(dtos.get(0).getFromComponentUuid()).isEqualTo("EFGH");
    assertThat(dtos.get(0).getToComponentUuid()).isEqualTo("IJKL");
    assertThat(dtos.get(0).getFromParentUuid()).isEqualTo("MNOP");
    assertThat(dtos.get(0).getToParentUuid()).isEqualTo("QRST");
    assertThat(dtos.get(0).getWeight()).isEqualTo(2);
    assertThat(dtos.get(0).getRootProjectSnapshotId()).isEqualTo(10L);
    assertThat(dtos.get(0).getCreatedAt()).isEqualTo(1000L);

    assertThat(dao.selectFromParents(session, "MNOP", "QRST", 123L)).isEmpty();
  }

  @Test
  public void select_all() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectAll(session)).hasSize(3);
  }

  @Test
  public void insert() throws Exception {
    dao.insert(session, new FileDependencyDto()
      .setFromComponentUuid("ABCD")
      .setToComponentUuid("EFGH")
      .setFromParentUuid("IJKL")
      .setToParentUuid("MNOP")
      .setRootProjectSnapshotId(10L)
      .setWeight(2)
      .setCreatedAt(1000L)
    );
    session.commit();

    dbTester.assertDbUnit(getClass(), "insert.xml", new String[]{"id"}, "dependencies");
  }
}
