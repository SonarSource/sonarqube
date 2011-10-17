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

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.bytecode.ClassLoaderBuilder;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.asm.AsmClassProviderImpl;
import org.sonar.java.bytecode.asm.AsmMethod;

import static org.junit.Assert.*;

public class AccessorVisitorTest {

  private static AsmClassProvider asmClassProvider;
  private static AsmClass javaBean;
  private static AccessorVisitor accessorVisitor = new AccessorVisitor();

  @BeforeClass
  public static void init() {
    asmClassProvider = new AsmClassProviderImpl(ClassLoaderBuilder.create(SquidTestUtils.getFile("/bytecode/bin/")));
    javaBean = asmClassProvider.getClass("properties/JavaBean");
    accessorVisitor.visitClass(javaBean);
    for (AsmMethod method : javaBean.getMethods()) {
      accessorVisitor.visitMethod(method);
    }
  }

  @Test
  public void testAccessorMethods() {
    assertTrue(javaBean.getMethod("getName()Ljava/lang/String;").isAccessor());
    assertTrue(javaBean.getMethod("setName(Ljava/lang/String;)V").isAccessor());
    assertTrue(javaBean.getMethod("setFrench(Z)V").isAccessor());
    assertTrue(javaBean.getMethod("isFrench()Z").isAccessor());
    assertFalse(javaBean.getMethod("anotherMethod()V").isAccessor());
  }
}
