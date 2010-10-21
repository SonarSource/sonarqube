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
package org.sonar.java.bytecode.check;

import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonar.java.ast.SquidTestUtils.getFile;

public class ArchitectureCheckTest {

  private Squid squid;

  @Test
  public void testDependencyCheckOneErrorMessage() {
    check("*", "java.**.Pattern");

    SourceFile file = (SourceFile) squid.search("ArchitectureCheckOneErrorMessage.java");
    assertThat(file.getCheckMessages().size(), is(1));
    CheckMessage message = file.getCheckMessages().iterator().next();
    assertThat(message.getDefaultMessage(), is("ArchitectureCheckOneErrorMessage shouldn't directly use java/util/regex/Pattern"));
    assertThat(message.getLine(), is(6));
  }

  @Test
  public void testDependencyCheckDateForbidden() {
    check("*", "**.Date");

    SourceFile file = (SourceFile) squid.search("ArchitectureCheckDateForbidden.java");
    assertThat(file.getCheckMessages().size(), is(2));
    // for (CheckMessage message : file.getCheckMessages()) {
    // System.out.println(message.getDefaultMessage());
    // }
  }

  @Test
  public void testDependencyCheckToSqlFromUI() {
    check("*UI", "java.sql.*");

    SourceFile file = (SourceFile) squid.search("ArchitectureCheckToSqlFromUI.java");
    assertThat(file.getCheckMessages().size(), is(4));
    // for (CheckMessage message : file.getCheckMessages()) {
    // System.out.println(message.getDefaultMessage() + " at line " + message.getLine());
    // }
  }

  @Test
  public void testDependencyCheckOKFromClassesToClasses() {
    check("*SA", "java.sql.*");

    SourceFile file = (SourceFile) squid.search("ArchitectureCheckToSqlFromUI.java");
    assertThat(file.getCheckMessages().size(), is(0));
  }

  private void check(String fromClasses, String toClasses) {
    ArchitectureCheck check = new ArchitectureCheck();
    check.setFromPatterns(fromClasses);
    check.setToPatterns(toClasses);

    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(getFile("/bytecode/architecture/src"));
    squid.registerVisitor(check);
    squid.register(BytecodeScanner.class).scanDirectory(getFile("/bytecode/architecture/bin"));
  }
}
