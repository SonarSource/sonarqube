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
package org.sonar.java.bytecode.asm;

import org.objectweb.asm.Type;

public final class AsmType {

  private AsmType() {
    // only static methods
  }

  public static boolean isArray(Type type) {
    return type.getSort() == Type.ARRAY;
  }

  public static boolean isObject(Type type) {
    return type.getSort() == Type.OBJECT;
  }

  public static boolean isArrayOfObject(Type type) {
    return isArray(type) && type.getElementType().getSort() == Type.OBJECT;
  }

  public static boolean containsObject(Type type) {
    return isObject(type) || isArrayOfObject(type);
  }

  public static boolean isVoid(Type type) {
    return type == Type.VOID_TYPE;
  }

  public static String getObjectInternalName(Type type) {
    if (isObject(type)) {
      return type.getInternalName();
    } else if (isArrayOfObject(type)) {
      return type.getElementType().getInternalName();
    } else {
      throw new IllegalStateException("This method should not be called on a descriptor whitout Object reference : " 
          + type.getDescriptor());
    }
  }

}
