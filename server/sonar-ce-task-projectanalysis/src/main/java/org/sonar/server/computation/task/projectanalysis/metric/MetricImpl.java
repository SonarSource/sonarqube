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
package org.sonar.server.computation.task.projectanalysis.metric;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Immutable
public final class MetricImpl implements Metric {

  private final int id;
  private final String key;
  private final String name;
  private final MetricType type;
  private final Integer decimalScale;
  private final Double bestValue;
  private final boolean bestValueOptimized;

  public MetricImpl(int id, String key, String name, MetricType type) {
    this(id, key, name, type, null, null, false);
  }

  public MetricImpl(int id, String key, String name, MetricType type, @Nullable Integer decimalScale,
    @Nullable Double bestValue, boolean bestValueOptimized) {
    checkArgument(!bestValueOptimized || bestValue != null, "A BestValue must be specified if Metric is bestValueOptimized");
    this.id = id;
    this.key = checkNotNull(key);
    this.name = checkNotNull(name);
    this.type = checkNotNull(type);
    if (type.getValueType() == Measure.ValueType.DOUBLE) {
      this.decimalScale = firstNonNull(decimalScale, org.sonar.api.measures.Metric.DEFAULT_DECIMAL_SCALE);
    } else {
      this.decimalScale = decimalScale;
    }
    this.bestValueOptimized = bestValueOptimized;
    this.bestValue = bestValue;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public MetricType getType() {
    return type;
  }

  @Override
  public int getDecimalScale() {
    checkState(decimalScale != null, "Decimal scale is not defined on metric %s", key);
    return decimalScale;
  }

  @Override
  @CheckForNull
  public Double getBestValue() {
    return bestValue;
  }

  @Override
  public boolean isBestValueOptimized() {
    return bestValueOptimized;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetricImpl metric = (MetricImpl) o;
    return Objects.equals(key, metric.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("id", id)
      .add("key", key)
      .add("name", name)
      .add("type", type)
      .add("bestValue", bestValue)
      .add("bestValueOptimized", bestValueOptimized)
      .toString();
  }
}
