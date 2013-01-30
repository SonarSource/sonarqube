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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
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
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;

public class DefaultTestable extends BeanVertex implements MutableTestable {

  public Component component() {
    Vertex component = GraphUtil.singleAdjacent(element(), Direction.IN, "testable");
    return beanGraph().wrap(component, ComponentVertex.class);
  }

  public Collection<TestCase> testCases() {
    List<TestCase> testCases = newArrayList();
    for (Edge edge : getCovers()) {
      Vertex testable = edge.getVertex(Direction.OUT);
      testCases.add(beanGraph().wrap(testable, DefaultTestCase.class));
    }
    return testCases;
  }

  public int countTestCasesOfLine(int line) {
    int number = 0;
    for (Edge edge : getCovers()) {
      if (Iterables.contains(testedLines(edge), Long.valueOf(line))) {
        number++;
      }
    }
    return number;
  }

  public Collection<TestCase> testCasesOfLine(int line) {
    ImmutableList.Builder<TestCase> cases = ImmutableList.builder();
    for (Edge edge : getCovers()) {
      if (Iterables.contains(testedLines(edge), Long.valueOf(line))) {
        Vertex vertexTestable = edge.getVertex(Direction.OUT);
        DefaultTestCase testCase = beanGraph().wrap(vertexTestable, DefaultTestCase.class);
        cases.add(testCase);
      }
    }
    return cases.build();
  }

  public SortedSet<Long> testedLines() {
    ImmutableSortedSet.Builder<Long> coveredLines = ImmutableSortedSet.naturalOrder();
    for (Edge edge : getCovers()) {
      coveredLines.addAll(testedLines(edge));
    }
    return coveredLines.build();
  }

  private Iterable<Edge> getCovers() {
    return element().getEdges(Direction.IN, "covers");
  }

  private List<Long> testedLines(Edge edge) {
    return (List<Long>) edge.getProperty("lines");
  }

}