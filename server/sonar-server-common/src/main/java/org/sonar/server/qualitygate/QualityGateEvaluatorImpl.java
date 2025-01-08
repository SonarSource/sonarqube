/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.Level;
import org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus;

import static java.util.stream.Collectors.toSet;

public class QualityGateEvaluatorImpl implements QualityGateEvaluator {

  public static final int MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS = 20;
  /**
   * Some metrics will be ignored on very small change sets.
   */
  public static final Set<String> METRICS_TO_IGNORE_ON_SMALL_CHANGESETS = ImmutableSet.of(
    CoreMetrics.NEW_COVERAGE_KEY,
    CoreMetrics.NEW_LINE_COVERAGE_KEY,
    CoreMetrics.NEW_BRANCH_COVERAGE_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_KEY,
    CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY);

  private final QualityGateFallbackManager qualityGateFallbackManager;

  public QualityGateEvaluatorImpl(QualityGateFallbackManager qualityGateFallbackManager) {
    this.qualityGateFallbackManager = qualityGateFallbackManager;
  }

  @Override
  public EvaluatedQualityGate evaluate(QualityGate gate, Measures measures, Configuration configuration) {
    EvaluatedQualityGate.Builder result = EvaluatedQualityGate.newBuilder()
      .setQualityGate(gate);

    boolean ignoreSmallChanges = configuration.getBoolean(CoreProperties.QUALITY_GATE_IGNORE_SMALL_CHANGES).orElse(true);
    boolean isSmallChangeset = ignoreSmallChanges && isSmallChangeset(measures);

    gate.getConditions().forEach(condition -> {
      String metricKey = condition.getMetricKey();
      EvaluatedCondition evaluation = ConditionEvaluator.evaluate(condition, measures);

      if (evaluation.isMissingMeasure()) {
        evaluation = qualityGateFallbackManager.getFallbackCondition(evaluation.getCondition())
          .map(c -> ConditionEvaluator.evaluate(c, measures).setOriginalCondition(condition))
          .orElse(evaluation);
      }

      if (isSmallChangeset && evaluation.getStatus() != EvaluationStatus.OK && METRICS_TO_IGNORE_ON_SMALL_CHANGESETS.contains(metricKey)) {
        result.addEvaluatedCondition(new EvaluatedCondition(evaluation.getCondition(), evaluation.getOriginalCondition(),
          EvaluationStatus.OK, evaluation.getValue().orElse(null), evaluation.isMissingMeasure()));
        result.setIgnoredConditionsOnSmallChangeset(true);
      } else {
        result.addEvaluatedCondition(evaluation);
      }
    });

    result.setStatus(overallStatusOf(result.getEvaluatedConditions()));

    return result.build();
  }

  @Override
  public Set<String> getMetricKeys(QualityGate gate) {
    Set<String> metricKeys = new HashSet<>();
    metricKeys.add(CoreMetrics.NEW_LINES_KEY);
    for (Condition condition : gate.getConditions()) {
      metricKeys.add(condition.getMetricKey());
      qualityGateFallbackManager.getFallbackCondition(condition)
        .ifPresent(c -> metricKeys.add(c.getMetricKey()));
    }
    return metricKeys;
  }

  private static boolean isSmallChangeset(Measures measures) {
    Optional<Measure> newLines = measures.get(CoreMetrics.NEW_LINES_KEY);
    return newLines.isPresent() &&
      newLines.get().getValue().isPresent() &&
      newLines.get().getValue().getAsDouble() < MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS;
  }

  private static Level overallStatusOf(Set<EvaluatedCondition> conditions) {
    Set<EvaluationStatus> statuses = conditions.stream()
      .map(EvaluatedCondition::getStatus)
      .collect(toSet());

    if (statuses.contains(EvaluationStatus.ERROR)) {
      return Level.ERROR;
    }
    return Level.OK;
  }
}
