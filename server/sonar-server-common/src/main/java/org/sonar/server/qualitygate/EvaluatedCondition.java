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

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class EvaluatedCondition {
  private final Condition condition;
  private Condition originalCondition;
  private final EvaluationStatus status;
  @Nullable
  private final String value;
  private final boolean missingMeasure;

  public EvaluatedCondition(Condition condition, EvaluationStatus status, @Nullable String value) {
    this(condition, status, value, false);
  }

  public EvaluatedCondition(Condition condition, EvaluationStatus status, @Nullable String value, boolean measureMissing) {
    this(condition, condition, status, value, measureMissing);
  }

  public EvaluatedCondition(Condition condition, Condition originalCondition, EvaluationStatus status, @Nullable String value, boolean measureMissing) {
    this.condition = requireNonNull(condition, "condition can't be null");
    this.originalCondition = originalCondition;
    this.status = requireNonNull(status, "status can't be null");
    this.value = value;
    this.missingMeasure = measureMissing;
  }

  public Condition getCondition() {
    return condition;
  }

  public Condition getOriginalCondition() {
    return originalCondition;
  }

  public EvaluatedCondition setOriginalCondition(Condition originalCondition) {
    this.originalCondition = originalCondition;
    return this;
  }

  public EvaluationStatus getStatus() {
    return status;
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
    EvaluatedCondition that = (EvaluatedCondition) o;
    return Objects.equals(condition, that.condition) &&
      status == that.status &&
      Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, status, value);
  }

  @Override
  public String toString() {
    return "EvaluatedCondition{" +
      "condition=" + condition +
      ", originalCondition=" + originalCondition +
      ", status=" + status +
      ", value=" + (value == null ? null : ('\'' + value + '\'')) +
      '}';
  }

  public boolean isMissingMeasure() {
    return missingMeasure;
  }

  /**
   * Quality gate condition evaluation status.
   */
  public enum EvaluationStatus {
    /**
     * No measure found or measure had no value. The condition has not been evaluated and therefor ignored in
     * the computation of the Quality Gate status.
     * @deprecated since 10.8, the value is never used in the code
     */
    @Deprecated(since = "10.8")
    NO_VALUE,
    /**
     * Condition evaluated as OK, error thresholds hasn't been reached.
     */
    OK,
    /**
     * Condition evaluated as ERROR, error thresholds has been reached.
     */
    ERROR
  }
}
