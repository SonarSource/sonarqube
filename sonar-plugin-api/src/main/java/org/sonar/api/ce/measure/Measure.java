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
package org.sonar.api.ce.measure;

/**
 * Measure used in {@link MeasureComputer}
 *
 * @since 5.2
 */
public interface Measure {

  /**
   * The value of this measure as a integer.
   *
   * @throws IllegalStateException if the value type of the metric is not an Integer as defined by {@link org.sonar.api.measures.Metric.ValueType#valueClass}
   *         To find out the value type of a metric, check it's definition (eg., core metrics are defined in {@link org.sonar.api.measures.CoreMetrics}).
   */
  int getIntValue();

  /**
   * The value of this measure as a long.
   *
   * @throws IllegalStateException if the value type of the metric is not a Long as defined by {@link org.sonar.api.measures.Metric.ValueType#valueClass}
   *         To find out the value type of a metric, check it's definition (eg., core metrics are defined in {@link org.sonar.api.measures.CoreMetrics}).
   */
  long getLongValue();

  /**
   * The value of this measure as a double.
   *
   * @throws IllegalStateException if the value type of the metric is not a Double as defined by {@link org.sonar.api.measures.Metric.ValueType#valueClass}.
   *         To find out the value type of a metric, check it's definition (eg., core metrics are defined in {@link org.sonar.api.measures.CoreMetrics}).
   */
  double getDoubleValue();

  /**
   * The value of this measure as a string.
   *
   * @throws IllegalStateException if the value type of the metric is not a String as defined by {@link org.sonar.api.measures.Metric.ValueType#valueClass}
   *         To find out the value type of a metric, check it's definition (eg., core metrics are defined in {@link org.sonar.api.measures.CoreMetrics}).
   */
  String getStringValue();

  /**
   * The value of this measure as a boolean.
   *
   * @throws IllegalStateException if the value type of the metric is not a Boolean as defined by {@link org.sonar.api.measures.Metric.ValueType#valueClass}
   *         To find out the value type of a metric, check it's definition (eg., core metrics are defined in {@link org.sonar.api.measures.CoreMetrics}).
   */
  boolean getBooleanValue();

}
