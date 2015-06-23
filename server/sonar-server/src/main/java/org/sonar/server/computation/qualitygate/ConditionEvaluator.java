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
package org.sonar.server.computation.qualitygate;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;

public final class ConditionEvaluator {

  private static final Optional<Double> NO_PERIOD_VALUE = Optional.absent();

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
      .or(evaluateCondition(condition, measureComparable, Measure.Level.WARN))
      .or(new EvaluationResult(Measure.Level.OK, measureComparable));
  }

  private static Optional<EvaluationResult> evaluateCondition(Condition condition, Comparable<?> measureComparable, Measure.Level alertLevel) {
    String conditionValue = getValueToEval(condition, alertLevel);
    if (StringUtils.isEmpty(conditionValue)) {
      return Optional.absent();
    }

    try {
      Comparable conditionComparable = parseConditionValue(condition.getMetric(), conditionValue);
      if (doesReachThresholds(measureComparable, conditionComparable, condition)) {
        return of(new EvaluationResult(alertLevel, measureComparable));
      }
      return Optional.absent();
    } catch (NumberFormatException badValueFormat) {
      throw new IllegalArgumentException(String.format(
        "Quality Gate: Unable to parse value '%s' to compare against %s",
        conditionValue, condition.getMetric().getName()));
    }
  }

  private static String getValueToEval(Condition condition, Measure.Level alertLevel) {
    if (alertLevel.equals(Measure.Level.ERROR)) {
      return condition.getErrorThreshold();
    } else if (alertLevel.equals(Measure.Level.WARN)) {
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
    if (condition.getPeriod() != null) {
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
    Optional<Double> periodValue = getPeriodValue(measure, condition.getPeriod());
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

  private static Optional<Double> getPeriodValue(Measure measure, int period) {
    if (!measure.hasVariations()) {
      return Optional.absent();
    }

    MeasureVariations variations = measure.getVariations();
    switch (period) {
      case 1:
        return variations.hasVariation1() ? of(variations.getVariation1()) : NO_PERIOD_VALUE;
      case 2:
        return variations.hasVariation2() ? of(variations.getVariation2()) : NO_PERIOD_VALUE;
      case 3:
        return variations.hasVariation3() ? of(variations.getVariation3()) : NO_PERIOD_VALUE;
      case 4:
        return variations.hasVariation4() ? of(variations.getVariation4()) : NO_PERIOD_VALUE;
      case 5:
        return variations.hasVariation5() ? of(variations.getVariation5()) : NO_PERIOD_VALUE;
      default:
        throw new IllegalArgumentException("Following index period is not allowed : " + period);
    }
  }

}
