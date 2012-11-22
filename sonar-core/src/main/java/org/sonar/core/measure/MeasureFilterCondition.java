/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.measures.Metric;

public class MeasureFilterCondition {

  private final Metric metric;
  private final String operator;
  private final double value;
  private Integer period = null;

  public MeasureFilterCondition(Metric metric, String operator, double value) {
    this.metric = metric;
    this.operator = operator;
    this.value = value;
  }

  public MeasureFilterCondition setPeriod(Integer period) {
    this.period = period;
    return this;
  }

  public Metric metric() {
    return metric;
  }

  public String operator() {
    return operator;
  }

  public double value() {
    return value;
  }

  public Integer period() {
    return period;
  }

  String valueColumn() {
    if (period != null) {
      return "pm.variation_value_" + period;
    }
    return "pm.value";
  }

  void appendSqlCondition(StringBuilder sql) {
    sql.append(" pm.metric_id=");
    sql.append(metric.getId());
    sql.append(" AND ").append(valueColumn()).append(operator).append(value);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
  }
}
