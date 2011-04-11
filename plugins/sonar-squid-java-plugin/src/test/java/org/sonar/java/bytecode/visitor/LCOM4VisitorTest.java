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
package org.sonar.java.bytecode.visitor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.measures.Metric;

public class LCOM4VisitorTest {

  private static Squid squid;

  @BeforeClass
  public static void setup() {
    JavaSquidConfiguration conf = new JavaSquidConfiguration();
    conf.addFieldToExcludeFromLcom4Calculation("LOG");
    squid = new Squid(conf);
    squid.register(JavaAstScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/lcom4/src"));
    squid.register(BytecodeScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/lcom4/bin"));
    squid.decorateSourceCodeTreeWith(Metric.values());
  }

  @Test
  public void testExclusionOfFieldNamesFromLcom4Calculation() {
    assertThat(squid.search("ExclusionOfFieldNamesFromLcom4Calculation").getInt(Metric.LCOM4), is(2));
  }
  
  @Test
  public void testLCOM4Visitor() {
    assertThat(squid.search("LCOM4Exclusions").getInt(Metric.LCOM4), is(2));
  }

}
