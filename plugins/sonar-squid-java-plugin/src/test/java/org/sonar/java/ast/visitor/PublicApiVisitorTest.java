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

public class PublicApiVisitorTest {

  private Squid squid;

  @Before
  public void setup() {
    squid = new Squid(new JavaSquidConfiguration());
  }

  @Test
  public void analyseClassWithCommentsOnLineOfCode() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/ClassWithCommentsOnLineOfCode.java"));
    SourceCode res = squid.aggregate();
    assertEquals(7, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(4, res.getInt(Metric.COMMENT_LINES));
    assertEquals(2, res.getInt(Metric.PUBLIC_API));
  }

  @Test
  public void analyseVars() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/ClassWithVars.java"));
    SourceCode res = squid.aggregate();
    assertEquals(15, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(59, res.getInt(Metric.LINES));
    assertEquals(10, res.getInt(Metric.BLANK_LINES));
    assertEquals(21, res.getInt(Metric.COMMENT_BLANK_LINES));
    assertEquals(5, res.getInt(Metric.PUBLIC_API));
    assertEquals(1, res.getInt(Metric.HEADER_COMMENT_LINES));
    assertEquals(3, res.getInt(Metric.PUBLIC_DOC_API));
    assertEquals(13, res.getInt(Metric.COMMENT_LINES));
    assertEquals(0.6, res.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY), 0.01);
  }

  @Test
  public void analyseConstants() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/Constants.java"));
    SourceCode res = squid.aggregate();
    assertEquals(9, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(76, res.getInt(Metric.LINES));
    assertEquals(11, res.getInt(Metric.BLANK_LINES));
    assertEquals(21, res.getInt(Metric.COMMENT_BLANK_LINES));
    assertEquals(1, res.getInt(Metric.PUBLIC_API));
    assertEquals(15, res.getInt(Metric.HEADER_COMMENT_LINES));
    assertEquals(1, res.getInt(Metric.PUBLIC_DOC_API));
    assertEquals(35, res.getInt(Metric.COMMENT_LINES));
  }

  @Test
  public void analyseApiDocCounter() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/ClassWithComments.java"));
    SourceCode res = squid.aggregate();
    assertEquals(7, res.getInt(Metric.PUBLIC_API));
    assertEquals(4, res.getInt(Metric.PUBLIC_DOC_API));
    assertEquals(66, res.getInt(Metric.LINES));
    assertEquals(18, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(0.47, res.getDouble(Metric.COMMENT_LINES_DENSITY), 0.01);
  }

  @Test
  public void analyseJavaDocCounterOnAnnotation() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/annotations/AnnotationDefinition.java"));
    SourceCode res = squid.aggregate();
    assertEquals(3, res.getInt(Metric.PUBLIC_API));
    assertEquals(2, res.getInt(Metric.PUBLIC_DOC_API));
    assertEquals(18, res.getInt(Metric.LINES));
    assertEquals(0.36, res.getDouble(Metric.COMMENT_LINES_DENSITY), 0.01);
  }

  @Test
  public void analyseInterfaceComments() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/InterfaceWithComments.java"));
    SourceCode res = squid.aggregate();
    assertEquals(6, res.getInt(Metric.PUBLIC_API));
    assertEquals(2, res.getInt(Metric.PUBLIC_DOC_API));
    assertEquals(1, res.getInt(Metric.HEADER_COMMENT_LINES));
    assertEquals(0.33, res.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY), 0.01);
  }

  @Test
  public void excludeMethodWithOverrideAnnotation() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/MethodsWithOverrideAnnotation.java"));
    SourceCode res = squid.aggregate();
    assertEquals(2, res.getInt(Metric.PUBLIC_API));
    assertEquals(0, res.getInt(Metric.PUBLIC_DOC_API));
  }

  @Test
  public void excludeEmptyConstructor() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/javadoc/EmptyConstructor.java"));
    SourceCode res = squid.aggregate();
    assertEquals(3, res.getInt(Metric.PUBLIC_API));
    assertEquals(0, res.getInt(Metric.PUBLIC_DOC_API));
  }
}
