/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.squid;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.picocontainer.MutablePicoContainer;
import org.picocontainer.containers.TransientPicoContainer;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.DirectedGraphAccessor;
import org.sonar.squid.api.CodeScanner;
import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.Query;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeEdge;
import org.sonar.squid.api.SourceCodeSearchEngine;
import org.sonar.squid.api.SourceCodeTreeDecorator;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.api.SquidConfiguration;
import org.sonar.squid.indexer.SquidIndex;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.measures.MetricDef;

public class Squid implements DirectedGraphAccessor<SourceCode, SourceCodeEdge>, SourceCodeSearchEngine {

  private MutablePicoContainer pico;
  private SourceProject project;
  private SquidIndex squidIndex;
  private DirectedGraph<SourceCode, SourceCodeEdge> graph = new DirectedGraph<SourceCode, SourceCodeEdge>();
  private Set<CodeVisitor> externalCodeVisitors = new HashSet<CodeVisitor>();

  public Squid(SquidConfiguration conf) {
    pico = new TransientPicoContainer();
    pico.addComponent(conf);
    project = new SourceProject("Project");
    squidIndex = new SquidIndex();
    squidIndex.index(project);
    pico.addComponent(squidIndex);
    pico.addComponent(project);
    pico.addComponent(graph);
  }

  public Squid() {
    this(new SquidConfiguration());
  }

  public void registerVisitor(CodeVisitor visitor) {
    externalCodeVisitors.add(visitor);
  }

  public void registerVisitor(Class<? extends CodeVisitor> visitor) {
    addToPicocontainer(visitor);
    externalCodeVisitors.add(pico.getComponent(visitor));
  }

  public <S extends CodeScanner> S register(Class<S> scannerClass) {
    if(pico.getComponent(scannerClass) != null){
      throw new IllegalStateException("The Squid SCANNER '" + scannerClass.getName() + "' can't be registered multiple times.");
    }
    addToPicocontainer(scannerClass);
    S scanner = pico.getComponent(scannerClass);
    for (Object clazz : scanner.getVisitorClasses()) {
      addToPicocontainer((Class) clazz);
      scanner.accept(pico.<CodeVisitor> getComponent((Class) clazz));
    }
    for (CodeVisitor externalVisitor : externalCodeVisitors) {
      scanner.accept(externalVisitor);
    }
    return scanner;
  }

  /**
   * @deprecated use {@link #decorateSourceCodeTreeWith(MetricDef...)} instead
   */
  @Deprecated
  public SourceProject aggregate() {
    return decorateSourceCodeTreeWith(Metric.values());
  }

  public SourceProject decorateSourceCodeTreeWith(MetricDef... metrics) {
    SourceCodeTreeDecorator decorator = new SourceCodeTreeDecorator(project);
    decorator.decorateWith(metrics);
    return project;
  }

  public SourceProject getProject() {
    return project;
  }

  private void addToPicocontainer(Class<?> classToExpose) {
    if (pico.getComponent(classToExpose) == null) {
      pico.addComponent(classToExpose);
    }
  }

  public SourceCode search(String key) {
    return squidIndex.search(key);
  }

  public Collection<SourceCode> search(Query... query) {
    return squidIndex.search(query);
  }

  public SourceCodeEdge getEdge(SourceCode from, SourceCode to) {
    return graph.getEdge(from, to);
  }

  public Collection<SourceCodeEdge> getIncomingEdges(SourceCode to) {
    return graph.getIncomingEdges(to);
  }

  public Collection<SourceCodeEdge> getOutgoingEdges(SourceCode from) {
    return graph.getOutgoingEdges(from);
  }

  public Set<SourceCode> getVertices() {
    return graph.getVertices();
  }

  public List<SourceCodeEdge> getEdges(Collection<SourceCode> vertices) {
    return graph.getEdges(vertices);
  }

  public boolean hasEdge(SourceCode from, SourceCode to) {
    return graph.hasEdge(from, to);
  }

  public void flush() {
    graph = null;
    pico = null;
  }
}
