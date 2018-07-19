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
package org.sonar.duplications.internal.pmd;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.cpd.FileCodeLoaderWithoutCache;

/**
 * Bridge, which allows to convert list of {@link TokenEntry} produced by {@link Tokenizer} into list of {@link TokensLine}s.
 */
public class TokenizerBridge {

  private final Tokenizer tokenizer;
  private final PmdBlockChunker blockBuilder;

  public TokenizerBridge(Tokenizer tokenizer, int blockSize) {
    this.tokenizer = tokenizer;
    this.blockBuilder = new PmdBlockChunker(blockSize);
  }

  public List<Block> chunk(String resourceId, String fileName, Reader fileReader) {
    return blockBuilder.chunk(resourceId, chunk(fileName, fileReader));
  }

  public List<TokensLine> chunk(String fileName, Reader fileReader) {
    SourceCode sourceCode = new SourceCode(new FileCodeLoaderWithoutCache(fileName, fileReader));
    Tokens tokens = new Tokens();
    TokenEntry.clearImages();
    try {
      tokenizer.tokenize(sourceCode, tokens);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    TokenEntry.clearImages();
    return convert(tokens.getTokens());
  }

  /**
   * We expect that implementation of {@link Tokenizer} is correct:
   * tokens ordered by occurrence in source code and last token is EOF.
   */
  public static List<TokensLine> convert(List<TokenEntry> tokens) {
    List<TokensLine> result = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    int startLine = Integer.MIN_VALUE;
    int startIndex = 0;
    int currentIndex = 0;
    for (TokenEntry token : tokens) {
      if (token != TokenEntry.EOF) {
        String value = token.getValue();
        int line = token.getBeginLine();
        if (line != startLine) {
          addNewTokensLine(result, startIndex, currentIndex, startLine, sb);
          startIndex = currentIndex + 1;
          startLine = line;
        }
        currentIndex++;
        sb.append(value);
      }
    }
    addNewTokensLine(result, startIndex, currentIndex, startLine, sb);
    return result;
  }

  private static void addNewTokensLine(List<TokensLine> result, int startUnit, int endUnit, int startLine, StringBuilder sb) {
    if (sb.length() != 0) {
      result.add(new TokensLine(startUnit, endUnit, startLine, sb.toString()));
      sb.setLength(0);
    }
  }

}
