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
package org.sonar.core.test;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.component.Component;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.TestCase;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;

public class DefaultTestable extends BeanVertex implements MutableTestable {

  public Component component() {
    Vertex component = GraphUtil.singleAdjacent(element(), Direction.IN, "testable");
    return beanGraph().wrap(component, ComponentVertex.class);
  }

  public Collection<TestCase> coveringTestCases() {
    List<TestCase> testCases = newArrayList();
    for (Edge edge : element().getEdges(Direction.IN, "covers")){
      Vertex testable = edge.getVertex(Direction.OUT);
      testCases.add(beanGraph().wrap(testable, DefaultTestCase.class));
    }
    return testCases;
  }

  public Collection<TestCase> testCasesCoveringLine(int line) {
    Set<TestCase> testCases = newHashSet();
    for (Edge edge : element().getEdges(Direction.IN, "covers")){
      if (Iterables.contains( (List<Integer>) edge.getProperty("lines"), line)){
        Vertex testable = edge.getVertex(Direction.OUT);
        testCases.add(beanGraph().wrap(testable, DefaultTestCase.class));
      }
    }
    return testCases;
  }

  public SortedSet<Integer> coveredLines() {
    SortedSet<Integer> coveredLines = newTreeSet();
    for (Edge edge : element().getEdges(Direction.IN, "covers")){
      List<Integer> lines = (List<Integer>) edge.getProperty("lines");
      coveredLines.addAll(lines);
    }
    return coveredLines;
  }

}