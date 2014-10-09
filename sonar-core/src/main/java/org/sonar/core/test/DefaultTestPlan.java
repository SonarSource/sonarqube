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

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.component.Component;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

import javax.annotation.CheckForNull;

import java.util.List;

public class DefaultTestPlan extends BeanVertex implements MutableTestPlan {
  private static final String TESTCASE = "testcase";

  @Override
  public Component component() {
    Vertex component = GraphUtil.singleAdjacent(element(), Direction.IN, "testplan");
    return beanGraph().wrap(component, ComponentVertex.class);
  }

  @Override
  @CheckForNull
  public Iterable<MutableTestCase> testCasesByName(String name) {
    List<MutableTestCase> result = Lists.newArrayList();
    for (MutableTestCase testCase : testCases()) {
      if (name.equals(testCase.name())) {
        result.add(testCase);
      }
    }
    return result;
  }

  @Override
  public MutableTestCase addTestCase(String name) {
    DefaultTestCase testCase = beanGraph().createAdjacentVertex(this, DefaultTestCase.class, TESTCASE);
    testCase.setName(name);
    return testCase;
  }

  @Override
  public Iterable<MutableTestCase> testCases() {
    return (Iterable) getVertices(DefaultTestCase.class, Direction.OUT, TESTCASE);
  }

}
