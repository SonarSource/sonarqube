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
package org.sonar.db.duplication;

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Category(DbTests.class)
public class DuplicationDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DuplicationDao dao = db.getDbClient().duplicationDao();

  @Test
  public void shouldGetByHash() {
    db.prepareDbUnit(getClass(), "shouldGetByHash.xml");

    List<DuplicationUnitDto> blocks = dao.selectCandidates(10, 7, "java");
    assertThat(blocks.size(), is(1));

    DuplicationUnitDto block = blocks.get(0);
    assertThat("block resourceId", block.getResourceKey(), is("bar-last"));
    assertThat("block hash", block.getHash(), is("aa"));
    assertThat("block index in file", block.getIndexInFile(), is(0));
    assertThat("block start line", block.getStartLine(), is(1));
    assertThat("block end line", block.getEndLine(), is(2));

    // check null for lastSnapshotId
    blocks = dao.selectCandidates(10, null, "java");
    assertThat(blocks.size(), is(2));
  }

  @Test
  public void shouldInsert() {
    db.prepareDbUnit(getClass(), "shouldInsert.xml");

    dao.insert(Arrays.asList(new DuplicationUnitDto(1, 2, "bb", 0, 1, 2)));

    db.assertDbUnit(getClass(), "shouldInsert-result.xml", "duplications_index");
  }

}
