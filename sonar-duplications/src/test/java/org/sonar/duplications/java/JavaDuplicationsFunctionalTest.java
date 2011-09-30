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
package org.sonar.duplications.java;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.duplications.detector.original.OriginalCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.MemoryCloneIndex;
import org.sonar.duplications.statement.Statement;
import org.sonar.duplications.statement.StatementChunker;
import org.sonar.duplications.token.TokenChunker;

import com.google.common.base.Joiner;

public class JavaDuplicationsFunctionalTest {

  private static final int BLOCK_SIZE = 1;

  private TokenChunker tokenChunker = JavaTokenProducer.build();
  private StatementChunker statementChunker = JavaStatementBuilder.build();
  private BlockChunker blockChunker = new BlockChunker(BLOCK_SIZE);

  private List<CloneGroup> detect(String... lines) {
    String sourceCode = Joiner.on('\n').join(lines);
    MemoryCloneIndex index = new MemoryCloneIndex();
    List<Statement> statements = statementChunker.chunk(tokenChunker.chunk(sourceCode)); 
    List<Block> blocks = blockChunker.chunk("resourceId", statements);
    for (Block block : blocks) {
      index.insert(block);
    }
    return OriginalCloneDetectionAlgorithm.detect(index, blocks);
  }

  /**
   * See SONAR-2837
   */
  @Test
  public void initializationOfMultidimensionalArray() {
    List<CloneGroup> duplications = detect("int[][] idx = new int[][] { { 1, 2 }, { 3, 4 } };");
    assertThat(duplications.size(), is(0));
  }

  /**
   * See SONAR-2782
   */
  @Test
  public void chainOfCases() {
    List<CloneGroup> duplications = detect(
        "switch (a) {",
        "  case 'a': case 'b': case 'c':",
        "    doSomething();",
        "  case 'd': case 'e': case 'f':",
        "    doSomethingElse();",
        "}");
    assertThat(duplications.size(), is(0));
  }

  @Test
  public void literalsNormalization() {
    List<CloneGroup> duplications = detect(
        "String s = \"abc\";",
        "String s = \"def\";");
    assertThat(duplications.size(), is(1));

    duplications = detect(
        "int i = 1;",
        "int i = 2;");
    assertThat(duplications.size(), is(1));
  }

}
