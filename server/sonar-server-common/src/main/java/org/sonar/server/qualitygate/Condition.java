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
package org.sonar.server.qualitygate;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.qualitygate.QualityGateConditionDto;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Objects.requireNonNull;

@Immutable
public class Condition {

  private final String metricKey;
  private final Operator operator;
  @CheckForNull
  private final String warningThreshold;
  @CheckForNull
  private final String errorThreshold;
  private final boolean onLeakPeriod;

  public Condition(String metricKey, Operator operator,
    @Nullable String errorThreshold, @Nullable String warningThreshold,
    boolean onLeakPeriod) {
    this.metricKey = requireNonNull(metricKey, "metricKey can't be null");
    this.operator = requireNonNull(operator, "operator can't be null");
    this.onLeakPeriod = onLeakPeriod;
    this.errorThreshold = emptyToNull(errorThreshold);
    this.warningThreshold = emptyToNull(warningThreshold);
  }

  public String getMetricKey() {
    return metricKey;
  }

  public boolean isOnLeakPeriod() {
    return onLeakPeriod;
  }

  public Operator getOperator() {
    return operator;
  }

  public Optional<String> getWarningThreshold() {
    return Optional.ofNullable(warningThreshold);
  }

  public Optional<String> getErrorThreshold() {
    return Optional.ofNullable(errorThreshold);
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
      Objects.equals(metricKey, condition.metricKey) &&
      operator == condition.operator &&
      Objects.equals(warningThreshold, condition.warningThreshold) &&
      Objects.equals(errorThreshold, condition.errorThreshold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metricKey, operator, warningThreshold, errorThreshold, onLeakPeriod);
  }

  @Override
  public String toString() {
    return "Condition{" +
      "metricKey='" + metricKey + '\'' +
      ", operator=" + operator +
      ", warningThreshold=" + toString(warningThreshold) +
      ", errorThreshold=" + toString(errorThreshold) +
      ", onLeakPeriod=" + onLeakPeriod +
      '}';
  }

  private static String toString(@Nullable String errorThreshold) {
    if (errorThreshold == null) {
      return null;
    }
    return '\'' + errorThreshold + '\'';
  }

  public enum Operator {
    EQUALS(QualityGateConditionDto.OPERATOR_EQUALS),
    NOT_EQUALS(QualityGateConditionDto.OPERATOR_NOT_EQUALS),
    GREATER_THAN(QualityGateConditionDto.OPERATOR_GREATER_THAN),
    LESS_THAN(QualityGateConditionDto.OPERATOR_LESS_THAN);

    private final String dbValue;

    Operator(String dbValue) {
      this.dbValue = dbValue;
    }

    public String getDbValue() {
      return dbValue;
    }

    public static Operator fromDbValue(String s) {
      return Stream.of(values())
        .filter(o -> o.getDbValue().equals(s))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported operator db value: " + s));
    }
  }
}
