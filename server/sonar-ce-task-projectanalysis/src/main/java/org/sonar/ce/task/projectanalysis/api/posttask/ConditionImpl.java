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
package org.sonar.ce.task.projectanalysis.api.posttask;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.posttask.QualityGate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.ce.posttask.QualityGate.EvaluationStatus.NO_VALUE;

@Immutable
class ConditionImpl implements QualityGate.Condition {
  private final QualityGate.EvaluationStatus status;
  private final String metricKey;
  private final QualityGate.Operator operator;
  private final String errorThreshold;
  private final boolean onLeakPeriod;
  @CheckForNull
  private final String value;

  private ConditionImpl(Builder builder) {
    requireNonNull(builder.status, "status can not be null");
    requireNonNull(builder.metricKey, "metricKey can not be null");
    requireNonNull(builder.operator, "operator can not be null");
    requireNonNull(builder.errorThreshold, "errorThreshold can not be null");
    verifyValue(builder);

    this.status = builder.status;
    this.metricKey = builder.metricKey;
    this.operator = builder.operator;
    this.errorThreshold = builder.errorThreshold;
    this.onLeakPeriod = builder.metricKey.startsWith("new_");
    this.value = builder.value;
  }

  private static void verifyValue(Builder builder) {
    if (builder.status == NO_VALUE) {
      checkArgument(builder.value == null, "value must be null when status is %s", NO_VALUE);
    } else {
      checkArgument(builder.value != null, "value can not be null when status is not %s", NO_VALUE);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String metricKey;
    private QualityGate.Operator operator;
    private String errorThreshold;
    @CheckForNull
    private String value;
    private QualityGate.EvaluationStatus status;

    private Builder() {
      // enforce use of static method
    }

    public Builder setMetricKey(String metricKey) {
      this.metricKey = metricKey;
      return this;
    }

    public Builder setOperator(QualityGate.Operator operator) {
      this.operator = operator;
      return this;
    }

    public Builder setErrorThreshold(String errorThreshold) {
      this.errorThreshold = errorThreshold;
      return this;
    }

    /**
     * @deprecated in 7.6. This method has no longer any effect.
     */
    @Deprecated
    public Builder setWarningThreshold(String warningThreshold) {
      return this;
    }

    /**
     * @deprecated in 7.6. This method has no longer any effect.
     */
    @Deprecated
    public Builder setOnLeakPeriod(boolean onLeakPeriod) {
      return this;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public Builder setStatus(QualityGate.EvaluationStatus status) {
      this.status = status;
      return this;
    }

    public ConditionImpl build() {
      return new ConditionImpl(this);
    }
  }

  @Override
  public QualityGate.EvaluationStatus getStatus() {
    return status;
  }

  @Override
  public String getMetricKey() {
    return metricKey;
  }

  @Override
  public QualityGate.Operator getOperator() {
    return operator;
  }

  @Override
  public String getErrorThreshold() {
    return errorThreshold;
  }

  @Deprecated
  @Override
  public String getWarningThreshold() {
    return null;
  }

  /**
   * @deprecated in 7.6. Conditions "on leak period" were removed. Use "New X" conditions instead.
   */
  @Deprecated
  @Override
  public boolean isOnLeakPeriod() {
    return onLeakPeriod;
  }

  @Override
  public String getValue() {
    checkState(status != NO_VALUE, "There is no value when status is %s", NO_VALUE);

    return value;
  }

  @Override
  public String toString() {
    return "ConditionImpl{" +
      "status=" + status +
      ", metricKey='" + metricKey + '\'' +
      ", operator=" + operator +
      ", errorThreshold='" + errorThreshold + '\'' +
      ", value='" + value + '\'' +
      '}';
  }
}
