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

import org.objectweb.asm.Opcodes;

public final class AsmAccessFlags {

  private AsmAccessFlags() {
    // utils class : only static methods
  }

  public static boolean isPrivate(int accessFlags) {
    return (accessFlags & Opcodes.ACC_PRIVATE) != 0;
  }

  public static boolean isProtected(int accessFlags) {
    return (accessFlags & Opcodes.ACC_PROTECTED) != 0;
  }

  public static boolean isPublic(int accessFlags) {
    return (accessFlags & Opcodes.ACC_PUBLIC) != 0;
  }

  public static boolean isStatic(int accessFlags) {
    return (accessFlags & Opcodes.ACC_STATIC) != 0;
  }

  public static boolean isAbstract(int accessFlags) {
    return (accessFlags & Opcodes.ACC_ABSTRACT) != 0;
  }

  public static boolean isInterface(int accessFlags) {
    return (accessFlags & Opcodes.ACC_INTERFACE) != 0;
  }

  public static boolean isDeprecated(int accessFlags) {
    return (accessFlags & Opcodes.ACC_DEPRECATED) != 0;
  }

  public static boolean isFinal(int accessFlags) {
    return (accessFlags & Opcodes.ACC_FINAL) != 0;
  }
}
