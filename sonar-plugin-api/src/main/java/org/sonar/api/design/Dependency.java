/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
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
  public boolean equals(Object obj) {
    if (!(obj instanceof Dependency)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Dependency other = (Dependency) obj;
    return new EqualsBuilder()
      .append(from, other.from)
      .append(to, other.to)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(from)
      .append(to)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("from", from)
      .append("to", to)
      .append("weight", weight)
      .append("usage", usage)
      .toString();
  }
}
