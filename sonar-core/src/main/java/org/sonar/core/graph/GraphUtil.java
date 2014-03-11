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

import com.google.common.base.Joiner;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import javax.annotation.CheckForNull;

import java.util.Iterator;

public class GraphUtil {

  private GraphUtil() {
  }

  /**
   * Get adjacent vertex. It assumes that there are only 0 or 1 results.
   *
   * @throws MultipleElementsException if there are more than 1 adjacent vertices with the given criteria.
   */
  @CheckForNull
  public static Vertex singleAdjacent(Vertex from, Direction direction, String... labels) {
    Iterator<Vertex> vertices = from.getVertices(direction, labels).iterator();
    Vertex result = null;
    if (vertices.hasNext()) {
      result = vertices.next();
      if (vertices.hasNext()) {
        throw new MultipleElementsException(String.format("More than one vertex is adjacent to: %s, direction: %s, labels: %s", from, direction, Joiner.on(",").join(labels)));
      }
    }
    return result;
  }

  public static <T extends Element> T single(Iterable<T> iterable) {
    Iterator<T> iterator = iterable.iterator();
    T result = null;
    if (iterator.hasNext()) {
      result = iterator.next();
      if (iterator.hasNext()) {
        throw new MultipleElementsException("More than one element");
      }
    }
    return result;
  }
}
