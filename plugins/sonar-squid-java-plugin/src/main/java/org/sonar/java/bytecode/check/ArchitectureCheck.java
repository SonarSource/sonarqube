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
import org.sonar.check.*;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

@Rule(key = "ArchitecturalConstraint", name = "Architectural constraint", cardinality = Cardinality.MULTIPLE, isoCategory = IsoCategory.Portability, priority = Priority.MAJOR, description = "<p>A source code comply to an architectural model when it fully adheres to a set of architectural constraints. " +
    "A constraint allows to deny references between classes by pattern.</p>" +
    "<p>You can for instance use this rule to :</p>" +
    "<ul><li>forbid access to **.web.** from **.dao.** classes</li>" +
    "<li>forbid access to java.util.Vector, java.util.Hashtable and java.util.Enumeration from any classes</li>" +
    "<li>forbid access to java.sql.** from **.ui.** and **.web.** classes</li></ul>")
public class ArchitectureCheck extends BytecodeCheck {

  @RuleProperty(description = "Optional. If this property is not defined, all classes should adhere to this constraint. Ex : **.web.**")
  private String fromClasses = new String();

  @RuleProperty(description = "Mandatory. Ex : java.util.Vector, java.util.Hashtable, java.util.Enumeration")
  private String toClasses = new String();

  private List<WildcardPattern> fromMatchers;
  private List<WildcardPattern> toMatchers;
  private AsmClass asmClass;
  private Map<String, CheckMessage> internalNames;

  public String getFromClasses() {
    return fromClasses;
  }

  public void setFromClasses(String patterns) {
    this.fromClasses = patterns;
  }

  public String getToClasses() {
    return toClasses;
  }

  public void setToClasses(String patterns) {
    this.toClasses = patterns;
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
    this.internalNames = Maps.newHashMap();
  }

  @Override
  public void leaveClass(AsmClass asmClass) {
    for (CheckMessage message : internalNames.values()) {
      SourceFile sourceFile = getSourceFile(asmClass);
      sourceFile.log(message);
    }
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (edge != null) {
      String internalNameTargetClass = edge.getTargetAsmClass().getInternalName();
      if ( !internalNames.containsKey(internalNameTargetClass)) {
        String nameAsmClass = asmClass.getInternalName();
        if (matches(nameAsmClass, getFromMatchers()) && matches(internalNameTargetClass, getToMatchers())) {
          logMessage(edge);
        }
      } else if (internalNames.get(internalNameTargetClass).getLine() == 0 && edge.getSourceLineNumber() != 0) {
        logMessage(edge);
      }
    }
  }

  private void logMessage(AsmEdge edge) {
    String fromClass = asmClass.getInternalName();
    String toClass = edge.getTargetAsmClass().getInternalName();
    CheckMessage message = new CheckMessage(this, fromClass + " must not use " + toClass);
    message.setLine(edge.getSourceLineNumber());
    internalNames.put(toClass, message);
  }

  private boolean matches(String className, List<WildcardPattern> matchers) {
    for (WildcardPattern matcher : matchers) {
      if (matcher.match(className)) {
        return true;
      }
    }
    return false;
  }

  private List<WildcardPattern> createMatchers(String pattern) {
    List<WildcardPattern> matchers = Lists.newArrayList();
    if (StringUtils.isNotEmpty(pattern)) {
      String[] patterns = pattern.split(",");
      for (String p : patterns) {
        p = StringUtils.replace(p, ".", "/");
        matchers.add(WildcardPattern.create(p));
      }
    }
    return matchers;
  }

  private List<WildcardPattern> getFromMatchers() {
    if (fromMatchers == null) {
      if (StringUtils.isBlank(fromClasses)) {
        fromMatchers = createMatchers("**");
      } else {
        fromMatchers = createMatchers(fromClasses);
      }
    }
    return fromMatchers;
  }

  private List<WildcardPattern> getToMatchers() {
    if (toMatchers == null) {
      toMatchers = createMatchers(toClasses);
    }
    return toMatchers;
  }

}
