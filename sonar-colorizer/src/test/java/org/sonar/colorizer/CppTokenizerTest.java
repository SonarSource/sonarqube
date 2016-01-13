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

import org.junit.Before;
import org.junit.Test;
import org.sonar.channel.CodeReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CppTokenizerTest {

  private HtmlCodeBuilder codeBuilder;

  @Before
  public void init() {
    codeBuilder = new HtmlCodeBuilder();
  }

  @Test
  public void testRead() {
    CppDocTokenizer javadocTokenizer = new CppDocTokenizer("<c>", "</c>");
    assertTrue(javadocTokenizer.consume(new CodeReader("/*this is a cpp comment*/ private"), codeBuilder));
    assertEquals("<c>/*this is a cpp comment*/</c>", codeBuilder.toString());
    
    assertFalse(javadocTokenizer.consume(new CodeReader("//this is not a cpp comment"), codeBuilder));
  }

  
  @Test
  public void testReadOnMultilines() {
    CppDocTokenizer javadocTokenizer = new CppDocTokenizer("<c>", "</c>");
    CodeReader reader = new CodeReader("/*this is \n a cpp comment*/ private");
    assertTrue(javadocTokenizer.consume(reader, codeBuilder));
    assertEquals("<c>/*this is </c>", codeBuilder.toString());
    codeBuilder.append((char)reader.pop());
    assertTrue(javadocTokenizer.consume(reader, codeBuilder));
    assertEquals("<c>/*this is </c>\n<c> a cpp comment*/</c>", codeBuilder.toString());
  }
}
