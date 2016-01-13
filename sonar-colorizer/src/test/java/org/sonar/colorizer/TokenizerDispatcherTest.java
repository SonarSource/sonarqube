/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.colorizer;

import org.junit.Test;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenizerDispatcherTest {

  @Test
  public void testPipeCodeTokenizer() {
    TokenizerDispatcher colorization = newColorizer();
    assertThat(colorization.colorize("public void get(){")).isEqualTo("public void get(){");
  }

  @Test
  public void testKeywordsCodeTokenizer() {
    TokenizerDispatcher colorization = newColorizer(new KeywordsTokenizer("<k>", "</k>", JavaKeywords.get()));
    assertThat(colorization.colorize("public void get(){")).isEqualTo("<k>public</k> <k>void</k> get(){");
  }

  @Test
  public void testPriorityToComment() {
    TokenizerDispatcher colorization = newColorizer(new CDocTokenizer("<c>", "</c>"), new KeywordsTokenizer("<k>", "</k>", JavaKeywords
      .get()));
    assertThat(colorization.colorize("assert //public void get(){")).isEqualTo("<k>assert</k> <c>//public void get(){</c>");
  }

  @Test
  public void testCommentThenStringThenJavaKeywords() {
    TokenizerDispatcher colorization = newColorizer(new CDocTokenizer("<c>", "</c>"), new LiteralTokenizer("<s>", "</s>"),
      new KeywordsTokenizer("<k>", "</k>", JavaKeywords.get()));
    assertThat(colorization.colorize("assert(\"message\"); //comment")).isEqualTo("<k>assert</k>(<s>\"message\"</s>); <c>//comment</c>");
  }

  @Test(expected = IllegalStateException.class)
  public void testCloneNotThreadSafeTokenizers() {
    NotThreadSafeTokenizer tokenizer = new NotThreadSafeTokenizer() {

      @Override
      public boolean consume(CodeReader code, HtmlCodeBuilder output) {
        output.append((char) code.pop());
        return true;
      }

      @Override
      public NotThreadSafeTokenizer clone() {
        throw new IllegalStateException("The clone method has been called as expected.");
      }
    };
    TokenizerDispatcher colorization = newColorizer(tokenizer);
    colorization.colorize("source code");
  }

  private TokenizerDispatcher newColorizer(Channel<HtmlCodeBuilder>... tokenizers) {
    return new TokenizerDispatcher(Arrays.asList(tokenizers));
  }
}
