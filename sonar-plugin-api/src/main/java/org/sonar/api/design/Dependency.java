/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.design;

import org.sonar.api.resources.Resource;
import org.sonar.graph.Edge;

/**
 * @deprecated since 5.2 No more design features
 */
@Deprecated
public class Dependency implements Edge<Resource> {

  private Resource from;
  private Resource to;
  private String usage;
  private int weight;
  private Dependency parent;
  private Long id;

  public Dependency(Resource from, Resource to) {
    if (from == null) {
      throw new IllegalArgumentException("Dependency source is null");
    }
    if (to == null) {
      throw new IllegalArgumentException("Dependency target is null");
    }
    this.from = from;
    this.to = to;
  }

  @Override
  public Resource getFrom() {
    return from;
  }

  /**
   * For internal use only
   */
  public void setFrom(Resource from) {
    this.from = from;
  }

  @Override
  public Resource getTo() {
    return to;
  }

  /**
   * For internal use only
   */
  public void setTo(Resource to) {
    this.to = to;
  }

  public String getUsage() {
    return usage;
  }

  public Dependency setUsage(String usage) {
    this.usage = usage;
    return this;
  }

  @Override
  public int getWeight() {
    return weight;
  }

  public Dependency setWeight(int weight) {
    this.weight = weight;
    return this;
  }

  public Dependency getParent() {
    return parent;
  }

  public Dependency setParent(Dependency parent) {
    this.parent = parent;
    return this;
  }

  public Long getId() {
    return id;
  }

  /**
   * Internal use only.
   */
  public Dependency setId(Long id) {
    this.id = id;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Dependency that = (Dependency) o;
    if (!from.equals(that.from)) {
      return false;
    }
    return to.equals(that.to);
  }

  @Override
  public int hashCode() {
    int result = from.hashCode();
    result = 31 * result + to.hashCode();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Dependency{");
    sb.append("from=").append(from);
    sb.append(", to=").append(to);
    sb.append(", usage='").append(usage).append('\'');
    sb.append(", weight=").append(weight);
    sb.append(", parent=").append(parent);
    sb.append(", id=").append(id);
    sb.append('}');
    return sb.toString();
  }
}
