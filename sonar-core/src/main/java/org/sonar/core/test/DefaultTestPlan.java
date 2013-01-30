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

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.component.Component;
import org.sonar.api.test.exception.TestCaseAlreadyExistsException;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

import javax.annotation.CheckForNull;

public class DefaultTestPlan extends BeanVertex implements MutableTestPlan {
  public Component component() {
    Vertex component = GraphUtil.singleAdjacent(element(), Direction.IN, "testplan");
    return beanGraph().wrap(component, ComponentVertex.class);
  }

  public String type() {
    return (String) getProperty("type");
  }

  public MutableTestPlan setType(String s) {
    setProperty("type", s);
    return this;
  }

  @CheckForNull
  public MutableTestCase testCaseByKey(String key) {
    for (MutableTestCase testCase : testCases()) {
      if (key.equals(testCase.key())) {
        return testCase;
      }
    }
    return null;
  }

  public MutableTestCase addTestCase(String key) {
    if (testCaseByKey(key)!=null) {
      throw new TestCaseAlreadyExistsException(component().key(), key);
    }
    DefaultTestCase testCase = beanGraph().createAdjacentVertex(this, DefaultTestCase.class, "testcase");
    testCase.setKey(key);
    return testCase;
  }

  public Iterable<MutableTestCase> testCases() {
    return (Iterable) getVertices(DefaultTestCase.class, Direction.OUT, "testcase");
  }

}
