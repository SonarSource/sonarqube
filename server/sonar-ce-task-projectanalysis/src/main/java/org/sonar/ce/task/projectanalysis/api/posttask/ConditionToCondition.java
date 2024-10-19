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
package org.sonar.ce.task.projectanalysis.api.posttask;

import java.util.Map;
import java.util.function.Function;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Converts a {@link Condition} from the Compute Engine internal API to a {@link org.sonar.api.ce.posttask.QualityGate.Condition}
 * of the public API.
 */
class ConditionToCondition implements Function<Condition, QualityGate.Condition> {
  private final ConditionImpl.Builder builder = ConditionImpl.newBuilder();
  private final Map<Condition, ConditionStatus> statusPerConditions;

  public ConditionToCondition(Map<Condition, ConditionStatus> statusPerConditions) {
    this.statusPerConditions = statusPerConditions;
  }

  @Override
  public QualityGate.Condition apply(Condition input) {
    String metricKey = input.getMetric().getKey();
    ConditionStatus conditionStatus = statusPerConditions.get(input);
    checkState(conditionStatus != null, "Missing ConditionStatus for condition on metric key %s", metricKey);
    return builder
      .setStatus(convert(conditionStatus.getStatus()))
      .setMetricKey(metricKey)
      .setOperator(convert(input.getOperator()))
      .setErrorThreshold(input.getErrorThreshold())
      .setValue(conditionStatus.getValue())
      .build();
  }

  private static QualityGate.EvaluationStatus convert(ConditionStatus.EvaluationStatus status) {
    switch (status) {
      case NO_VALUE:
        return QualityGate.EvaluationStatus.NO_VALUE;
      case OK:
        return QualityGate.EvaluationStatus.OK;
      case ERROR:
        return QualityGate.EvaluationStatus.ERROR;
      default:
        throw new IllegalArgumentException(format(
          "Unsupported value '%s' of ConditionStatus.EvaluationStatus can not be converted to QualityGate.EvaluationStatus",
          status));
    }
  }

  private static QualityGate.Operator convert(Condition.Operator operator) {
    switch (operator) {
      case GREATER_THAN:
        return QualityGate.Operator.GREATER_THAN;
      case LESS_THAN:
        return QualityGate.Operator.LESS_THAN;
      default:
        throw new IllegalArgumentException(format(
          "Unsupported value '%s' of Condition.Operation can not be converted to QualityGate.Operator",
          operator));
    }
  }
}
