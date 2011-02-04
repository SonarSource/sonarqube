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
package org.sonar.java.bytecode.visitor;

import org.sonar.graph.DirectedGraph;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeEdge;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.measures.Metric;

public class DependenciesVisitor extends BytecodeVisitor {

  private SourceClass fromSourceClass;
  private final DirectedGraph<SourceCode, SourceCodeEdge> graph;

  public DependenciesVisitor(DirectedGraph<SourceCode, SourceCodeEdge> graph) {
    this.graph = graph;
  }

  public void visitClass(AsmClass asmClass) {
    this.fromSourceClass = getSourceClass(asmClass);
  }

  public void visitEdge(AsmEdge edge) {
    AsmClass toAsmClass = edge.getTargetAsmClass();
    SourceClass toSourceClass = getSourceClass(toAsmClass);
    switch (edge.getUsage()) {
      case EXTENDS:
        link(fromSourceClass, toSourceClass, SourceCodeEdgeUsage.EXTENDS);
        break;
      case IMPLEMENTS:
        link(fromSourceClass, toSourceClass, SourceCodeEdgeUsage.IMPLEMENTS);
        break;
      default:
        link(fromSourceClass, toSourceClass, SourceCodeEdgeUsage.USES);
        break;
    }
  }

  private void link(SourceClass from, SourceClass to, SourceCodeEdgeUsage link) {
    if (canWeLinkNodes(from, to) && graph.getEdge(from, to) == null) {
      SourceCodeEdge edge = new SourceCodeEdge(from, to, link);
      graph.addEdge(edge);
      from.add(Metric.CE, 1);
      to.add(Metric.CA, 1);
      SourceCodeEdge fileEdge = createEdgeBetweenParents(SourceFile.class, from, to, edge);
      createEdgeBetweenParents(SourcePackage.class, from, to, fileEdge);
    }
  }

  private SourceCodeEdge createEdgeBetweenParents(Class<? extends SourceCode> type, SourceClass from, SourceClass to,
      SourceCodeEdge rootEdge) {
    SourceCode fromParent = from.getParent(type);
    SourceCode toParent = to.getParent(type);
    SourceCodeEdge parentEdge = null;
    if (canWeLinkNodes(fromParent, toParent) && rootEdge != null) {
      if (graph.getEdge(fromParent, toParent) == null) {
        parentEdge = new SourceCodeEdge(fromParent, toParent, SourceCodeEdgeUsage.USES);
        parentEdge.addRootEdge(rootEdge);
        graph.addEdge(parentEdge);
        fromParent.add(Metric.CE, 1);
        toParent.add(Metric.CA, 1);
      } else {
        parentEdge = graph.getEdge(fromParent, toParent);
        if ( !parentEdge.hasAnEdgeFromRootNode(rootEdge.getFrom())) {
          toParent.add(Metric.CA, 1);
        }
        if ( !parentEdge.hasAnEdgeToRootNode(rootEdge.getTo())) {
          fromParent.add(Metric.CE, 1);
        }
        parentEdge.addRootEdge(rootEdge);
      }
    }
    return parentEdge;
  }

  private boolean canWeLinkNodes(SourceCode from, SourceCode to) {
    return from != null && to != null && !from.equals(to);
  }

}
