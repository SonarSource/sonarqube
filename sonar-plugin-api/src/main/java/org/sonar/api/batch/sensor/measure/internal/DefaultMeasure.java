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
package org.sonar.api.batch.sensor.measure.internal;

import org.sonar.api.batch.sensor.internal.SensorStorage;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;

public class DefaultMeasure<G extends Serializable> extends DefaultStorable implements Measure<G>, NewMeasure<G> {

  private boolean onProject = false;
  private InputFile file;
  private Metric<G> metric;
  private G value;
  private boolean fromCore = false;

  public DefaultMeasure() {
    super();
  }

  public DefaultMeasure(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultMeasure<G> onFile(InputFile inputFile) {
    Preconditions.checkState(!this.onProject, "onProject already called");
    Preconditions.checkState(this.file == null, "onFile already called");
    Preconditions.checkNotNull(inputFile, "InputFile should be non null");
    this.file = inputFile;
    return this;
  }

  @Override
  public DefaultMeasure<G> onProject() {
    Preconditions.checkState(!this.onProject, "onProject already called");
    Preconditions.checkState(this.file == null, "onFile already called");
    this.onProject = true;
    return this;
  }

  @Override
  public DefaultMeasure<G> forMetric(Metric<G> metric) {
    Preconditions.checkState(metric != null, "Metric already defined");
    Preconditions.checkNotNull(metric, "metric should be non null");
    this.metric = metric;
    return this;
  }

  @Override
  public DefaultMeasure<G> withValue(G value) {
    Preconditions.checkState(this.value == null, "Measure value already defined");
    Preconditions.checkNotNull(value, "Measure value can't be null");
    this.value = value;
    return this;
  }

  /**
   * For internal use.
   */
  public boolean isFromCore() {
    return fromCore;
  }

  /**
   * For internal use. Used by core components to bypass check that prevent a plugin to store core measures.
   */
  public DefaultMeasure<G> setFromCore() {
    this.fromCore = true;
    return this;
  }

  @Override
  public void doSave() {
    Preconditions.checkNotNull(this.value, "Measure value can't be null");
    Preconditions.checkNotNull(this.metric, "Measure metric can't be null");
    Preconditions.checkState(this.metric.valueType().equals(this.value.getClass()), "Measure value should be of type " + this.metric.valueType());
    storage.store(this);
  }

  @Override
  public Metric<G> metric() {
    return metric;
  }

  @Override
  @CheckForNull
  public InputFile inputFile() {
    return file;
  }

  @Override
  public G value() {
    return value;
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
    DefaultMeasure rhs = (DefaultMeasure) obj;
    return new EqualsBuilder()
      .append(file, rhs.file)
      .append(metric, rhs.metric)
      .append(value, rhs.value)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(27, 45).
      append(file).
      append(metric).
      append(value).
      toHashCode();
  }

}
