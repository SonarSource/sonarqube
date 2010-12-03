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
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;

@Rule(key = "UnusedPrivateMethod", name = "Unused private method",
    priority = Priority.MAJOR, description = "<p>Private methods that are never executed are dead code. " +
        "Dead code means unnecessary, inoperative code that should be removed. " +
        "This helps in maintenance by decreasing the maintained code size, " +
        "making it easier to understand the program and preventing bugs from being introduced.</p>" +
        "<p>In the following two cases, private methods are not considered as dead code by Sonar :</p>" +
        "<ul><li>Private empty constructors that are intentionally used to prevent any direct instanciation of a class.</li>" +
        "<li>Private methods : readObject(...), writeObject(...), writeReplace(...), readResolve(...) " +
        "which can contractually be used when implementing the Serializable interface.</li></ul>")
public class UnusedPrivateMethodCheck extends BytecodeCheck {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    if (!asmMethod.isUsed() && asmMethod.isPrivate() && !asmMethod.isDefaultConstructor() && !SerializableContract.methodMatch(asmMethod)) {
      CheckMessage message = new CheckMessage(this, "Private method '" + asmMethod.getName() + "(...)' is never used.");
      SourceMethod sourceMethod = getSourceMethod(asmMethod);
      if (sourceMethod != null) {
        message.setLine(sourceMethod.getStartAtLine());
      }
      SourceFile file = getSourceFile(asmClass);
      file.log(message);
    }
  }
}
