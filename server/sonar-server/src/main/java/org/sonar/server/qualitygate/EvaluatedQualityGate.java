/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Immutable
public class EvaluatedQualityGate {
  private final QualityGate qualityGate;
  private final Status status;
  private final Set<EvaluatedCondition> evaluatedConditions;

  private EvaluatedQualityGate(QualityGate qualityGate, Status status, Set<EvaluatedCondition> evaluatedConditions) {
    this.qualityGate = qualityGate;
    this.status = status;
    this.evaluatedConditions = evaluatedConditions;
  }

  public QualityGate getQualityGate() {
    return qualityGate;
  }

  public Status getStatus() {
    return status;
  }

  public Set<EvaluatedCondition> getEvaluatedConditions() {
    return evaluatedConditions;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EvaluatedQualityGate that = (EvaluatedQualityGate) o;
    return Objects.equals(qualityGate, that.qualityGate) &&
      status == that.status &&
      Objects.equals(evaluatedConditions, that.evaluatedConditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualityGate, status, evaluatedConditions);
  }

  @Override
  public String toString() {
    return "EvaluatedQualityGate{" +
      "qualityGate=" + qualityGate +
      ", status=" + status +
      ", evaluatedConditions=" + evaluatedConditions +
      '}';
  }

  public static final class Builder {
    private QualityGate qualityGate;
    private Status status;
    private final Map<Condition, EvaluatedCondition> evaluatedConditions = new HashMap<>();

    private Builder() {
      // use static factory method
    }

    public Builder setQualityGate(QualityGate qualityGate) {
      this.qualityGate = checkQualityGate(qualityGate);
      return this;
    }

    public Builder setStatus(Status status) {
      this.status = checkStatus(status);
      return this;
    }

    public Builder addCondition(Condition condition, EvaluationStatus status, @Nullable String value) {
      evaluatedConditions.put(condition, new EvaluatedCondition(condition, status, value));
      return this;
    }

    public Set<EvaluatedCondition> getEvaluatedConditions() {
      return ImmutableSet.copyOf(evaluatedConditions.values());
    }

    public EvaluatedQualityGate build() {
      checkQualityGate(this.qualityGate);
      checkStatus(this.status);

      return new EvaluatedQualityGate(
        this.qualityGate,
        this.status,
        checkEvaluatedConditions(qualityGate, evaluatedConditions));
    }

    private static Set<EvaluatedCondition> checkEvaluatedConditions(QualityGate qualityGate, Map<Condition, EvaluatedCondition> evaluatedConditions) {
      Set<Condition> conditions = qualityGate.getConditions();

      Set<Condition> conditionsNotEvaluated = conditions.stream()
        .filter(c -> !evaluatedConditions.containsKey(c))
        .collect(Collectors.toSet());
      checkArgument(conditionsNotEvaluated.isEmpty(), "Evaluation missing for the following conditions: %s", conditionsNotEvaluated);

      Set<Condition> unknownConditions = evaluatedConditions.keySet().stream()
        .filter(c -> !conditions.contains(c))
        .collect(Collectors.toSet());
      checkArgument(unknownConditions.isEmpty(), "Evaluation provided for unknown conditions: %s", unknownConditions);

      return ImmutableSet.copyOf(evaluatedConditions.values());
    }

    private static QualityGate checkQualityGate(@Nullable QualityGate qualityGate) {
      return requireNonNull(qualityGate, "qualityGate can't be null");
    }

    private static Status checkStatus(@Nullable Status status) {
      return requireNonNull(status, "status can't be null");
    }
  }

  public enum Status {
    OK,
    WARN,
    ERROR
  }
}
