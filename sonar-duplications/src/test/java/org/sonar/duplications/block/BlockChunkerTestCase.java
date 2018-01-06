/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.duplications.statement.Statement;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * Any implementation of {@link BlockChunker} should pass these test scenarios.
 */
public abstract class BlockChunkerTestCase {

  /**
   * Factory method.
   */
  protected abstract BlockChunker createChunkerWithBlockSize(int blockSize);

  /**
   * Given:
   * <pre>
   * String[][] data = {
   *   {"a", "a"},
   *   {"a", "a"},
   *   {"a"},
   *   {"a", "a"},
   *   {"a", "a"}
   * };
   *
   * Statements (where L - literal, C - comma): "LCL", "C", "LCL", "C", "L", "C", "LCL", "C", "LCL"
   * Block size is 5.
   * First block: "LCL", "C", "LCL", "C", "L"
   * Last block: "L", "C", "LCL", "C", "LCL"
   * </pre>
   * Expected: different hashes for first and last blocks
   */
  @Test
  public void testSameChars() {
    List<Statement> statements = createStatementsFromStrings("LCL", "C", "LCL", "C", "L", "C", "LCL", "C", "LCL");
    BlockChunker chunker = createChunkerWithBlockSize(5);
    List<Block> blocks = chunker.chunk("resource", statements);
    assertThat("first and last block should have different hashes", blocks.get(0).getBlockHash(), not(equalTo(blocks.get(blocks.size() - 1).getBlockHash())));
  }

  /**
   * TODO Godin: should we allow empty statements in general?
   */
  @Test
  public void testEmptyStatements() {
    List<Statement> statements = createStatementsFromStrings("1", "", "1", "1", "");
    BlockChunker chunker = createChunkerWithBlockSize(3);
    List<Block> blocks = chunker.chunk("resource", statements);
    assertThat("first and last block should have different hashes", blocks.get(0).getBlockHash(), not(equalTo(blocks.get(blocks.size() - 1).getBlockHash())));
  }

  /**
   * Given: 5 statements, block size is 3
   * Expected: 4 blocks with correct index and with line numbers
   */
  @Test
  public void shouldBuildBlocksFromStatements() {
    List<Statement> statements = createStatementsFromStrings("1", "2", "3", "4", "5", "6");
    BlockChunker chunker = createChunkerWithBlockSize(3);
    List<Block> blocks = chunker.chunk("resource", statements);
    assertThat(blocks.size(), is(4));
    assertThat(blocks.get(0).getIndexInFile(), is(0));
    assertThat(blocks.get(0).getStartLine(), is(0));
    assertThat(blocks.get(0).getEndLine(), is(2));
    assertThat(blocks.get(1).getIndexInFile(), is(1));
    assertThat(blocks.get(1).getStartLine(), is(1));
    assertThat(blocks.get(1).getEndLine(), is(3));
  }

  @Test
  public void testHashes() {
    List<Statement> statements = createStatementsFromStrings("1", "2", "1", "2");
    BlockChunker chunker = createChunkerWithBlockSize(2);
    List<Block> blocks = chunker.chunk("resource", statements);
    assertThat("blocks 0 and 2 should have same hash", blocks.get(0).getBlockHash(), equalTo(blocks.get(2).getBlockHash()));
    assertThat("blocks 0 and 1 should have different hash", blocks.get(0).getBlockHash(), not(equalTo(blocks.get(1).getBlockHash())));
  }

  /**
   * Given: 0 statements
   * Expected: 0 blocks
   */
  @Test
  public void shouldNotBuildBlocksWhenNoStatements() {
    List<Statement> statements = Collections.emptyList();
    BlockChunker blockChunker = createChunkerWithBlockSize(2);
    List<Block> blocks = blockChunker.chunk("resource", statements);
    assertThat(blocks, sameInstance(Collections.EMPTY_LIST));
  }

  /**
   * Given: 1 statement, block size is 2
   * Expected: 0 blocks
   */
  @Test
  public void shouldNotBuildBlocksWhenNotEnoughStatements() {
    List<Statement> statements = createStatementsFromStrings("statement");
    BlockChunker blockChunker = createChunkerWithBlockSize(2);
    List<Block> blocks = blockChunker.chunk("resource", statements);
    assertThat(blocks, sameInstance(Collections.EMPTY_LIST));
  }

  /**
   * Creates list of statements from Strings, each statement on a new line starting from 0.
   */
  protected static List<Statement> createStatementsFromStrings(String... values) {
    List<Statement> result = new ArrayList<>();
    for (int i = 0; i < values.length; i++) {
      result.add(new Statement(i, i, values[i]));
    }
    return result;
  }

}
