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
package org.sonar.java.signature;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MethodSignaturePrinterTest {

  @Test
  public void testPrint() {
    List<Parameter> argumentTypes = new ArrayList<Parameter>();
    MethodSignature method = new MethodSignature("read", new Parameter(JvmJavaType.V, false), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method), is("read()V"));

    argumentTypes.add(new Parameter(JvmJavaType.L, "java/lang/String", true));
    method = new MethodSignature("read", new Parameter("org/sonar/squid/Squid", false), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method), is("read([LString;)LSquid;"));

    argumentTypes.add(new Parameter(JvmJavaType.B, false));
    method = new MethodSignature("write", new Parameter(JvmJavaType.I, true), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method), is("write([LString;B)[I"));

    argumentTypes.add(new Parameter(JvmJavaType.I, false));
    method = new MethodSignature("write", new Parameter(JvmJavaType.I, true), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method), is("write([LString;BI)[I"));
  }
}
