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
package org.sonar.plugins.core.sensors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;

public final class AlertUtils {
  private AlertUtils() {
  }

  /**
   * Get the matching alert level for the given measure
   */
  public static Metric.Level getLevel(Alert alert, Measure measure) {
    if (evaluateAlert(alert, measure, Metric.Level.ERROR)) {
      return Metric.Level.ERROR;
    }
    if (evaluateAlert(alert, measure, Metric.Level.WARN)) {
      return Metric.Level.WARN;
    }
    return Metric.Level.OK;
  }

  private static boolean evaluateAlert(Alert alert, Measure measure, Metric.Level alertLevel) {
    String valueToEval;
    if (alertLevel.equals(Metric.Level.ERROR)) {
      valueToEval = alert.getValueError();

    } else if (alertLevel.equals(Metric.Level.WARN)) {
      valueToEval = alert.getValueWarning();
    } else {
      throw new IllegalStateException(alertLevel.toString());
    }
    if (StringUtils.isEmpty(valueToEval)) {
      return false;
    }

    Comparable criteriaValue = getValueForComparison(alert.getMetric(), valueToEval);
    Comparable metricValue = getMeasureValue(alert.getMetric(), measure);

    int comparison = metricValue.compareTo(criteriaValue);
    if (alert.isNotEqualsOperator() && comparison == 0 ||
        alert.isGreaterOperator() && comparison != 1 ||
        alert.isSmallerOperator() && comparison != -1 ||
        alert.isEqualsOperator() && comparison != 0) {
      return false;
    }
    return true;
  }


  private static Comparable<?> getValueForComparison(Metric metric, String value) {
    if (metric.getType() == Metric.ValueType.FLOAT ||
        metric.getType() == Metric.ValueType.PERCENT) {
      return Double.parseDouble(value);
    }
    if (metric.getType() == Metric.ValueType.INT ||
        metric.getType() == Metric.ValueType.MILLISEC) {
      return value.contains(".") ? Integer.parseInt(value.substring(0, value.indexOf('.'))) : Integer.parseInt(value);
    }
    if (metric.getType() == Metric.ValueType.STRING ||
        metric.getType() == Metric.ValueType.LEVEL) {
      return value;
    }
    if (metric.getType() == Metric.ValueType.BOOL) {
      return Boolean.valueOf(value);
    }
    if (metric.getType() == Metric.ValueType.RATING) {
      return Double.parseDouble(value);
    }
    throw new NotImplementedException(metric.getType().toString());
  }

  private static Comparable<?> getMeasureValue(Metric metric, Measure measure) {
    if (metric.getType() == Metric.ValueType.FLOAT ||
        metric.getType() == Metric.ValueType.PERCENT) {
      return measure.getValue();
    }
    if (metric.getType() == Metric.ValueType.INT ||
        metric.getType() == Metric.ValueType.MILLISEC) {
      return measure.getValue().intValue();
    }
    if (metric.getType() == Metric.ValueType.STRING ||
        metric.getType() == Metric.ValueType.LEVEL) {
      return measure.getData();
    }
    if (metric.getType() == Metric.ValueType.BOOL) {
      return measure.getValue() == 0d ? Boolean.FALSE : Boolean.TRUE;
    }
    if (metric.getType() == Metric.ValueType.RATING) {
      return measure.getValue();
    }
    throw new NotImplementedException(metric.getType().toString());
  }
}
