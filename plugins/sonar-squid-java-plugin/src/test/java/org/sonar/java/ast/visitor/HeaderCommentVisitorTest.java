/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.java.ast.visitor;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;

public class HeaderCommentVisitorTest {

  private Squid squid;

  @Before
  public void setup() {
    squid = new Squid(new JavaSquidConfiguration());
  }

  @Test
  public void analyseHeaderCommentsStandard() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/ClassWithHeader.java"));
    SourceCode res = squid.aggregate();
    assertEquals(7, res.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(32, res.getInt(Metric.LINES));
    assertEquals(10, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(2, res.getInt(Metric.HEADER_COMMENT_LINES));
  }

  @Test
  public void analyseHeaderCommentsAndNoPackage() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/ClassWithHeaderAndNoPackage.java"));
    SourceCode res = squid.aggregate();
    assertEquals(7, res.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(30, res.getInt(Metric.LINES));
    assertEquals(10, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(2, res.getInt(Metric.HEADER_COMMENT_LINES));
  }

  @Test
  public void analyseHeaderCommentsAndNoPackageNoImports() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/ClassWithHeaderAndNoPackageNoImports.java"));
    SourceCode res = squid.aggregate();
    assertEquals(3, res.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(23, res.getInt(Metric.LINES));
    assertEquals(2, res.getInt(Metric.HEADER_COMMENT_LINES));
    assertEquals(9, res.getInt(Metric.LINES_OF_CODE));
  }

  @Test
  public void analyseJavadocHeaderAndPackage() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/", "foo/ClassWithPackageAndJavadocHeader.java"));
    SourceCode res = squid.aggregate();
    assertEquals(2, res.getInt(Metric.HEADER_COMMENT_LINES));
    assertEquals(3, res.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(50, res.getInt(Metric.LINES));
    assertEquals(27, res.getInt(Metric.LINES_OF_CODE));
  }

  @Test
  public void analyseCCommentWithoutHeader() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/ClassWithoutHeaderAndWithCComment.java"));
    SourceCode res = squid.aggregate();
    assertEquals(3, res.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(0, res.getInt(Metric.HEADER_COMMENT_LINES));
  }
}
