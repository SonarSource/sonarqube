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

import org.sonar.java.bytecode.asm.*;
import org.sonar.squid.api.SourceCodeEdgeUsage;

public class AccessorVisitor extends BytecodeVisitor {
  
  private AsmClass asmClass;
  
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  public void visitMethod(AsmMethod asmMethod) {
    if (asmMethod.isConstructor()) return;
    
    AsmField accessedField = getAccessedField(asmMethod);
    asmMethod.setAccessedField(accessedField);
  }
  
  private AsmField getAccessedField(AsmMethod asmMethod) {
    AsmField accessedField = null;
    
    for (AsmEdge edge: asmMethod.getOutgoingEdges()) {
      if (isCallToNonStaticInternalField(edge)) {
        if (accessedField != null && accessedField != edge.getTo()) {
          accessedField = null;
          break;
        }
        accessedField = (AsmField)edge.getTo();
      } else if (isCallToNonStaticInternalMethod(edge)) {
        accessedField = null;
        break;
      }
    }
    
    return accessedField;
  }
  
  private boolean isCallToNonStaticInternalField(AsmEdge edge) {
    return edge.getTargetAsmClass() == asmClass && edge.getUsage() == SourceCodeEdgeUsage.CALLS_FIELD && !((AsmField)edge.getTo()).isStatic();
  }
  
  private boolean isCallToNonStaticInternalMethod(AsmEdge edge) {
    return edge.getTargetAsmClass() == asmClass && edge.getUsage() == SourceCodeEdgeUsage.CALLS_METHOD && !((AsmMethod)edge.getTo()).isStatic();
  }
  
}
