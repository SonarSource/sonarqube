/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.java.ast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.lang.CharEncoding;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.AnalysisException;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.measures.Metric;

public class JavaAstScannerTest {

  private Squid squid;

  @Before
  public void setup() throws UnsupportedEncodingException {
    squid = new Squid(new JavaSquidConfiguration(false, Charset.defaultCharset(), 0.9));
  }

  @Test
  public void testMacRomanEncoding() {
    squid = new Squid(new JavaSquidConfiguration(false, Charset.forName("MacRoman")));
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/encoding/MacRomanEncoding.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(4, prj.getInt(Metric.METHODS));
  }

  @Test(expected = AnalysisException.class)
  public void testCP1252EncodingWithWrongDefined() {
    squid = new Squid(new JavaSquidConfiguration(true, Charset.forName("MacRoman")));
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/encoding/CP1252Encoding.java"));
  }

  @Test
  public void testCheckstyleParsingBug() {
    // see
    // http://sourceforge.net/tracker/?func=detail&atid=397078&aid=1667137&group_id=29721
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/parsingErrors/CheckstyleBug.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(0, prj.getInt(Metric.CLASSES));
  }

  /**
   * SONAR-1908
   */
  @Test
  public void testUnicodeEscape() {
    // see
    // https://sourceforge.net/tracker/?func=detail&aid=3296452&group_id=29721&atid=397078
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/parsingErrors/UnicodeEscape.java"));
  }

  /**
   * SONAR-1836: bug in Checkstyle 5.2 - 5.4
   */
  @Test
  public void testLineCommentAtTheEndOfFile() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/parsingErrors/LineCommentAtTheEndOfFile.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(1, prj.getInt(Metric.CLASSES));
  }

  @Test
  public void testEmptyClassWithComment() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/emptyFiles/ClassWithOnlyComment.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(0, prj.getInt(Metric.CLASSES));
    assertEquals(1, prj.getInt(Metric.PACKAGES));
    assertEquals(1, prj.getInt(Metric.FILES));
    assertEquals(1, prj.getInt(Metric.COMMENT_LINES));

    assertNotNull(squid.search("ClassWithOnlyComment.java"));//file
    assertNull(squid.search("ClassWithOnlyComment"));//class
  }

  @Test
  public void testEmptyFileWithBlankLines() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/emptyFiles/EmptyFileWithBlankLines.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(0, prj.getDouble(Metric.COMMENT_LINES_DENSITY), 0.01);
  }

  @Test
  public void testClassWithPackageImportsComment() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/emptyFiles", "foo/ClassWithPackageImportsComment.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(0, prj.getInt(Metric.CLASSES));
    assertEquals(1, prj.getInt(Metric.PACKAGES));
    assertEquals(2, prj.getInt(Metric.COMMENT_LINES));
  }

  @Test
  public void testEncodingWithSystemSetting() {
    SourceProject macRoman;
    SourceProject cp1252;
    String currentEncoding = System.getProperty("file.encoding");
    // changing the system encoding can have no effects
    try {
      System.setProperty("file.encoding", "UTF-16");
      Charset defaultEncoding = Charset.defaultCharset();
      if ( !defaultEncoding.displayName().equals("UTF-16")) {
        return;
      }
      System.setProperty("file.encoding", "MacRoman");
      squid = new Squid(new JavaSquidConfiguration(false));
      squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/encoding/MacRomanEncoding.java"));
      macRoman = squid.aggregate();
      System.setProperty("file.encoding", "CP1252");
      squid = new Squid(new JavaSquidConfiguration(false));
      squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/encoding/CP1252Encoding.java"));
      cp1252 = squid.aggregate();
    } finally {
      System.setProperty("file.encoding", currentEncoding);
    }
    assertEquals(4, macRoman.getInt(Metric.METHODS));
    assertEquals(4, cp1252.getInt(Metric.METHODS));
  }

  @Test
  public void testCP1252Encoding() {
    squid = new Squid(new JavaSquidConfiguration(false, Charset.forName("CP1252")));
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/encoding/CP1252Encoding.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(4, prj.getInt(Metric.METHODS));
  }

  @Test
  public void testUTF8Encoding() {
    squid = new Squid(new JavaSquidConfiguration(false, Charset.forName(CharEncoding.UTF_8)));
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/encoding/Utf8Encoding.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(4, prj.getInt(Metric.METHODS));
  }

  @Test
  public void testInterfaceWithAnnotations() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/annotations/InterfaceWithAnnotation.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(11, prj.getInt(Metric.LINES));
    assertEquals(6, prj.getInt(Metric.LINES_OF_CODE));
    assertEquals(0, prj.getInt(Metric.STATEMENTS));
    assertEquals(2, prj.getInt(Metric.METHODS));
    assertEquals(2, prj.getInt(Metric.COMPLEXITY));
  }

  @Test
  public void testClassesWithGenerics() {
    squid.register(JavaAstScanner.class).scanDirectory(SquidTestUtils.getFile("/special_cases/generics"));
    SourceProject prj = squid.aggregate();
    assertEquals(2, prj.getInt(Metric.FILES));
    assertEquals(3, prj.getInt(Metric.METHODS));
  }

  @Test
  public void testPackageInfo() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/packageInfo", "org/apache/cxf/jaxrs/ext/logging/package-info.java"));
    SourceProject prj = squid.aggregate();
    assertEquals(1, prj.getInt(Metric.FILES));
    assertEquals(4, prj.getInt(Metric.LINES_OF_CODE));
    assertEquals(29, prj.getInt(Metric.LINES));
  }
}
