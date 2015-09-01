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
   * @throws IllegalStateException if the value type of the metric is not a integer. See {@link org.sonar.api.measures.Metric.ValueType}
   */
  int getIntValue();

  /**
   * The value of this measure as a long.
   *
   * @throws IllegalStateException if the value type of the metric is not a long. See {@link org.sonar.api.measures.Metric.ValueType}
   */
  long getLongValue();

  /**
   * The value of this measure as a double.
   *
   * @throws IllegalStateException if the value type of the metric is not a double. See {@link org.sonar.api.measures.Metric.ValueType}
   */
  double getDoubleValue();

  /**
   * The value of this measure as a string.
   *
   * @throws IllegalStateException if the value type of the metric is not a string. See {@link org.sonar.api.measures.Metric.ValueType}
   */
  String getStringValue();

  /**
   * The value of this measure as a boolean.
   *
   * @throws IllegalStateException if the value type of the metric is not a boolean. See {@link org.sonar.api.measures.Metric.ValueType}
   */
  boolean getBooleanValue();

}
