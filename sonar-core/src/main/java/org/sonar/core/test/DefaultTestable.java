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
package org.sonar.core.test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.component.Component;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.TestCase;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static com.google.common.collect.Maps.newHashMap;

public class DefaultTestable extends BeanVertex implements MutableTestable {

  private static final String COVERS = "covers";

  @Override
  public Component component() {
    Vertex component = GraphUtil.singleAdjacent(element(), Direction.IN, "testable");
    return beanGraph().wrap(component, ComponentVertex.class);
  }

  @Override
  public List<TestCase> testCases() {
    ImmutableList.Builder<TestCase> cases = ImmutableList.builder();
    for (Edge coversEdge : coverEdges()) {
      Vertex testable = coversEdge.getVertex(Direction.OUT);
      cases.add(beanGraph().wrap(testable, DefaultTestCase.class));
    }
    return cases.build();
  }

  @Override
  public TestCase testCaseByName(final String name) {
    return Iterables.find(testCases(), new Predicate<TestCase>() {
      @Override
      public boolean apply(TestCase input) {
        return input.name().equals(name);
      }
    }, null);
  }

  @Override
  public int countTestCasesOfLine(Integer line) {
    int number = 0;
    for (Edge edge : coverEdges()) {
      if (Iterables.contains(lines(edge), line)) {
        number++;
      }
    }
    return number;
  }

  @Override
  public Map<Integer, Integer> testCasesByLines() {
    Map<Integer, Integer> testCasesByLines = newHashMap();
    for (Integer line : testedLines()) {
      testCasesByLines.put(line, countTestCasesOfLine(line));
    }
    return testCasesByLines;
  }

  @Override
  public List<TestCase> testCasesOfLine(int line) {
    ImmutableList.Builder<TestCase> cases = ImmutableList.builder();
    for (Edge edge : coverEdges()) {
      if (lines(edge).contains(line)) {
        Vertex vertexTestable = edge.getVertex(Direction.OUT);
        DefaultTestCase testCase = beanGraph().wrap(vertexTestable, DefaultTestCase.class);
        cases.add(testCase);
      }
    }
    return cases.build();
  }

  @Override
  public SortedSet<Integer> testedLines() {
    ImmutableSortedSet.Builder<Integer> coveredLines = ImmutableSortedSet.naturalOrder();
    for (Edge edge : coverEdges()) {
      coveredLines.addAll(lines(edge));
    }
    return coveredLines.build();
  }

  @Override
  public CoverageBlock coverageBlock(final TestCase testCase) {
    return Iterables.find(getEdges(DefaultCoverageBlock.class, Direction.IN, COVERS), new Predicate<CoverageBlock>() {
      @Override
      public boolean apply(CoverageBlock input) {
        return input.testCase().name().equals(testCase.name());
      }
    }, null);
  }

  @Override
  public Iterable<CoverageBlock> coverageBlocks() {
    return (Iterable) getEdges(DefaultCoverageBlock.class, Direction.IN, COVERS);
  }

  private Iterable<Edge> coverEdges() {
    return element().query().labels(COVERS).direction(Direction.IN).edges();
  }

  private List<Integer> lines(Edge edge) {
    return (List<Integer>) edge.getProperty("lines");
  }
}
