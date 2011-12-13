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
package org.sonar.java.bytecode.check;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.CheckMessages;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceFile;

public class CallToDeprecatedMethodCheckTest {

  private static Squid squid;

  @BeforeClass
  public static void setup() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/callToDeprecatedMethod/src"));
    squid.registerVisitor(CallToDeprecatedMethodCheck.class);
    squid.register(BytecodeScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/callToDeprecatedMethod/bin"));
  }

  @Test
  public void testCallToDeprecatedMethod() {
    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("CallToDeprecatedMethod.java"));
    checkMessages.assertNext().atLine(6);
    checkMessages.assertNoMore();
  }
}
