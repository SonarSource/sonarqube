/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.Metric;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;

class QualityGateEvaluatorImplTest {
  private final MapSettings settings = new MapSettings();
  private final Configuration configuration = new ConfigurationBridge(settings);
  QualityGateFallbackManager qualityGateFallbackManager = mock(QualityGateFallbackManager.class);
  private final QualityGateEvaluator underTest = new QualityGateEvaluatorImpl(qualityGateFallbackManager);

  @Test
  void getMetricKeys_includes_by_default_new_lines_and_new_lines_to_cover() {
    QualityGate gate = mock(QualityGate.class);
    assertThat(underTest.getMetricKeys(gate)).containsExactly(NEW_LINES_KEY, NEW_LINES_TO_COVER_KEY);
  }

  @Test
  void getMetricKeys_includes_metrics_from_qgate() {
    Set<String> metricKeys = ImmutableSet.of("foo", "bar", "baz");
    Set<Condition> conditions = metricKeys.stream().map(key -> {
      Condition condition = mock(Condition.class);
      when(condition.getMetricKey()).thenReturn(key);
      return condition;
    }).collect(Collectors.toSet());

    QualityGate gate = mock(QualityGate.class);
    when(gate.getConditions()).thenReturn(conditions);
    assertThat(underTest.getMetricKeys(gate)).containsAll(metricKeys);
  }

  @Test
  void getMetricKeys_shouldIncludeFallbackConditionsMetricKeys() {
    Set<String> metricKeys = ImmutableSet.of("foo", "bar", "baz");
    when(qualityGateFallbackManager.getFallbackCondition(any())).thenReturn(Optional.of(new Condition("fallback", Condition.Operator.GREATER_THAN, "0")));
    Set<Condition> conditions = metricKeys.stream().map(key -> {
      Condition condition = mock(Condition.class);
      when(condition.getMetricKey()).thenReturn(key);
      return condition;
    }).collect(Collectors.toSet());

    QualityGate gate = mock(QualityGate.class);
    when(gate.getConditions()).thenReturn(conditions);
    assertThat(underTest.getMetricKeys(gate)).containsAll(metricKeys);
    assertThat(underTest.getMetricKeys(gate)).contains("fallback");
  }

  @Test
  void evaluate_whenConditionHasNoDataAndHasFallBack_shouldEvaluateFallbackInstead() {
    when(qualityGateFallbackManager.getFallbackCondition(any())).thenReturn(Optional.of(new Condition("fallback", Condition.Operator.GREATER_THAN, "0")));
    Condition condition = mock(Condition.class);
    when(condition.getMetricKey()).thenReturn("foo");

    QualityGate gate = mock(QualityGate.class);
    when(gate.getConditions()).thenReturn(Set.of(condition));
    QualityGateEvaluator.Measures measures = mock(QualityGateEvaluator.Measures.class);
    when(measures.get("foo")).thenReturn(Optional.empty());
    when(measures.get("fallback")).thenReturn(Optional.of(new FakeMeasure(1)));

    assertThat(underTest.evaluate(gate, measures, configuration).getEvaluatedConditions())
      .extracting(x -> x.getCondition().getMetricKey(), x -> x.getOriginalCondition().getMetricKey())
      .containsExactly(tuple("fallback", "foo"));
  }

  @Test
  void evaluated_conditions_are_sorted() {
    Set<String> metricKeys = ImmutableSet.of("foo", "bar", NEW_MAINTAINABILITY_RATING_KEY);
    Set<Condition> conditions = metricKeys.stream().map(key -> {
      Condition condition = mock(Condition.class);
      when(condition.getMetricKey()).thenReturn(key);
      return condition;
    }).collect(Collectors.toSet());

    QualityGate gate = mock(QualityGate.class);
    when(gate.getConditions()).thenReturn(conditions);
    QualityGateEvaluator.Measures measures = mock(QualityGateEvaluator.Measures.class);

    assertThat(underTest.evaluate(gate, measures, configuration).getEvaluatedConditions()).extracting(x -> x.getCondition().getMetricKey())
      .containsExactly(NEW_MAINTAINABILITY_RATING_KEY, "bar", "foo");
  }

  @Test
  void evaluate_is_OK_for_empty_qgate() {
    QualityGate gate = mock(QualityGate.class);
    QualityGateEvaluator.Measures measures = mock(QualityGateEvaluator.Measures.class);
    EvaluatedQualityGate evaluatedQualityGate = underTest.evaluate(gate, measures, configuration);
    assertThat(evaluatedQualityGate.getStatus()).isEqualTo(Metric.Level.OK);
  }

  @Test
  void evaluate_is_ERROR() {
    Condition condition = new Condition(NEW_MAINTAINABILITY_RATING_KEY, Condition.Operator.GREATER_THAN, "0");

    QualityGate gate = mock(QualityGate.class);
    when(gate.getConditions()).thenReturn(singleton(condition));
    QualityGateEvaluator.Measures measures = key -> Optional.of(new FakeMeasure(1));

    assertThat(underTest.evaluate(gate, measures, configuration).getStatus()).isEqualTo(Metric.Level.ERROR);
  }

  @ParameterizedTest
  @MethodSource("coverageAndDuplication")
  void evaluate_for_small_changes(String conditionKey, String measureKey) {
    Condition condition = new Condition(conditionKey, Condition.Operator.GREATER_THAN, "0");

    Map<String, QualityGateEvaluator.Measure> notSmallChange = new HashMap<>();
    notSmallChange.put(conditionKey, new FakeMeasure(1));
    notSmallChange.put(measureKey, new FakeMeasure(1000));

    Map<String, QualityGateEvaluator.Measure> smallChange = new HashMap<>();
    smallChange.put(conditionKey, new FakeMeasure(1));
    smallChange.put(measureKey, new FakeMeasure(10));

    QualityGate gate = mock(QualityGate.class);
    when(gate.getConditions()).thenReturn(singleton(condition));
    QualityGateEvaluator.Measures notSmallChangeMeasures = key -> Optional.ofNullable(notSmallChange.get(key));
    QualityGateEvaluator.Measures smallChangeMeasures = key -> Optional.ofNullable(smallChange.get(key));

    settings.setProperty(CoreProperties.QUALITY_GATE_IGNORE_SMALL_CHANGES, true);
    assertThat(underTest.evaluate(gate, notSmallChangeMeasures, configuration).getStatus()).isEqualTo(Metric.Level.ERROR);
    assertThat(underTest.evaluate(gate, smallChangeMeasures, configuration).getStatus()).isEqualTo(Metric.Level.OK);

    settings.setProperty(CoreProperties.QUALITY_GATE_IGNORE_SMALL_CHANGES, false);
    assertThat(underTest.evaluate(gate, smallChangeMeasures, configuration).getStatus()).isEqualTo(Metric.Level.ERROR);
  }

  private static Stream<Arguments> coverageAndDuplication() {
    return Stream.of(
      arguments(NEW_DUPLICATED_LINES_KEY, NEW_LINES_KEY),
      arguments(NEW_COVERAGE_KEY, NEW_LINES_TO_COVER_KEY)
    );
  }
}
