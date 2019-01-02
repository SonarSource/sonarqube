/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils.dag;

import org.sonar.api.utils.SonarException;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.10
 */
public class Node implements Comparable<Node> {

  private final Object object;
  private int order = 0;
  private boolean seen = false;
  private final List<Node> dependencies = new ArrayList<>();

  public Node(final Object object) {
    if (object == null) {
      throw new SonarException("object can not be null");
    }
    this.object = object;
  }

  public void reset() {
    order = 0;
    seen = false;
  }

  public Object getObject() {
    return object;
  }

  public void addDependency(Node v) {
    if (!equals(v) && !dependencies.contains(v)) {
      dependencies.add(v);
    }
  }

  public void resolveOrder() {
    resolveOrder(toString());
  }

  private int resolveOrder(String path) {
    seen = true;
    try {
      int highOrder = -1;
      for (Node dep : dependencies) {
        if (dep.seen) {
          throw new CyclicDependenciesException(path + " -> " + dep.toString());
        }
        highOrder = Math.max(highOrder, dep.resolveOrder(path + " -> " + dep.toString()));

      }

      order = highOrder + 1;
      return order;

    } finally {
      seen = false;
    }
  }

  public List<Node> getDependencies() {
    return dependencies;
  }

  @Override
  public int compareTo(final Node other) {
    int orderInd = 0;

    if (order < other.order) {
      orderInd = -1;
    } else if (order > other.order) {
      orderInd = 1;
    }

    return orderInd;
  }

  @Override
  public String toString() {
    return object.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Node)) {
      return false;
    }
    return object.equals(((Node) o).getObject());
  }

  @Override
  public int hashCode() {
    return object.hashCode();
  }
}

