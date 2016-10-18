/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.component.es;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.stream;

public class ProjectMeasuresQuery {
  private List<MetricCriteria> metricCriteria = new ArrayList<>();

  public ProjectMeasuresQuery addMetricCriterion(MetricCriteria metricCriteria) {
    this.metricCriteria.add(metricCriteria);
    return this;
  }

  public List<MetricCriteria> getMetricCriteria() {
    return metricCriteria;
  }

  public enum Operator {
    LTE("<="), GT(">");

    String value;

    Operator(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }

    public static Operator getByValue(String value) {
      return stream(Operator.values())
        .filter(operator -> operator.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("Unknown operator '%s'", value)));
    }
  }

  public static class MetricCriteria {
    private final String metricKey;
    private final Operator operator;
    private final double value;

    public MetricCriteria(String metricKey, Operator operator, double value) {
      this.metricKey = metricKey;
      this.operator = operator;
      this.value = value;
    }

    public String getMetricKey() {
      return metricKey;
    }

    public Operator getOperator() {
      return operator;
    }

    public double getValue() {
      return value;
    }
  }
}
