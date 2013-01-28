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
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.test.CoveredTestable;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.api.test.Testable;
import org.sonar.core.component.ElementWrapper;
import org.sonar.core.graph.GraphUtil;

import javax.annotation.Nullable;

import java.util.Collection;

public class DefaultTestCase extends ElementWrapper<Vertex> implements MutableTestCase {

  public String type() {
    return (String) element().getProperty("type");
  }

  public Long durationInMs() {
    return (Long) element().getProperty("duration");
  }

  public MutableTestCase setDurationInMs(@Nullable Long l) {
    Preconditions.checkArgument(l==null || l >=0, String.format("Duration must be positive (got %d)", l));
    element().setProperty("duration", l);
    return this;
  }

  public String status() {
    return (String) element().getProperty("status");
  }

  public MutableTestCase setStatus(@Nullable String s) {
    element().setProperty("status", s);
    return this;
  }

  /**
   * The key is not blank and unique among the test plan.
   */
  public String key() {
    return (String) element().getProperty("key");
  }

  public MutableTestCase setKey(String s) {
    element().setProperty("key", s);
    return this;
  }

  public String name() {
    return (String) element().getProperty("name");
  }

  public MutableTestCase setName(String s) {
    element().setProperty("name", s);
    return this;
  }

  public String message() {
    return (String) element().getProperty("message");
  }

  public MutableTestCase setMessage(String s) {
    element().setProperty("message", s);
    return this;
  }

  public String stackTrace() {
    return (String) element().getProperty("stackTrace");
  }

  public MutableTestCase setStackTrace(String s) {
    element().setProperty("stackTrace", s);
    return this;
  }

  public void covers(Testable component, Collection<Integer> lines) {

  }

  public TestPlan testPlan() {
    Vertex plan = GraphUtil.singleAdjacent(element(), Direction.IN, "testcase");
    return graph().wrap(plan, DefaultTestPlan.class);
  }

  public Collection<CoveredTestable> coveredBlocks() {
    return null;
  }
}
