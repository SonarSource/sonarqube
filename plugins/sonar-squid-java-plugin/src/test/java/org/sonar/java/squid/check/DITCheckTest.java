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

package org.sonar.java.squid.check;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonar.java.ast.SquidTestUtils.getFile;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.java.squid.SquidScanner;
import org.sonar.squid.Squid;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

public class DITCheckTest {

  private static Squid squid;

  @BeforeClass
  public static void setup() {
    squid = new Squid(new JavaSquidConfiguration());
    DITCheck check = new DITCheck();
    check.setMax(1);
    squid.registerVisitor(check);
    squid.register(JavaAstScanner.class).scanDirectory(getFile("/bytecode/unusedProtectedMethod/src"));
    squid.register(BytecodeScanner.class).scanDirectory(getFile("/bytecode/unusedProtectedMethod/bin"));
    squid.register(SquidScanner.class).scan();
  }

  @Test
  public void testDepthOfInheritanceGreaterThanMaximum() {
    SourceFile file = (SourceFile) squid.search("UnusedProtectedMethod.java");
    assertThat(file.getCheckMessages().size(), is(1));
    CheckMessage message = file.getCheckMessages().iterator().next();
    assertThat(message.getLine(), is(7));
  }

  @Test
  public void testDepthOfInheritanceLowerThanMaximum() {
    SourceFile file = (SourceFile) squid.search("Job.java");
    assertThat(file.getCheckMessages().size(), is(0));
  }

}
