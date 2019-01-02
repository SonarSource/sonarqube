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
package org.sonar.duplications.block;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class BlockTest {

  @Test
  public void testBuilder() {
    ByteArray hash = new ByteArray(1);
    Block block = Block.builder()
        .setResourceId("resource")
        .setBlockHash(hash)
        .setIndexInFile(1)
        .setLines(2, 3)
        .setUnit(4, 5)
        .build();

    assertThat(block.getResourceId(), is("resource"));
    assertThat(block.getBlockHash(), sameInstance(hash));
    assertThat(block.getIndexInFile(), is(1));

    assertThat(block.getStartLine(), is(2));
    assertThat(block.getEndLine(), is(3));

    assertThat(block.getStartUnit(), is(4));
    assertThat(block.getEndUnit(), is(5));
  }

}
