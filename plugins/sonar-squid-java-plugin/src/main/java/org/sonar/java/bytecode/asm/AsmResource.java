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
package org.sonar.java.bytecode.asm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonar.squid.api.SourceCodeEdgeUsage;

public class AsmResource {

  private int accessFlags;
  AsmClass parent;
  boolean used = false;
  private List<AsmEdge> outgoingEdges = new ArrayList<AsmEdge>();

  void setAccessFlags(int accessFlags) {
    this.accessFlags = accessFlags;
  }

  void addUsesOfClasses(AsmClass[] asmClasses) {
    for (AsmClass asmClass : asmClasses) {
      addEdge(new AsmEdge(this, asmClass, SourceCodeEdgeUsage.USES));
    }
  }

  public Set<AsmClass> getDistinctUsedAsmClasses() {
    Set<AsmClass> distinctUsedAsmClasses = new HashSet<AsmClass>();
    for (AsmEdge usage : getOutgoingEdges()) {
      if (usage.getUsage() == SourceCodeEdgeUsage.USES) {
        distinctUsedAsmClasses.add((AsmClass) usage.getTo());
      }
    }
    return distinctUsedAsmClasses;
  }

  public Set<AsmClass> getImplementedInterfaces() {
    Set<AsmClass> implementedInterfaces = new HashSet<AsmClass>();
    for (AsmEdge usage : getOutgoingEdges()) {
      if (usage.getUsage() == SourceCodeEdgeUsage.IMPLEMENTS) {
        implementedInterfaces.add((AsmClass) usage.getTo());
      }
    }
    return implementedInterfaces;
  }

  public void addEdge(AsmEdge edge) {
    outgoingEdges.add(edge);
  }

  public Collection<AsmEdge> getOutgoingEdges() {
    return outgoingEdges;
  }

  public AsmClass getParent() {
    return parent;
  }

  public boolean isAbstract() {
    return AsmAccessFlags.isAbstract(accessFlags);
  }

  public boolean isInterface() {
    return AsmAccessFlags.isInterface(accessFlags);
  }

  public boolean isStatic() {
    return AsmAccessFlags.isStatic(accessFlags);
  }

  boolean isPublic() {
    return AsmAccessFlags.isPublic(accessFlags);
  }

  public boolean isPrivate() {
    return AsmAccessFlags.isPrivate(accessFlags);
  }

  public boolean isProtected() {
    return AsmAccessFlags.isProtected(accessFlags);
  }

  boolean isFinal() {
    return AsmAccessFlags.isFinal(accessFlags);
  }

  public boolean isDeprecated() {
    return AsmAccessFlags.isDeprecated(accessFlags);
  }

  public void setUsed(boolean used) {
    this.used = used;
  }

  public boolean isUsed() {
    return used;
  }
}
