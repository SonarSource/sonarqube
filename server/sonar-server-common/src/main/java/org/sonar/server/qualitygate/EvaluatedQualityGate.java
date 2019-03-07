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
package org.sonar.server.qualitygate;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.measures.Metric;
import org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Immutable
public class EvaluatedQualityGate {
  private final QualityGate qualityGate;
  private final Metric.Level status;
  private final Collection<EvaluatedCondition> evaluatedConditions;
  private final boolean ignoredConditionsOnSmallChangeset;

  private EvaluatedQualityGate(QualityGate qualityGate, Metric.Level status, Collection<EvaluatedCondition> evaluatedConditions, boolean ignoredConditionsOnSmallChangeset) {
    this.qualityGate = requireNonNull(qualityGate, "qualityGate can't be null");
    this.status = requireNonNull(status, "status can't be null");
    this.evaluatedConditions = evaluatedConditions;
    this.ignoredConditionsOnSmallChangeset = ignoredConditionsOnSmallChangeset;
  }

  public QualityGate getQualityGate() {
    return qualityGate;
  }

  public Metric.Level getStatus() {
    return status;
  }

  public Collection<EvaluatedCondition> getEvaluatedConditions() {
    return evaluatedConditions;
  }

  public boolean hasIgnoredConditionsOnSmallChangeset() {
    return ignoredConditionsOnSmallChangeset;
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
    private Metric.Level status;
    private final Map<Condition, EvaluatedCondition> evaluatedConditions = new LinkedHashMap<>();
    private boolean ignoredConditionsOnSmallChangeset = false;

    private Builder() {
      // use static factory method
    }

    public Builder setQualityGate(QualityGate qualityGate) {
      this.qualityGate = qualityGate;
      return this;
    }

    public Builder setStatus(Metric.Level status) {
      this.status = status;
      return this;
    }

    public Builder setIgnoredConditionsOnSmallChangeset(boolean b) {
      this.ignoredConditionsOnSmallChangeset = b;
      return this;
    }

    public Builder addEvaluatedCondition(Condition condition, EvaluationStatus status, @Nullable String value) {
      evaluatedConditions.put(condition, new EvaluatedCondition(condition, status, value));
      return this;
    }

    public Builder addEvaluatedCondition(EvaluatedCondition c) {
      evaluatedConditions.put(c.getCondition(), c);
      return this;
    }

    public Set<EvaluatedCondition> getEvaluatedConditions() {
      return ImmutableSet.copyOf(evaluatedConditions.values());
    }

    public EvaluatedQualityGate build() {
      checkEvaluatedConditions(qualityGate, evaluatedConditions);
      List<EvaluatedCondition> sortedEvaluatedConditions = new ArrayList<>(evaluatedConditions.values());
      sortedEvaluatedConditions.sort(new ConditionComparator<>(c -> c.getCondition().getMetricKey()));
      return new EvaluatedQualityGate(
        this.qualityGate,
        this.status,
        sortedEvaluatedConditions,
        ignoredConditionsOnSmallChangeset);
    }

    private static void checkEvaluatedConditions(QualityGate qualityGate, Map<Condition, EvaluatedCondition> evaluatedConditions) {
      Set<Condition> conditions = qualityGate.getConditions();

      Set<Condition> conditionsNotEvaluated = conditions.stream()
        .filter(c -> !evaluatedConditions.containsKey(c))
        .collect(Collectors.toSet());
      checkArgument(conditionsNotEvaluated.isEmpty(), "Evaluation missing for the following conditions: %s", conditionsNotEvaluated);

      Set<Condition> unknownConditions = evaluatedConditions.keySet().stream()
        .filter(c -> !conditions.contains(c))
        .collect(Collectors.toSet());
      checkArgument(unknownConditions.isEmpty(), "Evaluation provided for unknown conditions: %s", unknownConditions);
    }
  }
}
