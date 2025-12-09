/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.metric;

import javax.annotation.CheckForNull;
import org.sonar.ce.task.projectanalysis.measure.ValueType;

public interface Metric {
  /**
   * The metric's uuid (ie. its database identifier)
   */
  String getUuid();

  /**
   * The Metric's key is its domain identifier.
   */
  String getKey();

  String getName();

  MetricType getType();

  /**
   * When Metric is "bestValueOptimized" _and_ the component it belongs to is a FILE, any measure which has the same
   * value as the best value of the metric should _not_ be persisted into the DB to save on DB usage.
   */
  boolean isBestValueOptimized();

  /**
   * The best value for the current Metric, if there is any
   */
  @CheckForNull
  Double getBestValue();

  /**
   * The decimal scale of float measures. Returned value is greater than or equal zero.
   * @throws IllegalStateException if the value type is not decimal (see {@link ValueType}
   */
  int getDecimalScale();

  boolean isDeleteHistoricalData();

  enum MetricType {
    INT(ValueType.INT),
    MILLISEC(ValueType.LONG),
    RATING(ValueType.INT),
    WORK_DUR(ValueType.LONG),
    FLOAT(ValueType.DOUBLE),
    PERCENT(ValueType.DOUBLE),
    BOOL(ValueType.BOOLEAN),
    STRING(ValueType.STRING),
    DISTRIB(ValueType.STRING),
    DATA(ValueType.STRING),
    LEVEL(ValueType.LEVEL);

    private final ValueType valueType;

    MetricType(ValueType valueType) {
      this.valueType = valueType;
    }

    public ValueType getValueType() {
      return valueType;
    }
  }
}
