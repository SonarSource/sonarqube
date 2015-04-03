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

import org.sonar.api.batch.sensor.internal.SensorStorage;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.dependency.Dependency;
import org.sonar.api.batch.sensor.dependency.NewDependency;
import org.sonar.api.batch.sensor.internal.DefaultStorable;

import javax.annotation.Nullable;

public class DefaultDependency extends DefaultStorable implements Dependency, NewDependency {

  private String fromKey;
  private String toKey;
  private int weight = 1;

  public DefaultDependency() {
    super(null);
  }

  public DefaultDependency(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultDependency from(InputFile from) {
    Preconditions.checkNotNull(from, "InputFile should be non null");
    this.fromKey = ((DefaultInputFile) from).key();
    return this;
  }

  @Override
  public DefaultDependency to(InputFile to) {
    Preconditions.checkNotNull(to, "InputFile should be non null");
    this.toKey = ((DefaultInputFile) to).key();
    return this;
  }

  @Override
  public DefaultDependency weight(int weight) {
    Preconditions.checkArgument(weight > 1, "weight should be greater than 1");
    this.weight = weight;
    return this;
  }

  @Override
  public void doSave() {
    Preconditions.checkState(!this.fromKey.equals(this.toKey), "From and To can't be the same inputFile");
    Preconditions.checkNotNull(this.fromKey, "From inputFile can't be null");
    Preconditions.checkNotNull(this.toKey, "To inputFile can't be null");
    storage.store(this);
  }

  @Override
  public String fromKey() {
    return this.fromKey;
  }

  public DefaultDependency setFromKey(String fromKey) {
    this.fromKey = fromKey;
    return this;
  }

  @Override
  public String toKey() {
    return this.toKey;
  }

  public DefaultDependency setToKey(String toKey) {
    this.toKey = toKey;
    return this;
  }

  @Override
  public int weight() {
    return this.weight;
  }

  public DefaultDependency setWeight(int weight) {
    this.weight = weight;
    return this;
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
      .append(fromKey, rhs.fromKey)
      .append(toKey, rhs.toKey)
      .append(weight, rhs.weight)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(27, 45).
      append(fromKey).
      append(toKey).
      append(weight).
      toHashCode();
  }

}
