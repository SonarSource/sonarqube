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
package org.sonar.server.measure;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

class MeasureFilterSort {
  private Field field = Field.NAME;
  private Metric metric = null;
  private Integer period = null;
  private boolean asc = true;
  MeasureFilterSort() {
  }

  void setField(Field field) {
    this.field = field;
  }

  void setMetric(Metric metric) {
    this.field = Field.METRIC;
    this.metric = metric;
  }

  Integer period() {
    return period;
  }

  void setPeriod(Integer period) {
    this.period = period;
  }

  public Field field() {
    return field;
  }

  boolean onMeasures() {
    return field == Field.METRIC;
  }

  Metric metric() {
    return metric;
  }

  boolean isOnMeasure() {
    return metric != null;
  }

  boolean isOnNumericMeasure() {
    return metric != null && metric.isNumericType();
  }

  boolean isOnDate() {
    return Field.PROJECT_CREATION_DATE.equals(field);
  }

  boolean isOnTime() {
    return Field.DATE.equals(field);
  }

  boolean isOnAlert() {
    return metric != null && metric.getKey().equals(CoreMetrics.ALERT_STATUS_KEY);
  }

  boolean isAsc() {
    return asc;
  }

  void setAsc(boolean asc) {
    this.asc = asc;
  }

  String column() {
    // only numeric metrics can be sorted by database, else results are sorted programmatically.
    String column;
    switch (field) {
      case KEY:
        column = "p.kee";
        break;
      case NAME:
        column = "p.long_name";
        break;
      case SHORT_NAME:
        column = "p.name";
        break;
      case DESCRIPTION:
        column = "p.description";
        break;
      case VERSION:
        column = "s.version";
        break;
      case DATE:
        column = "s.created_at";
        break;
      case PROJECT_CREATION_DATE:
        column = "p.created_at";
        break;
      case METRIC:
        column = getMetricColumn();
        break;
      default:
        throw new IllegalArgumentException("Unsupported sorting: " + field);
    }
    return column;
  }

  private String getMetricColumn() {
    if (metric.isNumericType()) {
      return (period != null) ? ("pmsort.variation_value_" + period) : "pmsort.value";
    } else {
      return "pmsort.text_value";
    }
  }

  public static enum Field {
    KEY, NAME, VERSION, METRIC, SHORT_NAME, DESCRIPTION,
    // Sort by last analysis date
    DATE,
    // Sort by project creation date
    PROJECT_CREATION_DATE
  }
}
