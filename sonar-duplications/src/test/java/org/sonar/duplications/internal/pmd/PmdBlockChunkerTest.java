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
package org.sonar.duplications.internal.pmd;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sonar.api.batch.sensor.cpd.internal.TokensLine;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PmdBlockChunkerTest {

  @Test
  public void shouldBuildBlocks() {
    TokensLine line1 = new TokensLine(0, 9, 1, Character.toString((char) 1));
    TokensLine line2 = new TokensLine(10, 19, 2, Character.toString((char) 2));
    TokensLine line3 = new TokensLine(20, 29, 3, Character.toString((char) 3));

    List<Block> blocks = new PmdBlockChunker(2).chunk("resourceId", Arrays.asList(line1, line2, line3));
    assertThat(blocks.size(), is(2));

    Block block = blocks.get(0);
    // assertThat(block.getLengthInUnits(), is(11));
    assertThat(block.getStartLine(), is(1));
    assertThat(block.getEndLine(), is(2));
    assertThat(block.getBlockHash(), is(new ByteArray(1L * 31 + 2)));

    block = blocks.get(1);
    // assertThat(block.getLengthInUnits(), is(33));
    assertThat(block.getStartLine(), is(2));
    assertThat(block.getEndLine(), is(3));
    assertThat(block.getBlockHash(), is(new ByteArray(2L * 31 + 3)));
  }

}
