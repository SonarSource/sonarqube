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
package org.sonar.plugins.cpd.index;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class DbCloneIndexTest extends AbstractDbUnitTestCase {

  private DbCloneIndex index;

  @Before
  public void setUp() {
    index = new DbCloneIndex(getSession(), 5, 4);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldNotGetByResource() {
    index.getByResourceId("foo");
  }

  @Test
  public void shouldGetByHash() {
    setupData("fixture");

    index.prepareCache("foo");
    Collection<Block> blocks = index.getBySequenceHash(new ByteArray("aa"));
    Iterator<Block> blocksIterator = blocks.iterator();

    assertThat(blocks.size(), is(1));

    Block block = blocksIterator.next();
    assertThat(block.getResourceId(), is("bar-last"));
  }

  @Test
  public void shouldInsert() {
    setupData("fixture");

    index.insert(new Block("baz", new ByteArray("bb"), 0, 0, 1));

    checkTables("shouldInsert", "clone_blocks");
  }

}
