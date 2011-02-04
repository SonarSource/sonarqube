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
import org.sonar.squid.measures.Metric;

public class DITVisitor extends BytecodeVisitor {

  public void visitClass(AsmClass asmClass) {
    int dit = calculateDepthOfInheritance(asmClass);

    getSourceClass(asmClass).add(Metric.DIT, dit);

    if (isMainPublicClassInFile(asmClass)) {
      getSourceFile(asmClass).add(Metric.DIT, dit);
    }
  }

  private int calculateDepthOfInheritance(AsmClass asmClass) {
    int dit = 0;
    AsmClass superClass = asmClass.getSuperClass();
    while (superClass != null) {
      dit++;
      superClass = superClass.getSuperClass();
    }
    return dit;
  }
}
