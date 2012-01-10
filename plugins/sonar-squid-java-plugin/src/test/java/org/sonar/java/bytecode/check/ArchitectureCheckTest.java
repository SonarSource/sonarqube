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
package org.sonar.java.bytecode.check;

import org.junit.Test;
import org.sonar.java.CheckMessages;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceFile;

public class ArchitectureCheckTest {

  private Squid squid;

  @Test
  public void testDependencyCheckOneErrorMessage() {
    check("", "java.**.Pattern");

    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("ArchitectureCheckOneErrorMessage.java"));
    checkMessages.assertNext().atLine(6).withMessage("ArchitectureCheckOneErrorMessage must not use java/util/regex/Pattern");
    checkMessages.assertNoMore();
  }

  @Test
  public void testDependencyCheckDateForbidden() {
    check("", "**.Date");

    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("ArchitectureCheckDateForbidden.java"));
    checkMessages.assertNext().atLine(7);
    checkMessages.assertNext().atLine(9);
    checkMessages.assertNoMore();
  }

  @Test
  public void testDependencyCheckToSqlFromUI() {
    check("*UI", "java.sql.*");

    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("ArchitectureCheckToSqlFromUI.java"));
    checkMessages.assertNext().atLine(4);
    checkMessages.assertNoMore();
  }

  @Test
  public void testDependencyCheckOKFromClassesToClasses() {
    check("*SA", "java.sql.*");

    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("ArchitectureCheckToSqlFromUI.java"));
    checkMessages.assertNoMore();
  }

  private void check(String fromClasses, String toClasses) {
    ArchitectureCheck check = new ArchitectureCheck();
    check.setFromClasses(fromClasses);
    check.setToClasses(toClasses);

    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/architecture/src"));
    squid.registerVisitor(check);
    squid.register(BytecodeScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/architecture/bin"));
  }
}
