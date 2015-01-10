/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.colorizer;

import org.junit.Before;
import org.junit.Test;
import org.sonar.channel.CodeReader;

import static org.assertj.core.api.Assertions.assertThat;

public class MultilinesDocTokenizerTest {

  private HtmlCodeBuilder codeBuilder;

  @Before
  public void init() {
    codeBuilder = new HtmlCodeBuilder();
  }

  @Test
  public void testStandardComment() {
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("{[||", "");
    assertThat(tokenizer.hasNextToken(new CodeReader("{[|| And here is strange  multi-line comment"), new HtmlCodeBuilder())).isTrue();
    assertThat(tokenizer.hasNextToken(new CodeReader("// this is not a strange multi-line comment"), new HtmlCodeBuilder())).isFalse();
  }

  @Test
  public void testLongStartToken() {
    CodeReader reader = new CodeReader("/*** multi-line comment**/ private part");
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("/***", "**/");
    assertThat(tokenizer.consume(reader, codeBuilder)).isTrue();
    assertThat(codeBuilder.toString()).isEqualTo("/*** multi-line comment**/");
  }

  @Test
  public void testStartTokenEndTokenOverlapping() {
    CodeReader reader = new CodeReader("/*// multi-line comment*/ private part");
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("/*", "*/");
    assertThat(tokenizer.consume(reader, codeBuilder)).isTrue();
    assertThat(codeBuilder.toString()).isEqualTo("/*// multi-line comment*/");
  }

  @Test
  public void testMultilinesComment() {
    CodeReader reader = new CodeReader("/* multi-line comment\n*/ private part");
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("/*", "*/");
    assertThat(tokenizer.consume(reader, codeBuilder)).isTrue();
    assertThat(codeBuilder.toString()).isEqualTo("/* multi-line comment");
    reader.pop();
    assertThat(tokenizer.consume(reader, codeBuilder)).isTrue();
    assertThat(codeBuilder.toString()).isEqualTo("/* multi-line comment*/");
  }

  /**
   * SONAR-3531
   */
  @Test
  public void should_work_for_html_comments() {
    CodeReader reader = new CodeReader("<!-- multi-line comment\n--> private part");
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("<!--", "-->");
    assertThat(tokenizer.consume(reader, codeBuilder)).isTrue();
    assertThat(codeBuilder.toString()).isEqualTo("&lt;!-- multi-line comment");
    reader.pop();
    assertThat(tokenizer.consume(reader, codeBuilder)).isTrue();
    assertThat(codeBuilder.toString()).isEqualTo("&lt;!-- multi-line comment--&gt;");
  }

  /**
   * SONAR-3531
   */
  @Test
  public void should_be_possible_to_use_two_instances() {
    CodeReader reader = new CodeReader("<!-- multi-line comment\n--> private part");
    MultilinesDocTokenizer tokenizer1 = new MultiLineDocTokenizerImpl("<!--", "-->");
    MultilinesDocTokenizer tokenizer2 = new MultiLineDocTokenizerImpl("<!--", "-->");
    assertThat(tokenizer1.consume(reader, codeBuilder)).isTrue();
    reader.pop();
    assertThat(tokenizer2.consume(reader, codeBuilder)).isFalse();
  }

  public class MultiLineDocTokenizerImpl extends MultilinesDocTokenizer {
    public MultiLineDocTokenizerImpl(String startToken, String endToken) {
      super(startToken, endToken, "", "");
    }
  }

}
