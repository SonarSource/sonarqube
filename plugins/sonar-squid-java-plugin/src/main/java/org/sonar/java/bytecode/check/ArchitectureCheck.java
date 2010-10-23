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
import org.sonar.check.Cardinality;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

@Rule(key = "Dependency", name = "Respect rule architecture", cardinality = Cardinality.MULTIPLE, isoCategory = IsoCategory.Portability, priority = Priority.MINOR, description = "<p>Links between classes must respect defined architecture rules.</p>")
public class ArchitectureCheck extends BytecodeCheck {

  @RuleProperty(description = "Pattern forbidden for from classes")
  private String fromPatterns = new String();

  @RuleProperty(description = "Pattern forbidden for to classes")
  private String toPatterns = new String();

  private List<WildcardPattern> fromMatchers;
  private List<WildcardPattern> toMatchers;
  private AsmClass asmClass;
  private Set<String> internalNames;

  public String getFromPatterns() {
    return fromPatterns;
  }

  public void setFromPatterns(String patterns) {
    this.fromPatterns = patterns;
  }

  public String getToPatterns() {
    return toPatterns;
  }

  public void setToPatterns(String patterns) {
    this.toPatterns = patterns;
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
    this.internalNames = Sets.newHashSet();
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (edge != null) {
      String internalNameTargetClass = edge.getTargetAsmClass().getInternalName();
      if ( !internalNames.contains(internalNameTargetClass)) {
        String nameAsmClass = asmClass.getInternalName();
        System.out.println("Checking : " + nameAsmClass + " -> " + internalNameTargetClass);
        if (matches(nameAsmClass, getFromMatchers()) && matches(internalNameTargetClass, getToMatchers())) {
          SourceFile sourceFile = getSourceFile(asmClass);
          CheckMessage message = new CheckMessage(this, nameAsmClass + " shouldn't directly use " + internalNameTargetClass);
          message.setLine(edge.getSourceLineNumber());
          sourceFile.log(message);
          internalNames.add(internalNameTargetClass);
        }
      }
    }
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
      fromMatchers = createMatchers(fromPatterns);
    }
    return fromMatchers;
  }

  private List<WildcardPattern> getToMatchers() {
    if (toMatchers == null) {
      toMatchers = createMatchers(toPatterns);
    }
    return toMatchers;
  }

}
