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
package org.sonar.api.batch.sensor.dependency.internal;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.dependency.Dependency;

import javax.annotation.Nullable;

public class DefaultDependency implements Dependency {

  private final SensorStorage storage;
  private InputFile from;
  private InputFile to;
  private int weight = 1;
  private boolean saved = false;

  public DefaultDependency() {
    this.storage = null;
  }

  public DefaultDependency(@Nullable SensorStorage storage) {
    this.storage = storage;
  }

  @Override
  public Dependency from(InputFile from) {
    Preconditions.checkNotNull(from, "InputFile should be non null");
    this.from = from;
    return this;
  }

  @Override
  public Dependency to(InputFile to) {
    Preconditions.checkNotNull(to, "InputFile should be non null");
    this.to = to;
    return this;
  }

  @Override
  public Dependency weight(int weight) {
    Preconditions.checkArgument(weight > 1, "weight should be greater than 1");
    this.weight = weight;
    return this;
  }

  @Override
  public void save() {
    Preconditions.checkNotNull(this.storage, "No persister on this object");
    Preconditions.checkState(!saved, "This dependency was already saved");
    Preconditions.checkState(!this.from.equals(this.to), "From and To can't be the same inputFile");
    Preconditions.checkNotNull(this.from, "From inputFile can't be null");
    Preconditions.checkNotNull(this.to, "To inputFile can't be null");
    storage.store((Dependency) this);
    this.saved = true;
  }

  @Override
  public InputFile from() {
    return this.from;
  }

  @Override
  public InputFile to() {
    return this.to;
  }

  @Override
  public int weight() {
    return this.weight;
  }

  // For testing purpose

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    DefaultDependency rhs = (DefaultDependency) obj;
    return new EqualsBuilder()
      .append(from, rhs.from)
      .append(to, rhs.to)
      .append(weight, rhs.weight)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(27, 45).
      append(from).
      append(to).
      append(weight).
      toHashCode();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
