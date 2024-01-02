/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualitygate;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Optional.of;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;

class ConditionEvaluator {

  private static final Set<ValueType> NUMERICAL_TYPES = EnumSet.of(INT, RATING, FLOAT, MILLISEC, PERCENT, WORK_DUR);

  private ConditionEvaluator() {
    // prevent instantiation
  }

  /**
   * Evaluates the condition for the specified measure
   */
  static EvaluatedCondition evaluate(Condition condition, QualityGateEvaluator.Measures measures) {
    Optional<QualityGateEvaluator.Measure> measure = measures.get(condition.getMetricKey());
    if (measure.isEmpty()) {
      return new EvaluatedCondition(condition, EvaluationStatus.OK, null);
    }

    Optional<Comparable> value = getMeasureValue(condition, measure.get());
    if (value.isEmpty()) {
      return new EvaluatedCondition(condition, EvaluationStatus.OK, null);
    }

    ValueType type = measure.get().getType();
    return evaluateCondition(condition, type, value.get())
      .orElseGet(() -> new EvaluatedCondition(condition, EvaluationStatus.OK, value.get().toString()));
  }

  /**
   * Evaluates the error condition. Returns empty if threshold or measure value is not defined.
   */
  private static Optional<EvaluatedCondition> evaluateCondition(Condition condition, ValueType type, Comparable value) {
    Comparable threshold = getThreshold(condition, type);

    if (reachThreshold(value, threshold, condition)) {
      return of(new EvaluatedCondition(condition, EvaluationStatus.ERROR, value.toString()));
    }
    return Optional.empty();
  }

  private static Comparable getThreshold(Condition condition, ValueType valueType) {
    String valString = condition.getErrorThreshold();
    try {
      return switch (valueType) {
        case INT, RATING -> parseInteger(valString);
        case MILLISEC, WORK_DUR -> Long.parseLong(valString);
        case FLOAT, PERCENT -> Double.parseDouble(valString);
        case LEVEL -> valueType;
        default -> throw new IllegalArgumentException(format("Unsupported value type %s. Cannot convert condition value", valueType));
      };
    } catch (NumberFormatException badValueFormat) {
      throw new IllegalArgumentException(format(
        "Quality Gate: unable to parse threshold '%s' to compare against %s", valString, condition.getMetricKey()));
    }
  }

  private static Optional<Comparable> getMeasureValue(Condition condition, QualityGateEvaluator.Measure measure) {
    if (condition.isOnLeakPeriod()) {
      return Optional.ofNullable(getLeakValue(measure));
    }

    return Optional.ofNullable(getValue(measure));
  }

  @CheckForNull
  private static Comparable getValue(QualityGateEvaluator.Measure measure) {
    if (NUMERICAL_TYPES.contains(measure.getType())) {
      return measure.getValue().isPresent() ? getNumericValue(measure.getType(), measure.getValue().getAsDouble()) : null;
    }

    checkArgument(ValueType.LEVEL.equals(measure.getType()), "Condition is not allowed for type %s" , measure.getType());
    return measure.getStringValue().orElse(null);

  }

  @CheckForNull
  private static Comparable getLeakValue(QualityGateEvaluator.Measure measure) {
    if (NUMERICAL_TYPES.contains(measure.getType())) {
      return measure.getValue().isPresent() ? getNumericValue(measure.getType(), measure.getValue().getAsDouble()) : null;
    }

    throw new IllegalArgumentException("Condition on leak period is not allowed for type " + measure.getType());
  }

  private static Comparable getNumericValue(ValueType type, double value) {
    return switch (type) {
      case INT, RATING -> (int) value;
      case FLOAT, PERCENT -> value;
      case MILLISEC, WORK_DUR -> (long) value;
      default -> throw new IllegalArgumentException("Condition on numeric value is not allowed for type " + type);
    };
  }

  private static int parseInteger(String value) {
    return value.contains(".") ? Integer.parseInt(value.substring(0, value.indexOf('.'))) : Integer.parseInt(value);
  }

  private static boolean reachThreshold(Comparable measureValue, Comparable threshold, Condition condition) {
    int comparison = measureValue.compareTo(threshold);
    switch (condition.getOperator()) {
      case GREATER_THAN:
        return comparison > 0;
      case LESS_THAN:
        return comparison < 0;
      default:
        throw new IllegalArgumentException(format("Unsupported operator '%s'", condition.getOperator()));
    }
  }
}
