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
package org.sonar.graph;

import java.util.List;

public class Cycle {

  private Edge[] edges;
  private int hashcode = 0;

  public Cycle(List<Edge> edges) {
    this.edges = edges.toArray(new Edge[edges.size()]);
    for(Edge edge : edges) {
      hashcode += edge.hashCode();
    }
  }

  public int size() {
    return edges.length;
  }

  public boolean contains(Edge e) {
    for (Edge edge : edges) {
      if (edge.equals(e)) {
        return true;
      }
    }
    return false;
  }

  public Edge[] getEdges() {
    return edges;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("Cycle with " + size() + " edges : ");
    for (Edge edge : edges) {
      builder.append(edge.getFrom()).append(" -> ");
    }
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof Cycle) {
      Cycle otherCycle = (Cycle) object;
      if (otherCycle.hashcode == hashcode && otherCycle.edges.length == edges.length) {
        mainLoop: for (Edge otherEdge : otherCycle.edges) {
          for (Edge edge : edges) {
            if (otherEdge.equals(edge)) {
              continue mainLoop;
            }
          }
          return false;
        }
        return true;
      }
    }
    return false;
  }
}
