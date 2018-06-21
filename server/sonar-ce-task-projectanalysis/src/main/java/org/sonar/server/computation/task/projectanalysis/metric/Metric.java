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

import javax.annotation.CheckForNull;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

public interface Metric {
  /**
   * The metric's id (ie. its database identifier)
   */
  int getId();

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
   * @throws IllegalStateException if the value type is not decimal (see {@link org.sonar.server.computation.task.projectanalysis.measure.Measure.ValueType}
   */
  int getDecimalScale();

  enum MetricType {
    INT(Measure.ValueType.INT),
    MILLISEC(Measure.ValueType.LONG),
    RATING(Measure.ValueType.INT),
    WORK_DUR(Measure.ValueType.LONG),
    FLOAT(Measure.ValueType.DOUBLE),
    PERCENT(Measure.ValueType.DOUBLE),
    BOOL(Measure.ValueType.BOOLEAN),
    STRING(Measure.ValueType.STRING),
    DISTRIB(Measure.ValueType.STRING),
    DATA(Measure.ValueType.STRING),
    LEVEL(Measure.ValueType.LEVEL);

    private final Measure.ValueType valueType;

    MetricType(Measure.ValueType valueType) {
      this.valueType = valueType;
    }

    public Measure.ValueType getValueType() {
      return valueType;
    }
  }
}
