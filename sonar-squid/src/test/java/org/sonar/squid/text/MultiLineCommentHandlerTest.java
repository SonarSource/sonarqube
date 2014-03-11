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

package org.sonar.squid.text;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MultiLineCommentHandlerTest {

  @Test(expected = IllegalStateException.class)
  public void illegalCallToMatchToEnd() {
    MultiLinesCommentHandler handler = new MultiLinesCommentHandler();
    Line line = new Line();
    StringBuilder builder = new StringBuilder("d");
    handler.matchToEnd(line, builder);
  }

  @Test
  public void matchWithEndOfLine() {
    MultiLinesCommentHandler handler = new MultiLinesCommentHandler();
    Line line = new Line();
    StringBuilder builder = new StringBuilder("import java.util.*;  /*");
    assertTrue(handler.matchToBegin(line, builder));
    builder.append('N');
    assertFalse(handler.matchToEnd(line, builder));
    builder.append('O');
    assertFalse(handler.matchToEnd(line, builder));
    assertFalse(handler.matchWithEndOfLine(line, builder));
    assertEquals("/*NO", line.getComment());
    builder.append('*');
    assertFalse(handler.matchToEnd(line, builder));
    builder.append('/');
    assertTrue(handler.matchToEnd(line, builder));
    assertEquals("*/", line.getComment());
  }

  @Test
  public void testHeaderLicenseComment() {
    MultiLinesCommentHandler handler = new MultiLinesCommentHandler();
    Line line = new Line(1);
    StringBuilder builder = new StringBuilder("/*");
    assertTrue(handler.matchToBegin(line, builder));
    assertFalse(handler.matchWithEndOfLine(line, builder));
    assertTrue(line.isThereLicenseHeaderComment());

    line = new Line(2);
    builder = new StringBuilder("/*");
    assertTrue(handler.matchToBegin(line, builder));
    assertFalse(handler.matchWithEndOfLine(line, builder));
    assertFalse(line.isThereLicenseHeaderComment());
  }

  @Test
  public void testJavaDocComment() {
    MultiLinesCommentHandler handler = new MultiLinesCommentHandler();
    Line line = new Line(1);
    StringBuilder builder = new StringBuilder("/*");
    assertTrue(handler.matchToBegin(line, builder));
    builder.append('*');
    assertFalse(handler.matchToEnd(line, builder));
    assertFalse(handler.matchWithEndOfLine(line, builder));
    assertTrue(line.isThereJavadoc());

    handler = new MultiLinesCommentHandler();
    line = new Line(1);
    builder = new StringBuilder("/*");
    assertTrue(handler.matchToBegin(line, builder));
    assertFalse(handler.matchWithEndOfLine(line, builder));
    assertFalse(line.isThereJavadoc());
  }

  @Test
  public void matchToBegin() {
    MultiLinesCommentHandler handler = new MultiLinesCommentHandler();
    assertFalse(handler.matchToBegin(new Line(), new StringBuilder("import java.util.*;")));
    assertFalse(handler.matchToBegin(new Line(), new StringBuilder("")));
    assertTrue(handler.matchToBegin(new Line(), new StringBuilder("import java.util.*;  /*")));
  }
  
  @Test
  public void testBeginEndCommentWithOnly3Chars() {
    MultiLinesCommentHandler handler = new MultiLinesCommentHandler();
    Line line = new Line(1);
    StringBuilder builder = new StringBuilder("/*");
    assertTrue(handler.matchToBegin(line, builder));
    builder = new StringBuilder("/*/");
    assertFalse(handler.matchToEnd(line, builder));
    
    handler = new MultiLinesCommentHandler();
    line = new Line(1);
    builder = new StringBuilder("/*");
    assertTrue(handler.matchToBegin(line, builder));
    builder = new StringBuilder("/**/");
    assertTrue(handler.matchToEnd(line, builder));
  }
}
