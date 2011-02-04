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
package org.sonar.java.bytecode.asm;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;

public class AsmClassVisitor extends EmptyVisitor {

  private AsmClassProvider asmClassProvider;
  private DETAIL_LEVEL level;
  private AsmClass asmClass;

  public AsmClassVisitor(AsmClassProvider asmClassProvider, AsmClass asmClass, DETAIL_LEVEL level) {
    this.asmClassProvider = asmClassProvider;
    this.level = level;
    this.asmClass = asmClass;
  }

  public void visit(int version, int accessFlags, String internalName, String signature, String superClass, String[] interfaces) {
    if (asmClass.getDetailLevel() == DETAIL_LEVEL.NOTHING) {
      asmClass.setAccessFlags(accessFlags);
      if (asmClass.isInterface()) {
        if (interfaces.length == 1) {
          asmClass.setSuperClass(asmClassProvider.getClass(interfaces[0], DETAIL_LEVEL.STRUCTURE));
        }
      } else {
        if (superClass != null) {
          asmClass.setSuperClass(asmClassProvider.getClass(superClass, DETAIL_LEVEL.STRUCTURE));
        }
        for (String interfaceName : interfaces) {
          asmClass.addInterface(asmClassProvider.getClass(interfaceName, DETAIL_LEVEL.STRUCTURE));
        }
      }
      if (signature != null) {
        String[] internalNames = AsmSignature.extractInternalNames(signature);
        AsmClass[] asmClasses = internalNamesToAsmClasses(internalNames, DETAIL_LEVEL.NOTHING);
        asmClass.addUsesOfClasses(asmClasses);
      }
    }
  }

  public FieldVisitor visitField(int access, String fieldName, String description, String signature, Object value) {
    AsmField field = asmClass.getFieldOrCreateIt(fieldName);
    field.setAccessFlags(access);
    String[] internalNames = AsmSignature.extractInternalNames(description, signature);
    AsmClass[] asmClasses = internalNamesToAsmClasses(internalNames, DETAIL_LEVEL.NOTHING);
    field.addUsesOfClasses(asmClasses);
    return null;
  }

  public MethodVisitor visitMethod(int access, String methodName, String description, String signature, String[] exceptions) {
    AsmMethod method = asmClass.getMethodOrCreateIt(methodName + description);
    if (isInheritedMethodSignature(method.getParent(), method.getKey())) {
      method.setInherited(true);
    }
    method.setSignature(signature);
    method.setBodyLoaded(true);
    method.setAccessFlags(access);
    String[] internalNames = AsmSignature.extractInternalNames(description, signature);
    AsmClass[] asmClasses = internalNamesToAsmClasses(internalNames, DETAIL_LEVEL.NOTHING);
    method.addUsesOfClasses(asmClasses);
    AsmClass[] asmExceptionClasses = internalNamesToAsmClasses(exceptions, DETAIL_LEVEL.NOTHING);
    method.addUsesOfClasses(asmExceptionClasses);
    if (level == DETAIL_LEVEL.STRUCTURE_AND_CALLS) {
      return new AsmMethodVisitor(method, asmClassProvider);
    }
    return null;
  }

  public void visitEnd() {
    asmClass.setDetailLevel(level);
  }

  private AsmClass[] internalNamesToAsmClasses(String[] internalNames, DETAIL_LEVEL level) {
    if (internalNames == null) {
      return new AsmClass[0];
    }
    AsmClass[] asmClasses = new AsmClass[internalNames.length];
    for (int i = 0; i < internalNames.length; i++) {
      asmClasses[i] = asmClassProvider.getClass(internalNames[i], level);
    }
    return asmClasses;
  }

  private boolean isInheritedMethodSignature(AsmClass parent, String key) {
    if (parent.getSuperClass() != null
        && (parent.getSuperClass().getMethod(key) != null || isInheritedMethodSignature(parent.getSuperClass(), key))) {
      return true;
    }
    for (AsmClass interfaceClass : parent.getInterfaces()) {
      if (interfaceClass.getMethod(key) != null || isInheritedMethodSignature(interfaceClass, key)) {
        return true;
      }
    }
    return false;
  }
}
