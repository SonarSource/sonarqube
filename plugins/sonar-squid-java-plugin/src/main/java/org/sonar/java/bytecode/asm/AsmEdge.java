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
package org.sonar.java.bytecode.asm;

import org.sonar.squid.api.SourceCodeEdgeUsage;

public class AsmEdge {

  private final AsmResource from;
  private final AsmResource to;
  private final SourceCodeEdgeUsage usage;
  private int sourceLineNumber = 0;

  AsmEdge(AsmResource from, AsmResource to, SourceCodeEdgeUsage usage) {
    this.from = from;
    this.to = to;
    this.usage = usage;
    to.setUsed(true);
  }

  AsmEdge(AsmResource from, AsmResource to, SourceCodeEdgeUsage usage, int sourceLineNumber) {
    this(from, to, usage);
    this.sourceLineNumber = sourceLineNumber;
  }

  public int getSourceLineNumber() {
    return sourceLineNumber;
  }

  public AsmClass getTargetAsmClass() {
    if (getTo().getParent() != null) {
      return getTo().getParent();
    } else {
      return (AsmClass) getTo();
    }
  }

  public AsmResource getFrom() {
    return from;
  }

  public AsmResource getTo() {
    return to;
  }

  public SourceCodeEdgeUsage getUsage() {
    return usage;
  }

  @Override
  public boolean equals(Object obj) {
    if ( !(obj instanceof AsmEdge)) {
      return false;
    }
    AsmEdge edge = (AsmEdge) obj;
    return from.equals(edge.from) && to.equals(edge.to);
  }

  @Override
  public int hashCode() {
    return from.hashCode() + to.hashCode() + usage.hashCode();
  }
}
