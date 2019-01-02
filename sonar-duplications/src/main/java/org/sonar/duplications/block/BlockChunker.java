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

import java.util.ArrayList;
import org.sonar.duplications.statement.Statement;

import java.util.Collections;
import java.util.List;

/**
 * Creates blocks from statements, each block will contain specified number of statements (<code>blockSize</code>) and 64-bits (8-bytes) hash value.
 * Hash value computed using
 * <a href="http://en.wikipedia.org/wiki/Rolling_hash#Rabin-Karp_rolling_hash">Rabin-Karp rolling hash</a> :
 * <blockquote><pre>
 * s[0]*31^(blockSize-1) + s[1]*31^(blockSize-2) + ... + s[blockSize-1]
 * </pre></blockquote>
 * using <code>long</code> arithmetic, where <code>s[i]</code>
 * is the hash code of <code>String</code> (which is cached) for statement with number i.
 * Thus running time - O(N), where N - number of statements.
 * Implementation fully thread-safe.
 */
public class BlockChunker {

  private static final long PRIME_BASE = 31;

  private final int blockSize;
  private final long power;

  public BlockChunker(int blockSize) {
    this.blockSize = blockSize;

    long pow = 1;
    for (int i = 0; i < blockSize - 1; i++) {
      pow = pow * PRIME_BASE;
    }
    this.power = pow;
  }

  public List<Block> chunk(String resourceId, List<Statement> statements) {
    List<Statement> filtered = new ArrayList<>();
    int i = 0;
    while (i < statements.size()) {
      Statement first = statements.get(i);
      int j = i + 1;
      while (j < statements.size() && statements.get(j).getValue().equals(first.getValue())) {
        j++;
      }
      filtered.add(statements.get(i));
      if (i < j - 1) {
        filtered.add(statements.get(j - 1));
      }
      i = j;
    }
    statements = filtered;

    if (statements.size() < blockSize) {
      return Collections.emptyList();
    }
    Statement[] statementsArr = statements.toArray(new Statement[statements.size()]);
    List<Block> blocks = new ArrayList<>(statementsArr.length - blockSize + 1);
    long hash = 0;
    int first = 0;
    int last = 0;
    for (; last < blockSize - 1; last++) {
      hash = hash * PRIME_BASE + statementsArr[last].getValue().hashCode();
    }
    Block.Builder blockBuilder = Block.builder().setResourceId(resourceId);
    for (; last < statementsArr.length; last++, first++) {
      Statement firstStatement = statementsArr[first];
      Statement lastStatement = statementsArr[last];
      // add last statement to hash
      hash = hash * PRIME_BASE + lastStatement.getValue().hashCode();
      // create block
      Block block = blockBuilder.setBlockHash(new ByteArray(hash))
          .setIndexInFile(first)
          .setLines(firstStatement.getStartLine(), lastStatement.getEndLine())
          .build();
      blocks.add(block);
      // remove first statement from hash
      hash -= power * firstStatement.getValue().hashCode();
    }
    return blocks;
  }

  public int getBlockSize() {
    return blockSize;
  }

}
