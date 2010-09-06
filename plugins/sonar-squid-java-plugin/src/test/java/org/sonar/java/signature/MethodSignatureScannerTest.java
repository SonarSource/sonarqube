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
package org.sonar.java.signature;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MethodSignatureScannerTest {

  @Test
  public void scan() {
    MethodSignature method = MethodSignatureScanner.scan("read(Ljava/lang/String;[S)V");
    assertThat(method.getMethodName(), is("read"));

    assertThat(method.getReturnType().getJvmJavaType(), is(JvmJavaType.V));
    assertThat(method.getArgumentTypes().size(), is(2));

    Parameter param1 = method.getArgumentTypes().get(0);
    assertThat(param1.isOject(), is(true));
    assertThat(param1.getClassName(), is("String"));

    Parameter param2 = method.getArgumentTypes().get(1);
    assertThat(param2.isOject(), is(false));
    assertThat(param2.isArray(), is(true));
    assertThat(param2.getJvmJavaType(), is(JvmJavaType.S));
  }

  @Test
  public void scanMethodWithReturnType() {
    MethodSignature method = MethodSignatureScanner.scan("read(Ljava/lang/String;S)[Ljava/util/Vector;");

    assertThat(method.getReturnType().isOject(), is(true));
    assertThat(method.getReturnType().isArray(), is(true));
    assertThat(method.getReturnType().getClassName(), is("Vector"));
  }
}
