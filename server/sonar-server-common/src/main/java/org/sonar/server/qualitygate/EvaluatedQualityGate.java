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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.measures.Metric;
import org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;

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
    private static final List<String> CONDITIONS_ORDER = Arrays.asList(NEW_SECURITY_RATING_KEY, SECURITY_RATING_KEY, NEW_RELIABILITY_RATING_KEY,
      RELIABILITY_RATING_KEY, NEW_MAINTAINABILITY_RATING_KEY, SQALE_RATING_KEY, NEW_COVERAGE_KEY, COVERAGE_KEY, NEW_DUPLICATED_LINES_DENSITY_KEY,
      DUPLICATED_LINES_DENSITY_KEY);
    private static final Map<String, Integer> CONDITIONS_ORDER_IDX = IntStream.range(0, CONDITIONS_ORDER.size()).boxed()
      .collect(Collectors.toMap(CONDITIONS_ORDER::get, x -> x));

    private static final Comparator<EvaluatedCondition> CONDITION_COMPARATOR = (c1, c2) -> {
      Function<EvaluatedCondition, Integer> byList = c -> CONDITIONS_ORDER_IDX.getOrDefault(c.getCondition().getMetricKey(), Integer.MAX_VALUE);
      Function<EvaluatedCondition, String> byMetricKey = c -> c.getCondition().getMetricKey();
      return Comparator.comparing(byList).thenComparing(byMetricKey).compare(c1, c2);
    };

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
      sortedEvaluatedConditions.sort(CONDITION_COMPARATOR);
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
