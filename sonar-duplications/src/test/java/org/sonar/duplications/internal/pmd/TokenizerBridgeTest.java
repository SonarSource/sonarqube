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

import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.junit.Test;
import org.sonar.duplications.statement.Statement;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TokenizerBridgeTest {

  @Test
  public void test() {
    Tokenizer tokenizer = new Tokenizer() {
      public void tokenize(SourceCode tokens, Tokens tokenEntries) throws IOException {
        tokenEntries.add(new TokenEntry("t1", "src", 1));
        tokenEntries.add(new TokenEntry("t2", "src", 1));
        tokenEntries.add(new TokenEntry("t3", "src", 2));
        tokenEntries.add(new TokenEntry("t1", "src", 4));
        tokenEntries.add(new TokenEntry("t3", "src", 4));
        tokenEntries.add(TokenEntry.getEOF());
      }
    };

    TokenizerBridge bridge = new TokenizerBridge(tokenizer, "UTF-8");
    List<Statement> statements = bridge.tokenize(null);
    bridge.clearCache();

    assertThat(statements.size(), is(3));

    Statement statement = statements.get(0);
    assertThat(statement.getStartLine(), is(1));
    assertThat(statement.getEndLine(), is(1));
    assertThat(statement.getValue(), is("t1t2"));

    statement = statements.get(1);
    assertThat(statement.getStartLine(), is(2));
    assertThat(statement.getEndLine(), is(2));
    assertThat(statement.getValue(), is("t3"));

    statement = statements.get(2);
    assertThat(statement.getStartLine(), is(4));
    assertThat(statement.getEndLine(), is(4));
    assertThat(statement.getValue(), is("t1t3"));
  }

}
