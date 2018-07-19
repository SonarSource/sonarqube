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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TokenizerBridgeTest {

  private TokenizerBridge bridge;

  @Before
  public void setUp() {
    Tokenizer tokenizer = new Tokenizer() {
      public void tokenize(SourceCode tokens, Tokens tokenEntries) throws IOException {
        tokenEntries.add(new TokenEntry("t1", "src", 1));
        tokenEntries.add(new TokenEntry("t2", "src", 1));
        tokenEntries.add(new TokenEntry("t3", "src", 2));
        tokenEntries.add(new TokenEntry("t1", "src", 4));
        tokenEntries.add(new TokenEntry("t3", "src", 4));
        tokenEntries.add(new TokenEntry("t3", "src", 4));
        tokenEntries.add(TokenEntry.getEOF());
      }
    };
    bridge = new TokenizerBridge(tokenizer, 10);
  }

  @Test
  public void shouldClearCacheInTokenEntry() {
    bridge.chunk("file.txt", new InputStreamReader(new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8));
    TokenEntry token = new TokenEntry("image", "srcId", 0);
    assertThat(token.getIndex(), is(0));
    assertThat(token.getIdentifier(), is(1));
  }

  @Test
  public void test() {
    // To be sure that token index will be relative to file - run twice:
    bridge.chunk("file.txt", new InputStreamReader(new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8));
    List<TokensLine> lines = bridge.chunk("file.txt", new InputStreamReader(new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8));

    assertThat(lines.size(), is(3));

    TokensLine line = lines.get(0);
    // 2 tokens on 1 line
    assertThat(line.getStartUnit(), is(1));
    assertThat(line.getEndUnit(), is(2));

    assertThat(line.getStartLine(), is(1));
    assertThat(line.getEndLine(), is(1));
    assertThat(line.getHashCode(), is("t1t2".hashCode()));

    line = lines.get(1);
    // 1 token on 2 line
    assertThat(line.getStartUnit(), is(3));
    assertThat(line.getEndUnit(), is(3));

    assertThat(line.getStartLine(), is(2));
    assertThat(line.getEndLine(), is(2));
    assertThat(line.getHashCode(), is("t3".hashCode()));

    line = lines.get(2);
    // 3 tokens on 4 line
    assertThat(line.getStartUnit(), is(4));
    assertThat(line.getEndUnit(), is(6));

    assertThat(line.getStartLine(), is(4));
    assertThat(line.getEndLine(), is(4));
    assertThat(line.getHashCode(), is("t1t3t3".hashCode()));
  }

}
