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

public abstract class AsmClassProvider {

  public enum DETAIL_LEVEL {
    NOTHING(1), // Nothing is loaded from the bytecode
    STRUCTURE(2), // superclass and interfaces are loaded along with fields and methods but not types used by fields or methods
    STRUCTURE_AND_CALLS(3);// calls to other methods are loaded

    private int internalLevel;

    private DETAIL_LEVEL(int level) {
      this.internalLevel = level;
    }

    boolean isGreaterThan(DETAIL_LEVEL level) {
      return this.internalLevel > level.internalLevel;
    }
  }

  public abstract AsmClass getClass(String internalName, DETAIL_LEVEL level);

  public final AsmClass getClass(String internalName) {
    return getClass(internalName, DETAIL_LEVEL.STRUCTURE_AND_CALLS);
  }
}
