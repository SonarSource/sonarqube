/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.duplications.index;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

public class MemoryCloneIndexTest {

  private CloneIndex cloneIndex;

  @Before
  public void initialize() {
    cloneIndex = new MemoryCloneIndex();
  }

  @Test
  public void byFileName() {
    Block tuple1 = new Block("a", new ByteArray(0), 0, 0, 10);
    Block tuple2 = new Block("a", new ByteArray(0), 1, 10, 20);

    assertThat(cloneIndex.getByResourceId("a").size(), is(0));

    cloneIndex.insert(tuple1);
    assertThat(cloneIndex.getByResourceId("a").size(), is(1));

    cloneIndex.insert(tuple2);
    assertThat(cloneIndex.getByResourceId("a").size(), is(2));
  }

  @Test
  public void bySequenceHash() {
    Block tuple1 = new Block("a", new ByteArray(0), 0, 0, 5);
    Block tuple2 = new Block("a", new ByteArray(0), 1, 1, 6);

    assertThat(cloneIndex.getBySequenceHash(new ByteArray(0)).size(), is(0));

    cloneIndex.insert(tuple1);
    assertThat(cloneIndex.getBySequenceHash(new ByteArray(0)).size(), is(1));

    cloneIndex.insert(tuple2);
    assertThat(cloneIndex.getBySequenceHash(new ByteArray(0)).size(), is(2));
  }

  @Test
  public void insertSame() {
    Block tuple = new Block("a", new ByteArray(0), 0, 0, 5);
    Block tupleSame = new Block("a", new ByteArray(0), 0, 0, 5);

    assertThat(cloneIndex.getByResourceId("a").size(), is(0));
    assertThat(cloneIndex.getBySequenceHash(new ByteArray(0)).size(), is(0));

    cloneIndex.insert(tuple);
    assertThat(cloneIndex.getByResourceId("a").size(), is(1));
    assertThat(cloneIndex.getBySequenceHash(new ByteArray(0)).size(), is(1));

    cloneIndex.insert(tupleSame);
    assertThat(cloneIndex.getByResourceId("a").size(), is(1));
    assertThat(cloneIndex.getBySequenceHash(new ByteArray(0)).size(), is(1));
  }

  @Test
  public void testSorted() {
    for (int i = 0; i < 10; i++) {
      cloneIndex.insert(new Block("a", new ByteArray(1), 10 - i, i, i + 5));
    }
    assertThat(cloneIndex.getByResourceId("a").size(), is(10));
    assertThat(cloneIndex.getBySequenceHash(new ByteArray(1)).size(), is(10));

    Collection<Block> set = cloneIndex.getByResourceId("a");
    int prevStatementIndex = 0;
    for (Block tuple : set) {
      assertTrue(tuple.getIndexInFile() > prevStatementIndex);
      prevStatementIndex = tuple.getIndexInFile();
    }
  }
}
