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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Check;
import org.sonar.check.CheckProperty;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.api.SourceFile;

@Check(key = "Dependency", title = "Respect rule architecture", isoCategory = IsoCategory.Portability, priority = Priority.MINOR, description = "<p>Links between classes must respect defined architecture rules.</p>")
public class ArchitectureCheck extends BytecodeCheck {

  @CheckProperty(title = "Pattern forbidden for from classes", key = "fromClasses")
  private String fromClasses = new String();

  @CheckProperty(title = "Pattern forbidden for to classes", key = "toClasses")
  private String toClasses = new String();

  public String getFromClasses() {
    return fromClasses;
  }

  public void setFromClasses(String fromClasses) {
    this.fromClasses = fromClasses;
  }

  public String getToClasses() {
    return toClasses;
  }

  public void setToClasses(String toClasses) {
    this.toClasses = toClasses;
  }

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (edge != null) {
      SourceCodeEdgeUsage usage = edge.getUsage();
      if (usage.equals(SourceCodeEdgeUsage.USES) || usage.equals(SourceCodeEdgeUsage.CALLS_METHOD)
          || usage.equals(SourceCodeEdgeUsage.CALLS_FIELD) || usage.equals(SourceCodeEdgeUsage.CONTAINS)) {
        String internalNameTargetClass = edge.getTargetAsmClass().getInternalName();
        String nameAsmClass = asmClass.getInternalName();
        if (matchesPattern(nameAsmClass, fromClasses) && matchesPattern(internalNameTargetClass, toClasses)) {
          SourceFile sourceFile = getSourceFile(asmClass);
          CheckMessage message = new CheckMessage(this, nameAsmClass + " shouldn't directly use " + internalNameTargetClass);
          message.setLine(edge.getSourceLineNumber());
          sourceFile.log(message);
        }
      }
    }
  }

  private boolean matchesPattern(String className, String pattern) {
    if (StringUtils.isEmpty(pattern)) {
      return true;
    }
    String[] patterns = pattern.split(",");
    for (String p : patterns) {
      p = StringUtils.replace(p, ".", "/");
      if (WildcardPattern.create(p).match(className)) {
        return true;
      }
    }
    return false;
  }

}
