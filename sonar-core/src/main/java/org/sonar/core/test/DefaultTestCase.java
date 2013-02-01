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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.test.Cover;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.api.test.Testable;
import org.sonar.api.test.exception.IllegalDurationException;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

import javax.annotation.Nullable;

import java.util.List;

public class DefaultTestCase extends BeanVertex implements MutableTestCase {

  public Long durationInMs() {
    return (Long) getProperty("duration");
  }

  public MutableTestCase setDurationInMs(@Nullable Long l) {
    if (l != null && l < 0) {
      throw new IllegalDurationException("Test duration must be positive (got: " + l + ")");
    }
    setProperty("duration", l);
    return this;
  }

  public String status() {
    return (String) getProperty("status");
  }

  public MutableTestCase setStatus(@Nullable String s) {
    setProperty("status", s);
    return this;
  }

  /**
   * The key is not blank and unique among the test plan.
   */
  public String key() {
    return (String) getProperty("key");
  }

  public MutableTestCase setKey(String s) {
    setProperty("key", s);
    return this;
  }

  public String name() {
    return (String) getProperty("name");
  }

  public MutableTestCase setName(String s) {
    setProperty("name", s);
    return this;
  }

  public String message() {
    return (String) getProperty("message");
  }

  public MutableTestCase setMessage(String s) {
    setProperty("message", s);
    return this;
  }

  public String stackTrace() {
    return (String) getProperty("stackTrace");
  }

  public MutableTestCase setStackTrace(String s) {
    setProperty("stackTrace", s);
    return this;
  }

  public void setCover(Testable testable, List<Integer> lines) {
    beanGraph().getUnderlyingGraph().addEdge(null, element(), ((BeanVertex) testable).element(), "covers").setProperty("lines", lines);
  }

  public TestPlan testPlan() {
    Vertex plan = GraphUtil.singleAdjacent(element(), Direction.IN, "testcase");
    return beanGraph().wrap(plan, DefaultTestPlan.class);
  }

  public boolean doesCover() {
    return edgeCovers().iterator().hasNext();
  }

  public int countCoveredLines() {
    int result = 0;
    for (Edge edge : edgeCovers()) {
      List<Integer> lines = (List<Integer>) edge.getProperty("lines");
      result = result + lines.size();
    }
    return result;
  }

  public Iterable covers() {
    return getEdges(DefaultCover.class, Direction.OUT, "covers");
  }

  public Cover coverOfTestable(final Testable testable) {
    return Iterables.find(getEdges(DefaultCover.class, Direction.OUT, "covers"), new Predicate<Cover>() {
      public boolean apply(Cover input) {
        return input.testable().component().key().equals(testable.component().key());
      }
    }, null);
  }

  private Iterable<Edge> edgeCovers() {
    return element().query().labels("covers").direction(Direction.OUT).edges();
  }

}
