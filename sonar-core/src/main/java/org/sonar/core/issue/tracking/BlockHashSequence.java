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
package org.sonar.core.issue.tracking;

import java.util.List;
import javax.annotation.Nullable;

public class BlockHashSequence {

  public static final int DEFAULT_HALF_BLOCK_SIZE = 5;
  /**
   * Hashes of blocks around lines. Line 1 is at index 0.
   */
  private final int[] blockHashes;

  BlockHashSequence(LineHashSequence lineHashSequence, int halfBlockSize) {
    this.blockHashes = new int[lineHashSequence.length()];

    BlockHashFactory blockHashFactory = new BlockHashFactory(lineHashSequence.getHashes(), halfBlockSize);
    for (int line = 1; line <= lineHashSequence.length(); line++) {
      blockHashes[line - 1] = blockHashFactory.getHash();
      if (line - halfBlockSize > 0) {
        blockHashFactory.remove(lineHashSequence.getHashForLine(line - halfBlockSize).hashCode());
      }
      if (line + 1 + halfBlockSize <= lineHashSequence.length()) {
        blockHashFactory.add(lineHashSequence.getHashForLine(line + 1 + halfBlockSize).hashCode());
      } else {
        blockHashFactory.add(0);
      }
    }
  }

  public static BlockHashSequence create(LineHashSequence lineHashSequence) {
    return new BlockHashSequence(lineHashSequence, DEFAULT_HALF_BLOCK_SIZE);
  }

  /**
   * Hash of block around line. Line must be in range of valid lines. It starts with 1.
   */
  public int getBlockHashForLine(int line) {
    return blockHashes[line - 1];
  }

  public boolean hasLine(@Nullable Integer line) {
    return (line != null) && (line > 0) && (line <= blockHashes.length);
  }

  private static class BlockHashFactory {
    private static final int PRIME_BASE = 31;

    private final int power;
    private int hash = 0;

    public BlockHashFactory(List<String> hashes, int halfBlockSize) {
      int pow = 1;
      for (int i = 0; i < halfBlockSize * 2; i++) {
        pow = pow * PRIME_BASE;
      }
      this.power = pow;
      for (int i = 1; i <= Math.min(hashes.size(), halfBlockSize + 1); i++) {
        add(hashes.get(i - 1).hashCode());
      }
    }

    public void add(int value) {
      hash = hash * PRIME_BASE + value;
    }

    public void remove(int value) {
      hash = hash - power * value;
    }

    public int getHash() {
      return hash;
    }

  }
}
