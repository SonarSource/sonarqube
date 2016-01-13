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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.channel.CodeReader;

public class LiteralTokenizerTest {

  private HtmlCodeBuilder codeBuilder;

  @Before
  public void init() {
    codeBuilder = new HtmlCodeBuilder();
  }

  @Test
  public void nextToken() {
    LiteralTokenizer tokenizer = new LiteralTokenizer("<s>", "</s>");
    assertTrue(tokenizer.consume(new CodeReader("\"toto\";"), codeBuilder));
    assertEquals("<s>\"toto\"</s>", codeBuilder.toString());
    init();
    assertTrue(tokenizer.consume(new CodeReader("\"\";"), codeBuilder));
    assertEquals("<s>\"\"</s>", codeBuilder.toString());
    init();
    assertTrue(tokenizer.consume(new CodeReader("\'\';"), codeBuilder));
    assertEquals("<s>\'\'</s>", codeBuilder.toString());
    init();
    assertTrue(tokenizer.consume(new CodeReader("\"(\\\"\").replace"), codeBuilder));
    assertEquals("<s>\"(\\\"\"</s>", codeBuilder.toString());
    init();
    assertTrue(tokenizer.consume(new CodeReader("\'\\\\\';"), codeBuilder));
    assertEquals("<s>\'\\\\\'</s>", codeBuilder.toString());
    init();
    assertTrue(tokenizer.consume(new CodeReader("\"to\\'to\""), codeBuilder));
    assertEquals("<s>\"to\\'to\"</s>", codeBuilder.toString());
  }
}
