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

import org.apache.commons.lang.builder.ToStringBuilder;

public class StringEdge implements Edge<String> {

  private final String from;
  private final String to;
  private int weight;

  public StringEdge(String from, String to) {
    this.from = from;
    this.to = to;
    this.weight = 1;
  }

  public StringEdge(String from, String to, int weight) {
    this(from, to);
    this.weight = weight;
  }

  @Override
  public String getFrom() {
    return from;
  }

  @Override
  public String getTo() {
    return to;
  }

  @Override
  public int getWeight() {
    return weight;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof StringEdge)) {
      return false;
    }
    StringEdge edge = (StringEdge) obj;
    return from.equals(edge.from) && to.equals(edge.to);
  }

  @Override
  public int hashCode() {
    return 3*from.hashCode() + to.hashCode(); //NOSONAR Magic number 3 is suitable here
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("from", from).append("to", to).toString();
  }
}
