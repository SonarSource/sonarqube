/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.text;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LinesFactoryTest {

  @Test
  public void getLines() {
    LinesFactory factory = new LinesFactory(new StringReader("import java.util.*;\n\rimport java.io.*;"));
    assertEquals(2, factory.getLines().size());
    assertEquals("import java.util.*;", factory.getLines().get(0).getString());
    assertEquals("import java.io.*;", factory.getLines().get(1).getString());
  }

  @Test
  public void getLinesWithSingleLineComment() {
    LinesFactory factory = new LinesFactory(new StringReader("import java.util.*;\n\rint a = 4; //comments\nimport java.io.*;"));
    Line commentLine = factory.getLines().get(1);
    assertEquals("int a = 4; //comments", commentLine.getString());
    assertEquals("//comments", commentLine.getComment());
  }

  @Test
  public void getLinesWithMultiLineComment() {
    LinesFactory factory = new LinesFactory(new StringReader("import java.util.*;\n\rint a = 4; /*comments\nimport java.io.*;*/"));
    assertEquals("/*comments", factory.getLines().get(1).getComment());
    assertEquals("import java.io.*;*/", factory.getLines().get(2).getComment());
  }
  
  @Test
  public void testEndOfLineWithLFAndCR() {
    LinesFactory factory = new LinesFactory(new StringReader("/*\n\r\n\r\n\r*/"));
    assertEquals("/*", factory.getLines().get(0).getComment());
    assertEquals("*/", factory.getLines().get(3).getComment());
  }
  
  @Test
  public void testEndOfLineWithLF() {
    LinesFactory factory = new LinesFactory(new StringReader("/*\n\n\n*/"));
    assertEquals("/*", factory.getLines().get(0).getComment());
    assertEquals("*/", factory.getLines().get(3).getComment());
  }
  
  @Test
  public void testEndOfLineWithCR() {
    LinesFactory factory = new LinesFactory(new StringReader("/*\r\r\r*/"));
    assertEquals("/*", factory.getLines().get(0).getComment());
    assertEquals("*/", factory.getLines().get(3).getComment());
  }

  @Test
  public void getLinesWithCommentInsideDoubleQuotesString() {
    LinesFactory factory = new LinesFactory(new StringReader("String toto = \"//NOSONAR\""));
    Line commentLine = factory.getLines().get(0);
    assertNull(commentLine.getComment());
  }

  @Test
  public void getLinesWithCommentInsideSingleQuoteString() {
    LinesFactory factory = new LinesFactory(new StringReader("String toto = \'//NOSONAR\'"));
    Line commentLine = factory.getLines().get(0);
    assertNull(commentLine.getComment());
  }

}
