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

package org.sonar.server.computation.design;

import org.sonar.graph.Edge;

import java.io.Serializable;

public class FileDependency implements Edge<Integer>, Serializable {

  private Integer from;
  private Integer to;
  private int weight;

  public FileDependency(Integer from, Integer to, int weight) {
    this.from = from;
    this.to = to;
    // TODO fail when weight < 1
    this.weight = weight;
  }

  @Override
  public Integer getFrom() {
    return from;
  }

  @Override
  public Integer getTo() {
    return to;
  }

  @Override
  public int getWeight() {
    return weight;
  }

}
