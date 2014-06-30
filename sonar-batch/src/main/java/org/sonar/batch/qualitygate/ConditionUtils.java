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
package org.sonar.batch.qualitygate;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;

class ConditionUtils {

  private ConditionUtils() {
    // only static stuff
  }

  /**
   * Get the matching alert level for the given measure
   */
  static Metric.Level getLevel(ResolvedCondition condition, Measure measure) {
    if (evaluateCondition(condition, measure, Metric.Level.ERROR)) {
      return Metric.Level.ERROR;
    }
    if (evaluateCondition(condition, measure, Metric.Level.WARN)) {
      return Metric.Level.WARN;
    }
    return Metric.Level.OK;
  }

  private static boolean evaluateCondition(ResolvedCondition condition, Measure measure, Metric.Level alertLevel) {
    String valueToEval = getValueToEval(condition, alertLevel);
    if (StringUtils.isEmpty(valueToEval)) {
      return false;
    }

    Comparable criteriaValue = getValueForComparison(condition.metric(), valueToEval);
    Comparable measureValue = getMeasureValue(condition, measure);
    if (measureValue != null) {
      return doesReachThresholds(measureValue, criteriaValue, condition);
    }
    return false;
  }

  private static boolean doesReachThresholds(Comparable measureValue, Comparable criteriaValue, ResolvedCondition condition) {
    int comparison = measureValue.compareTo(criteriaValue);
    return !(isNotEquals(comparison, condition)
      || isGreater(comparison, condition)
      || isSmaller(comparison, condition)
      || isEquals(comparison, condition));
  }

  private static boolean isNotEquals(int comparison, ResolvedCondition condition) {
    return "NE".equals(condition.operator()) && comparison == 0;
  }

  private static boolean isGreater(int comparison, ResolvedCondition condition) {
    return "GT".equals(condition.operator()) && comparison != 1;
  }

  private static boolean isSmaller(int comparison, ResolvedCondition condition) {
    return "LT".equals(condition.operator()) && comparison != -1;
  }

  private static boolean isEquals(int comparison, ResolvedCondition condition) {
    return "EQ".equals(condition.operator()) && comparison != 0;
  }

  private static String getValueToEval(ResolvedCondition condition, Metric.Level alertLevel) {
    if (alertLevel.equals(Metric.Level.ERROR)) {
      return condition.errorThreshold();
    } else if (alertLevel.equals(Metric.Level.WARN)) {
      return condition.warningThreshold();
    } else {
      throw new IllegalStateException(alertLevel.toString());
    }
  }

  private static Comparable getValueForComparison(Metric metric, String value) {
    Comparable valueToCompare = null;
    try {
      if (isADouble(metric)) {
        valueToCompare = Double.parseDouble(value);
      } else if (isAInteger(metric)) {
        valueToCompare = parseInteger(value);
      } else if (isAString(metric)) {
        valueToCompare = value;
      } else if (isABoolean(metric)) {
        valueToCompare = Integer.parseInt(value);
      } else if (isAWorkDuration(metric)) {
        valueToCompare = Long.parseLong(value);
      } else {
        throw new NotImplementedException(metric.getType().toString());
      }
    } catch (NumberFormatException badValueFormat) {
      throw new IllegalArgumentException(String.format("Quality Gate: Unable to parse value '%s' to compare against %s", value, metric.getName()));
    }
    return valueToCompare;
  }

  private static Comparable<Integer> parseInteger(String value) {
    return value.contains(".") ? Integer.parseInt(value.substring(0, value.indexOf('.'))) : Integer.parseInt(value);
  }

  private static Comparable getMeasureValue(ResolvedCondition condition, Measure measure) {
    Metric metric = condition.metric();
    if (isADouble(metric)) {
      return getValue(condition, measure);
    }
    if (isAInteger(metric)) {
      return parseInteger(condition, measure);
    }
    if (isAWorkDuration(metric)) {
      return parseLong(condition, measure);
    }
    if (condition.period() == null) {
      return getMeasureValueForStringOrBoolean(metric, measure);
    }
    throw new NotImplementedException(metric.getType().toString());
  }

  private static Comparable getMeasureValueForStringOrBoolean(Metric metric, Measure measure) {
    if (isAString(metric)) {
      return measure.getData();
    }
    if (isABoolean(metric)) {
      return measure.getValue().intValue();
    }
    throw new NotImplementedException(metric.getType().toString());
  }

  private static Comparable<Integer> parseInteger(ResolvedCondition condition, Measure measure) {
    Double value = getValue(condition, measure);
    return value != null ? value.intValue() : null;
  }

  private static Comparable<Long> parseLong(ResolvedCondition condition, Measure measure) {
    Double value = getValue(condition, measure);
    return value != null ? value.longValue() : null;
  }

  private static boolean isADouble(Metric metric) {
    return metric.getType() == Metric.ValueType.FLOAT ||
      metric.getType() == Metric.ValueType.PERCENT ||
      metric.getType() == Metric.ValueType.RATING;
  }

  private static boolean isAInteger(Metric metric) {
    return metric.getType() == Metric.ValueType.INT ||
      metric.getType() == Metric.ValueType.MILLISEC;
  }

  private static boolean isAString(Metric metric) {
    return metric.getType() == Metric.ValueType.STRING ||
      metric.getType() == Metric.ValueType.LEVEL;
  }

  private static boolean isABoolean(Metric metric) {
    return metric.getType() == Metric.ValueType.BOOL;
  }

  private static boolean isAWorkDuration(Metric metric) {
    return metric.getType() == Metric.ValueType.WORK_DUR;
  }

  static Double getValue(ResolvedCondition condition, Measure measure) {
    Integer period = condition.period();
    Double value;
    if (period == null) {
      value = measure.getValue();
    } else {
      switch (period.intValue()) {
        case 1:
          value = measure.getVariation1();
          break;
        case 2:
          value = measure.getVariation2();
          break;
        case 3:
          value = measure.getVariation3();
          break;
        case 4:
          value = measure.getVariation4();
          break;
        case 5:
          value = measure.getVariation5();
          break;
        default:
          throw new IllegalStateException("Following index period is not allowed : " + Double.toString(period));
      }
    }
    return value;
  }
}
