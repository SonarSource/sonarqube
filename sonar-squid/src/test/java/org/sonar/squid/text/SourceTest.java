/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.squid.measures.Metric;
import org.sonar.squid.recognizer.CodeRecognizer;

import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class SourceTest {

  private CodeRecognizer codeRecognizer = new CodeRecognizer(0.91, new JavaFootprint());

  @Test
  public void testGetLines() {
    String[] lines = { "", "int i = 0;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(2, source.getMeasure(Metric.LINES));
  }

  @Test(expected = IllegalStateException.class)
  public void testGetIllegalMetric() {
    String[] lines = { "", "int i = 0;" };
    Source source = new Source(lines, codeRecognizer);
    source.getMeasure(Metric.COMPLEXITY);
  }

  @Test
  public void testGetBlankLines() {
    String[] lines = { "package toto;", " " };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(1, source.getMeasure(Metric.BLANK_LINES));
  }

  @Test
  public void testGetCppCommentLines() {
    String[] lines = { "package toto;", "//this is a comment", "int i = 4; //new comment" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(2, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(1, source.getMeasure(Metric.COMMENT_LINES, 2, 2));
    assertEquals(2, source.getMeasure(Metric.LINES_OF_CODE));
  }

  @Test
  public void testGetCCommentLines() {
    String[] lines = { "package toto;", " int a = 4; /*this is a comment", "new line of comment", "end of comment */ int b = 4;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(3, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(1, source.getMeasure(Metric.COMMENT_LINES, 2, 2));
    assertEquals(3, source.getMeasure(Metric.LINES_OF_CODE));
  }
  
  @Test
  public void testGetAdjacentCCommentBlocks() {
    String[] lines = { "/*first comment*//*second ", " * + \"Ver.", "comment*/" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(3, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(3, source.getMeasure(Metric.LINES));
  }

  @Test
  public void testGetLinesOfCode() {
    String[] lines = { "package toto;", " ", "import java.util.*;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(2, source.getMeasure(Metric.LINES_OF_CODE));
    assertEquals(0, source.getMeasure(Metric.LINES_OF_CODE, 2, 2));
  }

  @Test
  public void testGetCommentedCodeOutLines() {
    String[] lines = { "", "/*package toto;", "}*/", " ", "import java.util.*;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(1, source.getMeasure(Metric.LINES_OF_CODE));
    assertEquals(0, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(2, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  @Test
  public void testBlankLinesAfterEndOfComment() {
    String[] lines = { "/*Comment*/ " };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(0, source.getMeasure(Metric.LINES_OF_CODE));
    assertEquals(1, source.getMeasure(Metric.COMMENT_LINES));
  }

  @Test
  public void testGetCommentedCodeOutLinesIntoJavadoc() {
    String[] lines = { "/**package toto;", "}*/", " ", "import java.util.*;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(1, source.getMeasure(Metric.LINES_OF_CODE));
    assertEquals(2, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(0, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  @Test
  public void testGetBlankCommentLines() {
    String[] lines = { "/**", "*/", "import java.util.*;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(3, source.getMeasure(Metric.LINES));
    assertEquals(1, source.getMeasure(Metric.LINES_OF_CODE));
    assertEquals(0, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(2, source.getMeasure(Metric.COMMENT_BLANK_LINES));
  }

  @Test
  public void testGetNoSonarTagLines() {
    String[] lines = { "import java.util.*;", "//NOSONAR comment", };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(1, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(1, source.getNoSonarTagLines().size());
  }

  @Test
  public void testGetBlankLinesFromTo() {
    String[] lines = { "package toto;", "", "import java.util.*", "   " };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(1, source.getMeasure(Metric.BLANK_LINES, 1, 3));
    assertEquals(1, source.getMeasure(Metric.BLANK_LINES, 3, 4));
    assertEquals(2, source.getMeasure(Metric.BLANK_LINES, 1, 4));
  }

  @Test
  public void endWithEmptyLine() {
    String[] lines = { "package toto;", "" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(1, source.getMeasure(Metric.BLANK_LINES));
    assertEquals(2, source.getMeasure(Metric.LINES));
  }

  @Test(expected = IllegalStateException.class)
  public void testGetBlankLinesFromToWithOutOfBoundIndex() {
    String[] lines = { "package toto;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(1, source.getMeasure(Metric.BLANK_LINES, 1, 3));
  }

  @Test
  public void testConstructorWithReader() {
    Source source = new Source(new StringReader("package toto; \nimport java.util.*;"), codeRecognizer);
    assertEquals(2, source.getMeasure(Metric.LINES));
    assertEquals(2, source.getMeasure(Metric.LINES_OF_CODE));
  }

  @Test
  public void nativeGWTCodeRecognition() {
    String[] lines = { "/*-{", "// JavaScript code", "return this.nextSibling;", "}-*/;" };
    Source source = new Source(lines, codeRecognizer);
    assertEquals(4, source.getMeasure(Metric.LINES));
    assertEquals(3, source.getMeasure(Metric.LINES_OF_CODE));
    assertEquals(1, source.getMeasure(Metric.COMMENT_LINES));
  }

  @Test
  public void testSingleLineCommentWithDoubleDash() {
    String[] lines = { "import java.util.*;", "--NOSONAR", };
    Source source = new Source(new StringArrayReader(lines), codeRecognizer, "--");
    assertEquals(1, source.getMeasure(Metric.COMMENT_LINES));
    assertEquals(1, source.getNoSonarTagLines().size());
  }
}
