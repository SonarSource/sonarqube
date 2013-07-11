/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.measure;

import org.sonar.api.measures.Metric;

class MeasureFilterSort {
  public static enum Field {
    KEY, NAME, VERSION, LANGUAGE, METRIC, SHORT_NAME, DESCRIPTION,
    DATE,  // Sort by last analysis date
    PROJECT_CREATION_DATE // Sort by project creation date
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

  Integer period() {
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

  boolean isOnMeasure() {
    return metric != null;
  }

  boolean isOnNumericMeasure() {
    return metric != null && metric.isNumericType();
  }

  boolean isOnDate() {
    return Field.DATE.equals(field) || Field.PROJECT_CREATION_DATE.equals(field);
  }

  boolean isAsc() {
    return asc;
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
      case LANGUAGE:
        column = "p.language";
        break;
      case DATE:
        column = "s.created_at";
        break;
      case PROJECT_CREATION_DATE:
        column = "p.created_at";
        break;
      case METRIC:
        if (metric.isNumericType()) {
          column = (period != null ? "pmsort.variation_value_" + period : "pmsort.value");
        } else {
          column = "pmsort.text_value";
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported sorting: " + field);
    }
    return column;
  }
}
