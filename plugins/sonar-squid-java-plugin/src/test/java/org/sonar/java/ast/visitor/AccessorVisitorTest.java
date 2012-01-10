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
package org.sonar.java.ast.visitor;

import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;

import static org.junit.Assert.assertEquals;

public class AccessorVisitorTest {

  private Squid squid;

  @Test
  public void analyzePureJavaBean() {
    squid = new Squid(new JavaSquidConfiguration(true));
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/accessors/PureJavaBean.java"));
    SourceCode res = squid.aggregate();
    assertEquals(54, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(94, res.getInt(Metric.LINES));
    assertEquals(6, res.getInt(Metric.ACCESSORS));
    assertEquals(10, res.getInt(Metric.METHODS));
  }

  @Test
  public void considerAccessorAsMethod() {
    squid = new Squid(new JavaSquidConfiguration(false));
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/accessors/JavaBeanWithApiDoc.java"));
    SourceCode res = squid.aggregate();
    assertEquals(10, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(30, res.getInt(Metric.LINES));
    assertEquals(2, res.getInt(Metric.METHODS));
    assertEquals(0, res.getInt(Metric.ACCESSORS));
    assertEquals(4, res.getInt(Metric.PUBLIC_API));
    assertEquals(2, res.getInt(Metric.COMPLEXITY));
  }

  @Test
  public void analyseVarAccessorsImpactOnOtherMeasures() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/accessors/JavaBeanWithApiDoc.java"));
    SourceCode res = squid.aggregate();
    assertEquals(10, res.getInt(Metric.LINES_OF_CODE));
    assertEquals(30, res.getInt(Metric.LINES));
    assertEquals(1, res.getInt(Metric.METHODS));
    assertEquals(1, res.getInt(Metric.ACCESSORS));
    assertEquals(3, res.getInt(Metric.PUBLIC_API));
    assertEquals(1, res.getInt(Metric.COMPLEXITY));
  }
}
