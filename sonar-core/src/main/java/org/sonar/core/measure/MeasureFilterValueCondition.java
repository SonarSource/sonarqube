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

import org.sonar.api.measures.Metric;

public class MeasureFilterValueCondition {

  public static enum Operator {
    GREATER(">"), GREATER_OR_EQUALS(">="), EQUALS("="), LESS("<"), LESS_OR_EQUALS("<=");

    private String sql;

    private Operator(String sql) {
      this.sql = sql;
    }

    public String getSql() {
      return sql;
    }
  }

  private final Metric metric;
  private final Operator operator;
  private final float value;
  private int period = -1;

  public MeasureFilterValueCondition(Metric metric, Operator operator, float value) {
    this.metric = metric;
    this.operator = operator;
    this.value = value;
  }

  public MeasureFilterValueCondition setPeriod(int period) {
    this.period = (period > 0 ? period : -1);
    return this;
  }

  public Metric metric() {
    return metric;
  }

  public Operator operator() {
    return operator;
  }

  public double value() {
    return value;
  }

  public int period() {
    return period;
  }

  String valueColumn() {
    if (period > 0) {
      return "pm.variation_value_" + period;
    }
    return "pm.value";
  }

  void appendSql(StringBuilder sql) {
    sql.append(" pm.metric_id=");
    sql.append(metric.getId());
    sql.append(" AND ").append(valueColumn()).append(operator.getSql()).append(value);
  }
}
