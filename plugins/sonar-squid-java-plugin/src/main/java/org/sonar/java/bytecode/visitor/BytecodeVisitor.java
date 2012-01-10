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
package org.sonar.java.bytecode.visitor;

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.signature.MethodSignature;
import org.sonar.java.signature.MethodSignaturePrinter;
import org.sonar.java.signature.MethodSignatureScanner;
import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.indexer.SquidIndex;

public class BytecodeVisitor implements CodeVisitor {

  SquidIndex index;

  public void visitClass(AsmClass asmClass) {
  }

  public void visitMethod(AsmMethod asmMethod) {
  }

  public void visitField(AsmField asmField) {
  }

  public void visitEdge(AsmEdge asmEdge) {

  }

  public void leaveClass(AsmClass asmClass) {
  }

  protected final SourceClass getSourceClass(AsmClass asmClass) {
    return (SourceClass) index.search(asmClass.getInternalName());
  }

  protected final boolean isMainPublicClassInFile(AsmClass asmClass) {
    return index.search(asmClass.getInternalName() + ".java") != null;
  }

  protected final SourceFile getSourceFile(AsmClass asmClass) {
    return getSourceClass(asmClass).getParent(SourceFile.class);
  }

  protected final SourceMethod getSourceMethod(AsmMethod asmMethod) {
    MethodSignature methodSignature = MethodSignatureScanner.scan(asmMethod.getGenericKey());
    AsmClass asmClass = asmMethod.getParent();
    return (SourceMethod) index.search(asmClass.getInternalName() + "#" + MethodSignaturePrinter.print(methodSignature));
  }

  public final void setSquidIndex(SquidIndex index) {
    this.index = index;
  }
}
