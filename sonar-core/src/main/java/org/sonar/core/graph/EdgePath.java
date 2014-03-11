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
package org.sonar.core.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.tinkerpop.blueprints.Direction;

import java.util.List;

public class EdgePath {
  private List<Object> elements;

  private EdgePath(Object[] elements) {
    Preconditions.checkArgument(elements != null && elements.length > 0, "Elements can't be null or empty");
    Preconditions.checkArgument(elements.length % 2 == 0, "Odd number of elements (" + elements.length + ")");

    for (int i = 0; i < elements.length; i++) {
      if (i % 2 == 0) {
        Preconditions.checkArgument(elements[i] instanceof Direction,
          "Element " + i + " must be a " + Direction.class.getName() + " (got " + elements[i].getClass().getName() + ")");
      } else {
        Preconditions.checkArgument(elements[i] instanceof String,
          "Element " + i + " must be a String" + " (got " + elements[i].getClass().getName() + ")");
      }
    }

    this.elements = ImmutableList.copyOf(elements);
  }

  public List<Object> getElements() {
    return elements;
  }

  public static EdgePath create(Object... elements) {
    return new EdgePath(elements);
  }
}
