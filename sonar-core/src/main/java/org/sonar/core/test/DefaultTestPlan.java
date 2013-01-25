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

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.component.Component;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.core.component.ComponentWrapper;
import org.sonar.core.component.ElementWrapper;
import org.sonar.core.graph.GraphUtil;

import java.util.List;

public class DefaultTestPlan extends ElementWrapper<Vertex> implements MutableTestPlan {
  public Component component() {
    Vertex component = GraphUtil.singleAdjacent(element(), Direction.IN, "testplan");
    return graph().wrap(component, ComponentWrapper.class);
  }

  public MutableTestCase addTestCase(String key) {
    return graph().createVertex(this, DefaultTestCase.class, "testcase").setKey(key);
  }

  public List<MutableTestCase> testCases() {
    List<MutableTestCase> testCases = Lists.newArrayList();
    for (Vertex testCaseVertex : element().getVertices(Direction.OUT, "testcase")) {
      testCases.add(graph().wrap(testCaseVertex, DefaultTestCase.class));
    }
    return testCases;
  }
}
