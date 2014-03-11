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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.channel.CodeReader;

public class JavaConstantTokenizerTest {

  private JavaConstantTokenizer tokenizer;
  private HtmlCodeBuilder codeBuilder;

  @Before
  public void init() {
    tokenizer = new JavaConstantTokenizer("<c>", "</c>");
    codeBuilder = new HtmlCodeBuilder();
  }

  @Test
  public void testRead() {
    assertTrue(tokenizer.consume(new CodeReader("CONSTANT"), codeBuilder));
    assertTrue(tokenizer.consume(new CodeReader("IS_CONSTANT "), codeBuilder));
    assertTrue(tokenizer.consume(new CodeReader("IS-CONSTANT "), codeBuilder));
    assertFalse(tokenizer.consume(new CodeReader("Class"), codeBuilder));
    assertFalse(tokenizer.consume(new CodeReader("_NOTACONSTANT"), codeBuilder));
    assertFalse(tokenizer.consume(new CodeReader("IS_not_a_constant"), codeBuilder));
    assertFalse(tokenizer.consume(new CodeReader("property"), codeBuilder));
    assertFalse(tokenizer.consume(new CodeReader(" "), codeBuilder));
  }

  @Test
  public void upperCaseWordsStartingWithADotShoutNotBeHighlighted() {
    CodeReader reader = new CodeReader(".URL");
    reader.pop();
    assertFalse(tokenizer.consume(reader, codeBuilder));
  }

  @Test
  public void hasNextTokenWhenFirstCharacterIsNotAConstant() {
    CodeReader code = new CodeReader("sCONSTANT");
    code.pop();
    assertFalse(tokenizer.consume(code, codeBuilder));
  }

  @Test
  public void nextToken() {
    assertTrue(tokenizer.consume(new CodeReader("IS_TRUE = 4;"), codeBuilder));
    assertEquals("<c>IS_TRUE</c>", codeBuilder.toString());
  }
}
