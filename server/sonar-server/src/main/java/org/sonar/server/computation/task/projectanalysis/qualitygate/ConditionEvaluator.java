/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.of;

public final class ConditionEvaluator {

  /**
   * Evaluates the condition for the specified measure
   */
  public EvaluationResult evaluate(Condition condition, Measure measure) {
    checkArgument(condition.getMetric().getType() != Metric.MetricType.DATA, "Conditions on MetricType DATA are not supported");

    Comparable measureComparable = parseMeasure(condition, measure);
    if (measureComparable == null) {
      return new EvaluationResult(Measure.Level.OK, null);
    }

    return evaluateCondition(condition, measureComparable, Measure.Level.ERROR)
      .orElseGet(() -> evaluateCondition(condition, measureComparable, Measure.Level.WARN)
        .orElseGet(() -> new EvaluationResult(Measure.Level.OK, measureComparable)));
  }

  private static Optional<EvaluationResult> evaluateCondition(Condition condition, Comparable<?> measureComparable, Measure.Level alertLevel) {
    String conditionValue = getValueToEval(condition, alertLevel);
    if (StringUtils.isEmpty(conditionValue)) {
      return Optional.empty();
    }

    try {
      Comparable conditionComparable = parseConditionValue(condition.getMetric(), conditionValue);
      if (doesReachThresholds(measureComparable, conditionComparable, condition)) {
        return of(new EvaluationResult(alertLevel, measureComparable));
      }
      return Optional.empty();
    } catch (NumberFormatException badValueFormat) {
      throw new IllegalArgumentException(String.format(
        "Quality Gate: Unable to parse value '%s' to compare against %s",
        conditionValue, condition.getMetric().getName()));
    }
  }

  private static String getValueToEval(Condition condition, Measure.Level alertLevel) {
    if (Measure.Level.ERROR.equals(alertLevel)) {
      return condition.getErrorThreshold();
    } else if (Measure.Level.WARN.equals(alertLevel)) {
      return condition.getWarningThreshold();
    } else {
      throw new IllegalStateException(alertLevel.toString());
    }
  }

  private static boolean doesReachThresholds(Comparable measureValue, Comparable criteriaValue, Condition condition) {
    int comparison = measureValue.compareTo(criteriaValue);
    switch (condition.getOperator()) {
      case EQUALS:
        return comparison == 0;
      case NOT_EQUALS:
        return comparison != 0;
      case GREATER_THAN:
        return comparison > 0;
      case LESS_THAN:
        return comparison < 0;
      default:
        throw new IllegalArgumentException(String.format("Unsupported operator '%s'", condition.getOperator()));
    }
  }

  private static Comparable parseConditionValue(Metric metric, String value) {
    switch (metric.getType().getValueType()) {
      case BOOLEAN:
        return Integer.parseInt(value) == 1;
      case INT:
        return parseInteger(value);
      case LONG:
        return Long.parseLong(value);
      case DOUBLE:
        return Double.parseDouble(value);
      case STRING:
      case LEVEL:
        return value;
      default:
        throw new IllegalArgumentException(String.format("Unsupported value type %s. Can not convert condition value", metric.getType().getValueType()));
    }
  }

  private static Comparable<Integer> parseInteger(String value) {
    return value.contains(".") ? Integer.parseInt(value.substring(0, value.indexOf('.'))) : Integer.parseInt(value);
  }

  @CheckForNull
  private static Comparable parseMeasure(Condition condition, Measure measure) {
    if (condition.hasPeriod()) {
      return parseMeasureFromVariation(condition, measure);
    }
    switch (measure.getValueType()) {
      case BOOLEAN:
        return measure.getBooleanValue();
      case INT:
        return measure.getIntValue();
      case LONG:
        return measure.getLongValue();
      case DOUBLE:
        return measure.getDoubleValue();
      case STRING:
        return measure.getStringValue();
      case LEVEL:
        return measure.getLevelValue().name();
      case NO_VALUE:
        return null;
      default:
        throw new IllegalArgumentException(
          String.format("Unsupported measure ValueType %s. Can not parse measure to a Comparable", measure.getValueType()));
    }
  }

  @CheckForNull
  private static Comparable parseMeasureFromVariation(Condition condition, Measure measure) {
    Optional<Double> periodValue = getPeriodValue(measure);
    if (periodValue.isPresent()) {
      switch (condition.getMetric().getType().getValueType()) {
        case BOOLEAN:
          return periodValue.get().intValue() == 1;
        case INT:
          return periodValue.get().intValue();
        case LONG:
          return periodValue.get().longValue();
        case DOUBLE:
          return periodValue.get();
        case NO_VALUE:
        case STRING:
        case LEVEL:
        default:
          throw new IllegalArgumentException("Period conditions are not supported for metric type " + condition.getMetric().getType());
      }
    }
    return null;
  }

  private static Optional<Double> getPeriodValue(Measure measure) {
    return measure.hasVariation() ? Optional.of(measure.getVariation()) : Optional.empty();
  }

}
