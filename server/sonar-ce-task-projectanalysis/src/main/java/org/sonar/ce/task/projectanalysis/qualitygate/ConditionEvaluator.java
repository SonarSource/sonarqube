/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitygate;

import java.util.EnumSet;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.of;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.FLOAT;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.INT;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.LEVEL;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.MILLISEC;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.PERCENT;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.RATING;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.WORK_DUR;

public final class ConditionEvaluator {

  private static final EnumSet<Metric.MetricType> SUPPORTED_METRIC_TYPE = EnumSet.of(INT, MILLISEC, RATING, WORK_DUR, FLOAT, PERCENT, LEVEL);

  /**
   * Evaluates the condition for the specified measure
   */
  public EvaluationResult evaluate(Condition condition, Measure measure) {
    checkArgument(SUPPORTED_METRIC_TYPE.contains(condition.getMetric().getType()), "Conditions on MetricType %s are not supported", condition.getMetric().getType());

    Comparable measureComparable = parseMeasure(condition, measure);
    if (measureComparable == null) {
      return new EvaluationResult(Measure.Level.OK, null);
    }

    return evaluateCondition(condition, measureComparable)
      .orElseGet(() -> new EvaluationResult(Measure.Level.OK, measureComparable));
  }

  private static Optional<EvaluationResult> evaluateCondition(Condition condition, Comparable<?> measureComparable) {
    try {
      Comparable conditionComparable = parseConditionValue(condition.getMetric(), condition.getErrorThreshold());
      if (doesReachThresholds(measureComparable, conditionComparable, condition)) {
        return of(new EvaluationResult(Measure.Level.ERROR, measureComparable));
      }
      return Optional.empty();
    } catch (NumberFormatException badValueFormat) {
      throw new IllegalArgumentException(String.format(
        "Quality Gate: Unable to parse value '%s' to compare against %s",
        condition.getErrorThreshold(), condition.getMetric().getName()));
    }
  }

  private static boolean doesReachThresholds(Comparable measureValue, Comparable criteriaValue, Condition condition) {
    int comparison = measureValue.compareTo(criteriaValue);
    switch (condition.getOperator()) {
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
      case INT:
        return parseInteger(value);
      case LONG:
        return Long.parseLong(value);
      case DOUBLE:
        return Double.parseDouble(value);
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
    if (condition.useVariation()) {
      return parseMeasureFromVariation(condition, measure);
    }
    switch (measure.getValueType()) {
      case INT:
        return measure.getIntValue();
      case LONG:
        return measure.getLongValue();
      case DOUBLE:
        return measure.getDoubleValue();
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
    if (!measure.hasVariation()) {
      return null;
    }

    Double variation = measure.getVariation();
    Metric.MetricType metricType = condition.getMetric().getType();
    switch (metricType.getValueType()) {
      case INT:
        return variation.intValue();
      case LONG:
        return variation.longValue();
      case DOUBLE:
        return variation;
      case NO_VALUE:
      case LEVEL:
      default:
        throw new IllegalArgumentException("Unsupported metric type " + metricType);
    }
  }
}
