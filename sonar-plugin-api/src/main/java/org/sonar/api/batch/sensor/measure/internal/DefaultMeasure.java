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
package org.sonar.api.batch.sensor.measure.internal;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;

import static java.util.Objects.requireNonNull;

public class DefaultMeasure<G extends Serializable> extends DefaultStorable implements Measure<G>, NewMeasure<G> {

  private InputComponent component;
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
  public DefaultMeasure<G> on(InputComponent component) {
    Preconditions.checkArgument(component != null, "Component can't be null");
    Preconditions.checkState(this.component == null, "on() already called");
    this.component = component;
    return this;
  }

  @Override
  public DefaultMeasure<G> forMetric(Metric<G> metric) {
    Preconditions.checkState(this.metric == null, "Metric already defined");
    requireNonNull(metric, "metric should be non null");
    this.metric = metric;
    return this;
  }

  @Override
  public DefaultMeasure<G> withValue(G value) {
    Preconditions.checkState(this.value == null, "Measure value already defined");
    requireNonNull(value, "Measure value can't be null");
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
    requireNonNull(this.value, "Measure value can't be null");
    requireNonNull(this.metric, "Measure metric can't be null");
    Preconditions.checkState(this.metric.valueType().equals(this.value.getClass()), "Measure value should be of type %s", this.metric.valueType());
    storage.store(this);
  }

  @Override
  public Metric<G> metric() {
    return metric;
  }

  @Override
  public InputComponent inputComponent() {
    return component;
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
    DefaultMeasure<?> rhs = (DefaultMeasure<?>) obj;
    return new EqualsBuilder()
      .append(component, rhs.component)
      .append(metric, rhs.metric)
      .append(value, rhs.value)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(27, 45).append(component).append(metric).append(value).toHashCode();
  }

}
