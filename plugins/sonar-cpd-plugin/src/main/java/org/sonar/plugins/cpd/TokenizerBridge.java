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
package org.sonar.plugins.cpd;

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
  private List<Statement> convert(List<TokenEntry> tokens) {
    ImmutableList.Builder<Statement> result = ImmutableList.builder();
    for (TokenEntry token : tokens) {
      if (token != TokenEntry.EOF) {
        int line = token.getBeginLine();
        int id = token.getIdentifier();
        result.add(new Statement(line, line, Integer.toString(id)));
      }
    }
    return result.build();
  }

  public void clearCache() {
    TokenEntry.clearImages();
  }

}
