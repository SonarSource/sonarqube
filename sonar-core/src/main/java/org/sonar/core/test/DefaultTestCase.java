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
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.api.test.Testable;
import org.sonar.api.test.exception.CoverageAlreadyExistsException;
import org.sonar.api.test.exception.IllegalDurationException;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

import javax.annotation.Nullable;

import java.util.List;

public class DefaultTestCase extends BeanVertex implements MutableTestCase {

  private static final String DURATION = "duration";
  private static final String TYPE = "type";
  private static final String STATUS = "status";
  private static final String NAME = "name";
  private static final String MESSAGE = "message";
  private static final String STACK_TRACE = "stackTrace";
  private static final String COVERS = "covers";
  private static final String LINES = "lines";
  private static final String TESTCASE = "testcase";

  @Override
  public String type() {
    return (String) getProperty(TYPE);
  }

  @Override
  public MutableTestCase setType(@Nullable String s) {
    setProperty(TYPE, s);
    return this;
  }

  @Override
  public Long durationInMs() {
    return (Long) getProperty(DURATION);
  }

  @Override
  public MutableTestCase setDurationInMs(@Nullable Long l) {
    if (l != null && l < 0) {
      throw new IllegalDurationException("Test duration must be positive (got: " + l + ")");
    }
    setProperty(DURATION, l);
    return this;
  }

  @Override
  public Status status() {
    return Status.of((String) getProperty(STATUS));
  }

  @Override
  public MutableTestCase setStatus(@Nullable Status s) {
    setProperty(STATUS, s == null ? null : s.name());
    return this;
  }

  @Override
  public String name() {
    return (String) getProperty(NAME);
  }

  public MutableTestCase setName(String s) {
    setProperty(NAME, s);
    return this;
  }

  @Override
  public String message() {
    return (String) getProperty(MESSAGE);
  }

  @Override
  public MutableTestCase setMessage(String s) {
    setProperty(MESSAGE, s);
    return this;
  }

  @Override
  public String stackTrace() {
    return (String) getProperty(STACK_TRACE);
  }

  @Override
  public MutableTestCase setStackTrace(String s) {
    setProperty(STACK_TRACE, s);
    return this;
  }

  @Override
  public MutableTestCase setCoverageBlock(Testable testable, List<Integer> lines) {
    if (coverageBlock(testable) != null) {
      throw new CoverageAlreadyExistsException("The link between " + name() + " and " + testable.component().key() + " already exists");
    }
    beanGraph().getUnderlyingGraph().addEdge(null, element(), ((BeanVertex) testable).element(), COVERS).setProperty(LINES, lines);
    return this;
  }

  @Override
  public TestPlan testPlan() {
    Vertex plan = GraphUtil.singleAdjacent(element(), Direction.IN, TESTCASE);
    return beanGraph().wrap(plan, DefaultTestPlan.class);
  }

  @Override
  public boolean doesCover() {
    return edgeCovers().iterator().hasNext();
  }

  @Override
  public int countCoveredLines() {
    int result = 0;
    for (Edge edge : edgeCovers()) {
      List<Integer> lines = (List<Integer>) edge.getProperty(LINES);
      result = result + lines.size();
    }
    return result;
  }

  @Override
  public Iterable<CoverageBlock> coverageBlocks() {
    return (Iterable) getEdges(DefaultCoverageBlock.class, Direction.OUT, COVERS);
  }

  @Override
  public CoverageBlock coverageBlock(final Testable testable) {
    return Iterables.find(getEdges(DefaultCoverageBlock.class, Direction.OUT, COVERS), new Predicate<CoverageBlock>() {
      @Override
      public boolean apply(CoverageBlock input) {
        return input.testable().component().key().equals(testable.component().key());
      }
    }, null);
  }

  private Iterable<Edge> edgeCovers() {
    return element().query().labels(COVERS).direction(Direction.OUT).edges();
  }

}
