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
package org.sonar.java.bytecode.check;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

@Rule(key = "CallToDeprecatedMethod", name = "Avoid use of deprecated method", priority = Priority.MINOR,
    description = "<p>Once deprecated, a method should no longer be used as it means that "
        + "the method might be removed sooner or later.</p>")
public class CallToDeprecatedMethodCheck extends BytecodeCheck {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (edge.getTo().isDeprecated() && edge.getTo() instanceof AsmMethod) {
      AsmMethod targetMethod = (AsmMethod) edge.getTo();
      SourceFile sourceFile = getSourceFile(asmClass);
      CheckMessage message = new CheckMessage(this, "Method '" + targetMethod.getName() + "(...)' is deprecated.");
      message.setLine(edge.getSourceLineNumber());
      sourceFile.log(message);
    }
  }
}
