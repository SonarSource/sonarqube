/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

package org.sonar.colorizer;

import org.junit.Before;
import org.junit.Test;
import org.sonar.channel.CodeReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultilinesDocTokenizerTest {

  private HtmlCodeBuilder codeBuilder;

  @Before
  public void init() {
    codeBuilder = new HtmlCodeBuilder();
  }

  @Test
  public void testStandardComment() {
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("{[||", "");
    assertThat(tokenizer.hasNextToken(new CodeReader("{[|| And here is strange  multi-line comment"), new HtmlCodeBuilder()), is(true));
    assertThat(tokenizer.hasNextToken(new CodeReader("// this is not a strange multi-line comment"), new HtmlCodeBuilder()), is(false));
  }

  @Test
  public void testLongStartToken() {
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("/***", "**/");
    assertTrue(tokenizer.consume(new CodeReader("/*** multi-line comment**/ private part"), codeBuilder));
    assertEquals("/*** multi-line comment**/", codeBuilder.toString());
  }
  
  @Test
  public void testStartTokenEndTokenOverlapping() {
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("/*", "*/");
    assertTrue(tokenizer.consume(new CodeReader("/*// multi-line comment*/ private part"), codeBuilder));
    assertEquals("/*// multi-line comment*/", codeBuilder.toString());
  }
  
  @Test
  public void testMultilinesComment() {
    CodeReader reader = new CodeReader("/* multi-line comment\n*/ private part");
    MultilinesDocTokenizer tokenizer = new MultiLineDocTokenizerImpl("/*", "*/");
    assertTrue(tokenizer.consume(reader, codeBuilder));
    assertEquals("/* multi-line comment", codeBuilder.toString());
    reader.pop();
    assertTrue(tokenizer.consume(reader, codeBuilder));
    assertEquals("/* multi-line comment*/", codeBuilder.toString());
  }

  public class MultiLineDocTokenizerImpl extends MultilinesDocTokenizer {

    public MultiLineDocTokenizerImpl(String startToken, String endToken) {
      super(startToken, endToken, "", "");
    }
  }

}
