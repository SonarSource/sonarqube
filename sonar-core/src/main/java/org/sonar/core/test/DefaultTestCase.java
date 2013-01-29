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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.test.CoveredTestable;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.api.test.Testable;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DefaultTestCase extends BeanVertex implements MutableTestCase {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultTestCase.class);

  public String type() {
    return (String) getProperty("type");
  }

  public Long durationInMs() {
    return (Long) getProperty("duration");
  }

  public MutableTestCase setDurationInMs(@Nullable Long l) {
    Preconditions.checkArgument(l == null || l >= 0, String.format("Duration must be positive (got %d)", l));
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

  public void covers(Testable testable, Set<Integer> lines) {
    LOG.info("Covers : " + testable.component().key(), " on "+ lines);

    Vertex componentVertex = GraphUtil.single(beanGraph().getUnderlyingGraph().getVertices("key", testable.component().key()));
    beanGraph().getUnderlyingGraph().addEdge(null, element(), componentVertex, "covers").setProperty("lines", lines);
  }

  public TestPlan testPlan() {
    Vertex plan = GraphUtil.singleAdjacent(element(), Direction.IN, "testcase");
    return beanGraph().wrap(plan, DefaultTestPlan.class);
  }

  public boolean hasCoveredBlocks(){
    return Iterables.size(element().getEdges(Direction.OUT, "covers")) > 0;
  }

  public int countCoveredBlocks() {
    int coveredBlocks = 0;
    for (Edge edge : element().getEdges(Direction.OUT, "covers")){
      List<String> lines = (List<String>) edge.getProperty("lines");
      coveredBlocks = coveredBlocks + lines.size();
    }
    return coveredBlocks;
  }

  public Collection<CoveredTestable> coveredBlocks() {
    return null;
  }
}
