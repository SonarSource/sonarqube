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

class MeasureFilterSort {
  public static enum Field {
    KEY, NAME, VERSION, LANGUAGE, DATE, METRIC
  }

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

  Integer getPeriod() {
    return period;
  }

  void setPeriod(Integer period) {
    this.period = period;
  }

  void setAsc(boolean asc) {
    this.asc = asc;
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

  boolean isSortedByDatabase() {
    return metric != null && metric.isNumericType();
  }

  boolean isAsc() {
    return asc;
  }

  String column() {
    // only numeric metrics can be sorted by database, else results are sorted programmatically.
    String column = null;
    switch (field) {
      case KEY:
        column = "p.kee";
        break;
      case NAME:
        column = "p.long_name";
        break;
      case VERSION:
        column = "s.version";
        break;
      case LANGUAGE:
        column = "p.language";
        break;
      case DATE:
        column = "s.created_at";
        break;
      case METRIC:
        if (metric.isNumericType()) {
          column = (period != null ? "pm.variation_value_" + period : "pm.value");
        } else {
          column = "pm.text_value";
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported sorting: " + field);
    }
    return column;
  }

}
