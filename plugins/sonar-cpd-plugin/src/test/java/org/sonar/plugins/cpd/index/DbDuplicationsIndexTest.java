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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class DbDuplicationsIndexTest extends AbstractDbUnitTestCase {

  private DbDuplicationsIndex index;

  @Test
  public void shouldGetByHash() {
    Resource resource = new JavaFile("foo");
    index = spy(new DbDuplicationsIndex(getSession(), null, 9, 7));
    doReturn(10).when(index).getSnapshotIdFor(resource);
    setupData("shouldGetByHash");

    index.prepareCache(resource);
    Collection<Block> blocks = index.getByHash(new ByteArray("aa"));
    Iterator<Block> blocksIterator = blocks.iterator();

    assertThat(blocks.size(), is(1));

    Block block = blocksIterator.next();
    assertThat("block resourceId", block.getResourceId(), is("bar-last"));
    assertThat("block hash", block.getBlockHash(), is(new ByteArray("aa")));
    assertThat("block index in file", block.getIndexInFile(), is(0));
    assertThat("block start line", block.getFirstLineNumber(), is(1));
    assertThat("block end line", block.getLastLineNumber(), is(2));
  }

  @Test
  public void shouldInsert() {
    Resource resource = new JavaFile("foo");
    index = spy(new DbDuplicationsIndex(getSession(), null, 1, null));
    doReturn(2).when(index).getSnapshotIdFor(resource);
    setupData("shouldInsert");

    index.insert(resource, Arrays.asList(new Block("foo", new ByteArray("bb"), 0, 1, 2)));

    checkTables("shouldInsert", "duplications_index");
  }

}
