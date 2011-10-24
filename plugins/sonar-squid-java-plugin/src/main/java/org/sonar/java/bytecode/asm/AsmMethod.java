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

import java.util.ArrayList;
import java.util.List;

import org.sonar.squid.api.SourceCodeEdgeUsage;

public class AsmMethod extends AsmResource {

  private String name;
  private String key;
  private boolean inherited = false;
  private boolean empty = false;
  private boolean bodyLoaded = true;
  private AsmField accessedField = null;
  private String signature;
  private AsmMethod implementationLinkage = null;

  public AsmMethod(AsmClass parent, String name, String descriptor) {
    this.parent = parent;
    this.name = name;
    key = name + descriptor;
  }

  public AsmMethod(AsmClass parent, String key) {
    this.parent = parent;
    this.key = key;
    this.name = key.substring(0, key.indexOf('('));
  }

  public String getName() {
    return name;
  }

  public String getKey() {
    return key;
  }

  public String getGenericKey() {
    if (signature != null) {
      return name + signature;
    }
    return getKey();
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getSignature() {
    return signature;
  }

  public List<AsmField> getCallsToField() {
    List<AsmField> callsToField = new ArrayList<AsmField>();
    for (AsmEdge usage : getOutgoingEdges()) {
      if (usage.getUsage() == SourceCodeEdgeUsage.CALLS_FIELD) {
        callsToField.add((AsmField) usage.getTo());
      }
    }
    return callsToField;
  }

  public List<AsmMethod> getCallsToMethod() {
    List<AsmMethod> callsToMethod = new ArrayList<AsmMethod>();
    for (AsmEdge usage : getOutgoingEdges()) {
      if (usage.getUsage() == SourceCodeEdgeUsage.CALLS_METHOD) {
        callsToMethod.add((AsmMethod) usage.getTo());
      }
    }
    return callsToMethod;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object instanceof AsmMethod) {
      AsmMethod otherMethod = (AsmMethod) object;
      return parent.equals(otherMethod.parent) && key.equals(otherMethod.key);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return parent.hashCode() + key.hashCode();
  }

  public boolean isConstructor() {
    return "<init>".equals(name) || "<clinit>".equals(name);
  }

  public boolean isDefaultConstructor() {
    return "<init>()V".equals(key);
  }

  void setInherited(boolean inherited) {
    this.inherited = inherited;
  }

  public boolean isInherited() {
    return inherited;
  }

  public boolean isEmpty() {
    return empty;
  }

  public boolean isBodyLoaded() {
    return bodyLoaded;
  }

  void setBodyLoaded(boolean bodyLoaded) {
    this.bodyLoaded = bodyLoaded;
  }

  void setEmpty(boolean empty) {
    this.empty = empty;
  }

  public boolean isAccessor() {
    return accessedField != null;
  }
  
  public AsmField getAccessedField() {
    return accessedField;
  }

  public void setAccessedField(AsmField accessedField) {
    this.accessedField = accessedField;
  }

  @Override
  public String toString() {
    return key;
  }

  public boolean isStaticConstructor() {
    return "<init>".equals(name);
  }

  public void linkTo(AsmMethod implementationLinkage) {
    this.implementationLinkage = implementationLinkage;
  }

  public AsmMethod getImplementationLinkage() {
    return implementationLinkage;
  }
}
