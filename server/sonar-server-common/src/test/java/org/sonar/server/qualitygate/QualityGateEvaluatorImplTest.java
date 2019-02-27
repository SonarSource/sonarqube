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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;

public class QualityGateEvaluatorImplTest {

  private final QualityGateEvaluator underTest = new QualityGateEvaluatorImpl();

  @Test
  public void getMetricKeys_includes_by_default_new_lines() {
    QualityGate gate = mock(QualityGate.class);
    assertThat(underTest.getMetricKeys(gate)).containsExactly(NEW_LINES_KEY);
  }

  @Test
  public void getMetricKeys_includes_metrics_from_qgate() {
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
  public void evaluated_conditions_are_sorted() {
    Set<String> metricKeys = ImmutableSet.of("foo", "bar", CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY);
    Set<Condition> conditions = metricKeys.stream().map(key -> {
      Condition condition = mock(Condition.class);
      when(condition.getMetricKey()).thenReturn(key);
      return condition;
    }).collect(Collectors.toSet());

    QualityGate gate = mock(QualityGate.class);
    when(gate.getConditions()).thenReturn(conditions);
    QualityGateEvaluator.Measures measures = mock(QualityGateEvaluator.Measures.class);

    assertThat(underTest.evaluate(gate, measures).getEvaluatedConditions()).extracting(x -> x.getCondition().getMetricKey())
    .containsExactly(CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY, "bar", "foo");
  }

  @Test
  public void evaluate_is_OK_for_empty_qgate() {
    QualityGate gate = mock(QualityGate.class);
    QualityGateEvaluator.Measures measures = mock(QualityGateEvaluator.Measures.class);
    EvaluatedQualityGate evaluatedQualityGate = underTest.evaluate(gate, measures);
    assertThat(evaluatedQualityGate.getStatus()).isEqualTo(Metric.Level.OK);
  }
}
