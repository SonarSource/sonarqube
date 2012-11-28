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
    String valueToEval = getValueToEval(alert, alertLevel);
    if (StringUtils.isEmpty(valueToEval)) {
      return false;
    }

    Comparable criteriaValue = getValueForComparison(alert.getMetric(), valueToEval);
    Comparable metricValue = getMeasureValue(alert, measure);

    int comparison = metricValue.compareTo(criteriaValue);
    return !(// NOSONAR complexity of this boolean expression is under control
        (alert.isNotEqualsOperator() && comparison == 0)
            || (alert.isGreaterOperator() && comparison != 1)
            || (alert.isSmallerOperator() && comparison != -1)
            || (alert.isEqualsOperator() && comparison != 0));
  }

  private static String getValueToEval(Alert alert, Metric.Level alertLevel) {
    if (alertLevel.equals(Metric.Level.ERROR)) {
      return alert.getValueError();
    } else if (alertLevel.equals(Metric.Level.WARN)) {
      return alert.getValueWarning();
    } else {
      throw new IllegalStateException(alertLevel.toString());
    }
  }

  private static Comparable<?> getValueForComparison(Metric metric, String value) {
    if (metric.getType() == Metric.ValueType.FLOAT ||
        metric.getType() == Metric.ValueType.PERCENT ||
        metric.getType() == Metric.ValueType.RATING
        ) {
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
      return Integer.parseInt(value);
    }
    throw new NotImplementedException(metric.getType().toString());
  }

  private static Comparable<?> getMeasureValue(Alert alert, Measure measure) {
    Metric metric = alert.getMetric();
    if (metric.getType() == Metric.ValueType.FLOAT ||
        metric.getType() == Metric.ValueType.PERCENT ||
        metric.getType() == Metric.ValueType.RATING) {
      return getValue(alert, measure);
    }
    if (metric.getType() == Metric.ValueType.INT ||
        metric.getType() == Metric.ValueType.MILLISEC) {
      return getValue(alert, measure).intValue();
    }
    if (alert.getPeriod() == null) {
      if (metric.getType() == Metric.ValueType.STRING ||
          metric.getType() == Metric.ValueType.LEVEL) {
        return measure.getData();
      }
      if (metric.getType() == Metric.ValueType.BOOL) {
        return measure.getValue().intValue();
      }
    }
    throw new NotImplementedException(metric.getType().toString());
  }

  private static Double getValue(Alert alert, Measure measure) {
    if (alert.getPeriod() == null) {
      return measure.getValue();
    } else if (alert.getPeriod() == 1) {
      return measure.getVariation1();
    } else if (alert.getPeriod() == 2) {
      return measure.getVariation2();
    } else if (alert.getPeriod() == 3) {
      return measure.getVariation3();
    } else {
      throw new IllegalStateException("Following index period is not allowed : " + Double.toString(alert.getPeriod()));
    }
  }
}
