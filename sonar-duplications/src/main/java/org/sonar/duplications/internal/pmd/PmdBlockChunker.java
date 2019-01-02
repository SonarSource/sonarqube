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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.sensor.cpd.internal.TokensLine;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

/**
 * Differences with {@link org.sonar.duplications.block.BlockChunker}:
 * works with {@link TokensLine},
 * sets {@link Block#getStartUnit() startUnit} and {@link Block#getEndUnit() endUnit} - indexes of first and last token for this block.
 */
@Immutable
public class PmdBlockChunker {

  private static final long PRIME_BASE = 31;

  private final int blockSize;
  private final long power;

  public PmdBlockChunker(int blockSize) {
    this.blockSize = blockSize;

    long pow = 1;
    for (int i = 0; i < blockSize - 1; i++) {
      pow = pow * PRIME_BASE;
    }
    this.power = pow;
  }

  /**
   * @return ArrayList as we need a serializable object
   */
  public List<Block> chunk(String resourceId, List<TokensLine> fragments) {
    List<TokensLine> filtered = new ArrayList<>();
    int i = 0;
    while (i < fragments.size()) {
      TokensLine first = fragments.get(i);
      int j = i + 1;
      while (j < fragments.size() && fragments.get(j).getValue().equals(first.getValue())) {
        j++;
      }
      filtered.add(fragments.get(i));
      if (i < j - 1) {
        filtered.add(fragments.get(j - 1));
      }
      i = j;
    }
    fragments = filtered;

    if (fragments.size() < blockSize) {
      return new ArrayList<>();
    }
    TokensLine[] fragmentsArr = fragments.toArray(new TokensLine[fragments.size()]);
    List<Block> blocks = new ArrayList<>(fragmentsArr.length - blockSize + 1);
    long hash = 0;
    int first = 0;
    int last = 0;
    for (; last < blockSize - 1; last++) {
      hash = hash * PRIME_BASE + fragmentsArr[last].getHashCode();
    }
    Block.Builder blockBuilder = Block.builder().setResourceId(resourceId);
    for (; last < fragmentsArr.length; last++, first++) {
      TokensLine firstFragment = fragmentsArr[first];
      TokensLine lastFragment = fragmentsArr[last];
      // add last statement to hash
      hash = hash * PRIME_BASE + lastFragment.getHashCode();
      // create block
      Block block = blockBuilder
        .setBlockHash(new ByteArray(hash))
        .setIndexInFile(first)
        .setLines(firstFragment.getStartLine(), lastFragment.getEndLine())
        .setUnit(firstFragment.getStartUnit(), lastFragment.getEndUnit())
        .build();
      blocks.add(block);
      // remove first statement from hash
      hash -= power * firstFragment.getHashCode();
    }
    return blocks;
  }

}
