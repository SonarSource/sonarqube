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
package org.sonar.java.bytecode.check;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;

@Rule(key = "UnusedProtectedMethod", priority = Priority.MAJOR)
public class UnusedProtectedMethodCheck extends BytecodeVisitor {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    if ( !asmMethod.isUsed() && asmMethod.isProtected() && !asmClass.isAbstract() && !SerializableContract.methodMatch(asmMethod)
        && !asmMethod.isInherited()) {
      CheckMessage message = new CheckMessage(this, "Protected method '" + asmMethod.getName() + "(...)' is never used.");
      SourceMethod sourceMethod = getSourceMethod(asmMethod);
      if (sourceMethod != null) {
        message.setLine(sourceMethod.getStartAtLine());
      }
      SourceFile file = getSourceFile(asmClass);
      file.log(message);
    }
  }
}
