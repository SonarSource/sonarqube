/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.persistence.dao;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.persistence.model.DuplicationUnit;

import com.google.common.collect.Lists;

public class DuplicationDaoTest extends DaoTestCase {

  private DuplicationDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new DuplicationDao(getMyBatis());
  }

  @Test
  public void shouldGetByHash() throws Exception {
    setupData("shouldGetByHash");

    List<DuplicationUnit> blocks = dao.selectCandidates(10, 7);
    assertThat(blocks.size(), is(1));

    DuplicationUnit block = blocks.get(0);
    assertThat("block resourceId", block.getResourceKey(), is("bar-last"));
    assertThat("block hash", block.getHash(), is("aa"));
    assertThat("block index in file", block.getIndexInFile(), is(0));
    assertThat("block start line", block.getStartLine(), is(1));
    assertThat("block end line", block.getEndLine(), is(2));

    // check null for lastSnapshotId
    blocks = dao.selectCandidates(10, null);
    assertThat(blocks.size(), is(2));
  }

  @Test
  public void shouldInsert() throws Exception {
    setupData("shouldInsert");

    dao.insert(Arrays.asList(new DuplicationUnit(1, 2, "bb", 0, 1, 2)));

    checkTables("shouldInsert", "duplications_index");
  }

  @Test
  public void testBatchInsert() {
    List<DuplicationUnit> duplications = Lists.newArrayList();
    for (int i = 0; i < 50; i++) {
      duplications.add(new DuplicationUnit(i, i, "hash", 2, 30, 40));
    }
    dao.insert(duplications);

    for (DuplicationUnit duplication : duplications) {
      // batch insert : faster but generated ids are not returned
      assertThat(duplication.getId(), nullValue());
    }
  }

}
