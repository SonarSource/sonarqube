/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.api;

import java.util.HashSet;
import java.util.Set;

import org.sonar.graph.Edge;

public class SourceCodeEdge implements Edge<SourceCode> {

  private final SourceCode from;
  private final SourceCode to;
  private final SourceCodeEdgeUsage usage;
  private Set<SourceCodeEdge> rootEdges;
  private Set<SourceCode> rootFromNodes;
  private Set<SourceCode> rootToNodes;
  private final int hashcode;
  private SourceCodeEdge parent;

  public SourceCodeEdge(SourceCode from, SourceCode to, SourceCodeEdgeUsage link) {
    this(from, to, link, null);
  }

  public SourceCodeEdge(SourceCode from, SourceCode to, SourceCodeEdgeUsage usage, SourceCodeEdge rootEdge) {
    this.hashcode = from.hashCode() * 31 + to.hashCode() + usage.hashCode(); //NOSONAR even if this basic algorithm could be improved
    this.from = from;
    this.to = to;
    this.usage = usage;
    addRootEdge(rootEdge);
  }

  public SourceCode getFrom() {
    return from;
  }

  public SourceCode getTo() {
    return to;
  }

  public SourceCodeEdgeUsage getUsage() {
    return usage;
  }

  private boolean noRoots() {
    return rootEdges == null;
  }

  public boolean hasAnEdgeFromRootNode(SourceCode rootFromNode) {
    if (noRoots()) {
      return false;
    }
    return rootFromNodes.contains(rootFromNode);
  }

  public boolean hasAnEdgeToRootNode(SourceCode rootToNode) {
    if (noRoots()) {
      return false;
    }
    return rootToNodes.contains(rootToNode);
  }

  public Set<SourceCodeEdge> getRootEdges() {
    return rootEdges;
  }

  public int getNumberOfRootFromNodes() {
    if (noRoots()) {
      return 0;
    }
    return rootFromNodes.size();
  }

  public final void addRootEdge(SourceCodeEdge rootRelationShip) {
    if (noRoots()) {
      rootEdges = new HashSet<SourceCodeEdge>();
      rootFromNodes = new HashSet<SourceCode>();
      rootToNodes = new HashSet<SourceCode>();
    }
    if (rootRelationShip != null) {
      rootEdges.add(rootRelationShip);
      rootFromNodes.add(rootRelationShip.getFrom());
      rootToNodes.add(rootRelationShip.getTo());
      rootRelationShip.setParent(this);
    }
  }

  public int getWeight() {
    if (noRoots()) {
      return 0;
    }
    return rootEdges.size();
  }

  public SourceCodeEdge getParent() {
    return parent;
  }

  public SourceCodeEdge setParent(SourceCodeEdge parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if ( !(obj instanceof SourceCodeEdge) || this.hashCode() != obj.hashCode()) {
      return false;
    }
    SourceCodeEdge edge = (SourceCodeEdge) obj;
    return from.equals(edge.from) && to.equals(edge.to);
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public String toString() {
    return "from : " + from + ", to : " + to;
  }
}
