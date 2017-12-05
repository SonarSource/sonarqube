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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.Level;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus;

import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.toEnumSet;

public class QualityGateEvaluatorImpl implements QualityGateEvaluator {

  private static final int MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS = 20;
  /**
   * Some metrics will be ignored on very small change sets.
   */
  private static final Set<String> METRICS_TO_IGNORE_ON_SMALL_CHANGESETS = ImmutableSet.of(
    CoreMetrics.NEW_COVERAGE_KEY,
    CoreMetrics.NEW_LINE_COVERAGE_KEY,
    CoreMetrics.NEW_BRANCH_COVERAGE_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_KEY,
    CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY);

  @Override
  public EvaluatedQualityGate evaluate(QualityGate gate, Measures measures) {
    EvaluatedQualityGate.Builder result = EvaluatedQualityGate.newBuilder()
      .setQualityGate(gate);

    boolean isSmallChangeset = isSmallChangeset(measures);
    Multimap<String, Condition> conditionsPerMetric = gate.getConditions().stream()
      .collect(MoreCollectors.index(Condition::getMetricKey, Function.identity()));

    for (Map.Entry<String, Collection<Condition>> entry : conditionsPerMetric.asMap().entrySet()) {
      String metricKey = entry.getKey();
      Collection<Condition> conditionsOnSameMetric = entry.getValue();

      EvaluatedCondition evaluation = evaluateConditionsOnMetric(conditionsOnSameMetric, measures);

      if (isSmallChangeset && evaluation.getStatus() != EvaluationStatus.OK && METRICS_TO_IGNORE_ON_SMALL_CHANGESETS.contains(metricKey)) {
        result.addCondition(new EvaluatedCondition(evaluation.getCondition(), EvaluationStatus.OK, evaluation.getValue().orElse(null)));
        result.setIgnoredConditionsOnSmallChangeset(true);
      } else {
        result.addCondition(evaluation);
      }
    }

    result.setStatus(overallStatusOf(result.getEvaluatedConditions()));

    return result.build();
  }

  @Override
  public Set<String> getMetricKeys(QualityGate gate) {
    Set<String> metricKeys = new HashSet<>();
    metricKeys.add(CoreMetrics.NEW_LINES_KEY);
    for (Condition condition : gate.getConditions()) {
      metricKeys.add(condition.getMetricKey());
    }
    return metricKeys;
  }

  private static boolean isSmallChangeset(Measures measures) {
    Optional<Measure> newLines = measures.get(CoreMetrics.NEW_LINES_KEY);
    return newLines.isPresent() &&
      newLines.get().getLeakValue().isPresent() &&
      newLines.get().getLeakValue().getAsDouble() < MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS;
  }

  private static EvaluatedCondition evaluateConditionsOnMetric(Collection<Condition> conditionsOnSameMetric, Measures measures) {
    EvaluatedCondition leakEvaluation = null;
    EvaluatedCondition absoluteEvaluation = null;
    for (Condition condition : conditionsOnSameMetric) {
      if (condition.isOnLeakPeriod()) {
        leakEvaluation = ConditionEvaluator.evaluate(condition, measures);
      } else {
        absoluteEvaluation = ConditionEvaluator.evaluate(condition, measures);
      }
    }

    if (leakEvaluation == null) {
      return requireNonNull(absoluteEvaluation, "Evaluation of absolute value can't be null on conditions " + conditionsOnSameMetric);
    }
    if (absoluteEvaluation == null) {
      return requireNonNull(leakEvaluation, "Evaluation of leak value can't be null on conditions " + conditionsOnSameMetric);
    }
    // both conditions are present. Take the worse one. In case of equality, take
    // the one on the leak period
    if (absoluteEvaluation.getStatus().compareTo(leakEvaluation.getStatus()) > 0) {
      return absoluteEvaluation;
    }
    return leakEvaluation;
  }

  private static Level overallStatusOf(Set<EvaluatedCondition> conditions) {
    Set<EvaluationStatus> statuses = conditions.stream().map(EvaluatedCondition::getStatus).collect(toEnumSet(EvaluationStatus.class));
    if (statuses.contains(EvaluationStatus.ERROR)) {
      return Level.ERROR;
    }
    if (statuses.contains(EvaluationStatus.WARN)) {
      return Level.WARN;
    }
    return Level.OK;
  }

}
