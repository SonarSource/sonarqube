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
import static org.sonar.java.ast.SquidTestUtils.getFile;

import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;

public class EndAtLineVisitorTest {

  @Test
  public void testEndAtLineForMethod() {
    Squid squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanFile(getFile("/metrics/methods/ClassWithStaticMethods.java"));
    assertEquals(17, squid.search("ClassWithStaticMethods#doJob2()V").getEndAtLine());
  }

  @Test
  public void testEndAtLineForClass() {
    Squid squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanFile(getFile("/metrics/methods/ClassWithStaticMethods.java"));
    assertEquals(30, squid.search("ClassWithStaticMethods").getEndAtLine());
  }
}
