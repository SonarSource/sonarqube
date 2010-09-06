/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import static org.sonar.java.ast.SquidTestUtils.getFile;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;

public class LinesOfCodeVisitorTest {

  private Squid squid;

  @Before
  public void setup() {
    squid = new Squid(new JavaSquidConfiguration());
  }

  @Test
  public void analyseTestNcloc() {
    squid.register(JavaAstScanner.class).scanFile(getFile("/metrics/ncloc/TestNcloc.java"));
    SourceCode res = squid.aggregate();
    assertEquals(39, res.getInt(Metric.LINES));
    assertEquals(9, res.getInt(Metric.BLANK_LINES));
    assertEquals(26, res.getInt(Metric.LINES_OF_CODE));

    SourceCode classSource = squid.search("ClassWithStaticMethods");
    assertEquals(26, classSource.getInt(Metric.LINES_OF_CODE));

    SourceCode methodSource = squid.search("ClassWithStaticMethods#doJob2()V");
    assertEquals(8, methodSource.getInt(Metric.LINES_OF_CODE));
  }

  @Test
  public void analyseTestNclocWithClassComment() {
    squid.register(JavaAstScanner.class).scanFile(getFile("/metrics/ncloc/TestNclocWithClassComment.java"));
    SourceCode res = squid.aggregate();
    assertEquals(46, res.getInt(Metric.LINES));
    assertEquals(9, res.getInt(Metric.BLANK_LINES));
    assertEquals(4, res.getInt(Metric.COMMENT_LINES));
    assertEquals(7, res.getInt(Metric.COMMENT_BLANK_LINES));
    assertEquals(26, res.getInt(Metric.LINES_OF_CODE));
  }

  @Test
  public void analyseTestNclocWithHeader() {
    squid.register(JavaAstScanner.class).scanFile(getFile("/metrics/ncloc/TestNclocWithHeader.java"));
    SourceCode res = squid.aggregate();
    assertEquals(59, res.getInt(Metric.LINES));
    assertEquals(11, res.getInt(Metric.BLANK_LINES));
    assertEquals(4, res.getInt(Metric.HEADER_COMMENT_LINES));
    assertEquals(12, res.getInt(Metric.COMMENT_BLANK_LINES));
    assertEquals(27, res.getInt(Metric.LINES_OF_CODE));
  }
}
