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
package org.sonar.server.webhook;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class QualityGate {
  private final String id;
  private final String name;
  private final Status status;
  private final Set<Condition> conditions;

  public QualityGate(String id, String name, Status status, Set<Condition> conditions) {
    this.id = requireNonNull(id, "id can't be null");
    this.name = requireNonNull(name, "name can't be null");
    this.status = requireNonNull(status, "status can't be null");
    this.conditions = ImmutableSet.copyOf(requireNonNull(conditions, "conditions can't be null"));
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Status getStatus() {
    return status;
  }

  public Collection<Condition> getConditions() {
    return conditions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityGate that = (QualityGate) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(name, that.name) &&
      status == that.status &&
      Objects.equals(conditions, that.conditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, status, conditions);
  }

  @Override
  public String toString() {
    return "QualityGate{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", status=" + status +
      ", conditions=" + conditions +
      '}';
  }

  public enum Status {
    OK,
    WARN,
    ERROR
  }

  public static final class Condition {
    private final EvaluationStatus status;
    private final String metricKey;
    private final Operator operator;
    private final String errorThreshold;
    private final String warnThreshold;
    private final boolean onLeakPeriod;
    private final String value;

    public Condition(EvaluationStatus status, String metricKey, Operator operator,
      @Nullable String errorThreshold, @Nullable String warnThreshold,
      boolean onLeakPeriod, @Nullable String value) {
      this.status = requireNonNull(status, "status can't be null");
      this.metricKey = requireNonNull(metricKey, "metricKey can't be null");
      this.operator = requireNonNull(operator, "operator can't be null");
      this.errorThreshold = errorThreshold;
      this.warnThreshold = warnThreshold;
      this.onLeakPeriod = onLeakPeriod;
      this.value = value;
    }

    public EvaluationStatus getStatus() {
      return status;
    }

    public String getMetricKey() {
      return metricKey;
    }

    public Operator getOperator() {
      return operator;
    }

    public Optional<String> getErrorThreshold() {
      return Optional.ofNullable(errorThreshold);
    }

    public Optional<String> getWarningThreshold() {
      return Optional.ofNullable(warnThreshold);
    }

    public boolean isOnLeakPeriod() {
      return onLeakPeriod;
    }

    public Optional<String> getValue() {
      return Optional.ofNullable(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Condition condition = (Condition) o;
      return onLeakPeriod == condition.onLeakPeriod &&
        status == condition.status &&
        Objects.equals(metricKey, condition.metricKey) &&
        operator == condition.operator &&
        Objects.equals(errorThreshold, condition.errorThreshold) &&
        Objects.equals(warnThreshold, condition.warnThreshold) &&
        Objects.equals(value, condition.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(status, metricKey, operator, errorThreshold, warnThreshold, onLeakPeriod, value);
    }

    @Override
    public String toString() {
      return "Condition{" +
        "status=" + status +
        ", metricKey='" + metricKey + '\'' +
        ", operator=" + operator +
        ", errorThreshold='" + errorThreshold + '\'' +
        ", warnThreshold='" + warnThreshold + '\'' +
        ", onLeakPeriod=" + onLeakPeriod +
        ", value='" + value + '\'' +
        '}';
    }
  }

  /**
   * Quality Gate condition operator.
   */
  public enum Operator {
    EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN
  }

  /**
   * Quality gate condition evaluation status.
   */
  public enum EvaluationStatus {
    /**
     * No measure found or measure had no value. The condition has not been evaluated and therefor ignored in
     * the computation of the Quality Gate status.
     */
    NO_VALUE,
    /**
     * Condition evaluated as OK, neither error nor warning thresholds have been reached.
     */
    OK,
    /**
     * Condition evaluated as WARN, only warning thresholds has been reached.
     */
    WARN,
    /**
     * Condition evaluated as ERROR, error thresholds has been reached (and most likely warning thresholds too).
     */
    ERROR
  }
}
