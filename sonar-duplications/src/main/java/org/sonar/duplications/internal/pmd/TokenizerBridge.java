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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.sonar.duplications.cpd.FileCodeLoaderWithoutCache;
import org.sonar.duplications.statement.Statement;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Bridge, which allows to convert list of {@link TokenEntry} produced by {@link Tokenizer} into list of {@link Statement}s.
 * Principle of conversion - statement formed from tokens of one line.
 */
public class TokenizerBridge {

  private final Tokenizer tokenizer;
  private final String encoding;

  public TokenizerBridge(Tokenizer tokenizer, String encoding) {
    this.tokenizer = tokenizer;
    this.encoding = encoding;
    clearCache();
  }

  public List<Statement> tokenize(File file) {
    SourceCode sourceCode = new SourceCode(new FileCodeLoaderWithoutCache(file, encoding));
    Tokens tokens = new Tokens();
    try {
      tokenizer.tokenize(sourceCode, tokens);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    return convert(tokens.getTokens());
  }

  /**
   * We expect that implementation of {@link Tokenizer} is correct:
   * tokens ordered by occurrence in source code and last token is EOF.
   */
  private static List<Statement> convert(List<TokenEntry> tokens) {
    ImmutableList.Builder<Statement> result = ImmutableList.builder();
    int currentLine = Integer.MIN_VALUE;
    StringBuilder sb = new StringBuilder();
    for (TokenEntry token : tokens) {
      if (token != TokenEntry.EOF) {
        String value = token.getValue();
        int line = token.getBeginLine();
        if (line != currentLine) {
          addNewStatement(result, currentLine, sb);
          currentLine = line;
        }
        sb.append(value);
      }
    }
    addNewStatement(result, currentLine, sb);
    return result.build();
  }

  private static void addNewStatement(ImmutableList.Builder<Statement> result, int line, StringBuilder sb) {
    if (sb.length() != 0) {
      result.add(new Statement(line, line, sb.toString()));
      sb.setLength(0);
    }
  }

  public void clearCache() {
    TokenEntry.clearImages();
  }

}
