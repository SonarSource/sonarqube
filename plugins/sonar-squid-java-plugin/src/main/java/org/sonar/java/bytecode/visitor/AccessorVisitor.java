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

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmMethod;

public class AccessorVisitor extends BytecodeVisitor {

  private AsmClass asmClass;

  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  public void visitMethod(AsmMethod asmMethod) {
    String propertyName = extractPropertyNameFromMethodName(asmMethod);
    if (propertyName != null && asmClass.getField(propertyName) != null) {
      asmMethod.setAccessor(true);
    }
  }

  private String extractPropertyNameFromMethodName(AsmMethod asmMethod) {
    String propertyName;
    String methodName = asmMethod.getName();
    if (methodName.length() > 3 && (methodName.startsWith("get") || methodName.startsWith("set"))) {
      propertyName = methodName.substring(3);
    } else if (methodName.length() > 2 && methodName.startsWith("is")) {
      propertyName = methodName.substring(2);
    } else {
      return null;
    }
    byte[] bytes = propertyName.getBytes();
    bytes[0] = (byte) Character.toLowerCase((char) bytes[0]);
    return new String(bytes);
  }
}
