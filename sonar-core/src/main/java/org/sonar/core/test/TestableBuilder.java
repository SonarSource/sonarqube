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
import org.sonar.api.test.MutableTestable;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.component.PerspectiveBuilder;
import org.sonar.core.graph.GraphUtil;

public class TestableBuilder extends PerspectiveBuilder<MutableTestable> {

  static final String PERSPECTIVE_KEY = "testable";

  public TestableBuilder() {
    super(PERSPECTIVE_KEY, MutableTestable.class);
  }

  @Override
  public MutableTestable load(ComponentVertex component) {
    Vertex perspectiveVertex = GraphUtil.singleAdjacent(component.element(), Direction.OUT, PERSPECTIVE_KEY);
    if (perspectiveVertex != null) {
      return component.beanGraph().wrap(perspectiveVertex, DefaultTestable.class);
    }
    return null;
  }

  @Override
  public MutableTestable create(ComponentVertex component) {
    return component.beanGraph().createAdjacentVertex(component, DefaultTestable.class, PERSPECTIVE_KEY);
  }
}
