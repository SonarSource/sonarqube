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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.measures.Metric;

public class CommentVisitorTest {

  private Squid squid;

  @Before
  public void setup() {
    squid = new Squid(new JavaSquidConfiguration());
  }

  @Test
  public void analyseTest002() {
    String path = "/metrics/loc/Test002.java";
    SourceProject prj = scan(path);
    assertEquals(1, prj.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(1, prj.getInt(Metric.COMMENT_LINES));
    assertEquals(0, prj.getInt(Metric.HEADER_COMMENT_LINES));
  }

  @Test
  public void analyseTest001() {
    SourceProject res = scan("/metrics/loc/Test001.java");
    assertEquals(4, res.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(5, res.getInt(Metric.COMMENT_LINES));
    assertEquals(1, res.getInt(Metric.HEADER_COMMENT_LINES));
    assertEquals(6, res.getInt(Metric.COMMENT_BLANK_LINES));
  }

  @Test
  public void testCommentedCode() {
    SourceProject res = scan("/metrics/commentedCode/CommentedCode.java");
    assertEquals(4, res.getInt(Metric.COMMENT_LINES));
    assertEquals(4, res.getInt(Metric.COMMENTED_OUT_CODE_LINES));
    assertEquals(25, res.getInt(Metric.LINES));
    assertEquals(7, res.getInt(Metric.LINES_OF_CODE));

    SourceCode method = squid.search("CommentedCode#analyse()V");
    assertEquals(3, method.getInt(Metric.COMMENTED_OUT_CODE_LINES));
    assertEquals(2, method.getInt(Metric.COMMENT_BLANK_LINES));
  }

  @Test
  @Ignore("TODO")
  public void testCommentedOutFile() {
    SourceProject res = scan("/metrics/commentedCode", "org/foo/CommentedOutFile.java");
  }

  @Test
  @Ignore("TODO")
  public void shouldGuessPackageOfcommentedOutFile() {
    SourceProject res = scan("/metrics/commentedCode", "org/foo/CommentedOutFile.java");
  }

  @Test
  public void testNoSonarTagDetection() {
    scan("/rules/FileWithNOSONARTags.java");
    SourceFile file = (SourceFile) squid.search("FileWithNOSONARTags.java");
    assertEquals(2, file.getNoSonarTagLines().size());
    assertTrue(file.hasNoSonarTagAtLine(5));
    assertFalse(file.hasNoSonarTagAtLine(6));
    assertTrue(file.hasNoSonarTagAtLine(10));
  }

  @Test
  public void testNoSonarTagDetectionWhenNoTag() {
    scan("/rules/FileWithoutNOSONARTags.java");
    SourceFile file = (SourceFile) squid.search("FileWithoutNOSONARTags.java");
    assertEquals(0, file.getNoSonarTagLines().size());
    assertFalse(file.hasNoSonarTagAtLine(6));
  }

  @Test
  public void testJavaClassWithGWTNativeCode() {
    SourceProject project = scan("/special_cases/gwt/JavaClassWithGWTNativeCode.java");
    assertEquals(17, project.getInt(Metric.LINES));
    assertEquals(4, project.getInt(Metric.COMMENT_LINES));
    assertEquals(6, project.getInt(Metric.LINES_OF_CODE));
    assertEquals(0, project.getInt(Metric.COMMENTED_OUT_CODE_LINES));
  }

  private SourceProject scan(String filePath) {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile(filePath));
    return squid.aggregate();
  }

  private SourceProject scan(String basedir, String filePath) {
    InputFile inputFile = SquidTestUtils.getInputFile(basedir, filePath);
    squid.register(JavaAstScanner.class).scanFile(inputFile);
    return squid.aggregate();
  }
}
