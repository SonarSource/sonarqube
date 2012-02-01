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
package org.sonar.duplications.internal.pmd;

import com.google.common.collect.Lists;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

import java.util.Collections;
import java.util.List;

/**
 * Differences with {@link org.sonar.duplications.block.BlockChunker}:
 * works with {@link TokensLine},
 * sets {@link Block#setStartUnit(int)} and {@link Block#setEndUnit(int)} - indexes of first and last token for this block.
 */
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

  public List<Block> chunk(String resourceId, List<TokensLine> fragments) {
    if (fragments.size() < blockSize) {
      return Collections.emptyList();
    }
    TokensLine[] fragmentsArr = fragments.toArray(new TokensLine[fragments.size()]);
    List<Block> blocks = Lists.newArrayListWithCapacity(fragmentsArr.length - blockSize + 1);
    long hash = 0;
    int first = 0;
    int last = 0;
    for (; last < blockSize - 1; last++) {
      hash = hash * PRIME_BASE + fragmentsArr[last].getHashCode();
    }
    for (; last < fragmentsArr.length; last++, first++) {
      TokensLine firstFragment = fragmentsArr[first];
      TokensLine lastFragment = fragmentsArr[last];
      // add last statement to hash
      hash = hash * PRIME_BASE + lastFragment.getHashCode();
      // create block
      Block block = new Block(resourceId, new ByteArray(hash), first, firstFragment.getStartLine(), lastFragment.getEndLine());
      block.setStartUnit(firstFragment.getStartUnit());
      block.setEndUnit(lastFragment.getEndUnit());
      blocks.add(block);
      // remove first statement from hash
      hash -= power * firstFragment.getHashCode();
    }
    return blocks;
  }

}
