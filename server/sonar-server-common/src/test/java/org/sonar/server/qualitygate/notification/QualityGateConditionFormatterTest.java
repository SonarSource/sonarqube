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
package org.sonar.server.qualitygate.notification;

import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.i18n.I18n;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.notification.QualityGateConditionFormatter.MetricInfo;
import org.sonar.server.qualitygate.notification.QualityGateConditionFormatter.MetricInfoProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QualityGateConditionFormatterTest {

  private final I18n i18n = mock(I18n.class);
  private final Durations durations = mock(Durations.class);
  private final QualityGateConditionFormatter underTest = new QualityGateConditionFormatter(i18n, durations);

  @Test
  void format_uses_translated_metric_name() {
    when(i18n.message(Locale.ENGLISH, "metric.new_coverage.name", "Coverage on New Code"))
      .thenReturn("Coverage on New Code");

    String result = underTest.format("new_coverage", "Coverage on New Code", "PERCENT",
      Condition.Operator.LESS_THAN, "80");

    assertThat(result).isEqualTo("Coverage on New Code < 80");
    verify(i18n).message(Locale.ENGLISH, "metric.new_coverage.name", "Coverage on New Code");
  }

  @Test
  void format_fallsback_to_metric_name_when_translation_not_found() {
    when(i18n.message(Locale.ENGLISH, "metric.custom_metric.name", "Custom Metric"))
      .thenReturn("Custom Metric");

    String result = underTest.format("custom_metric", "Custom Metric", "INT",
      Condition.Operator.GREATER_THAN, "100");

    assertThat(result).isEqualTo("Custom Metric > 100");
  }

  @Test
  void format_fallsback_to_metric_key_when_name_is_null() {
    when(i18n.message(Locale.ENGLISH, "metric.custom_metric.name", "custom_metric"))
      .thenReturn("custom_metric");

    String result = underTest.format("custom_metric", null, "INT",
      Condition.Operator.GREATER_THAN, "100");

    assertThat(result).isEqualTo("custom_metric > 100");
  }

  @Test
  void format_uses_durations_for_work_dur_metrics() {
    when(i18n.message(any(Locale.class), any(String.class), any(String.class)))
      .thenReturn("Effort to Reach Maintainability Rating A");
    when(durations.format(any(Duration.class))).thenReturn("2h 30min");

    String result = underTest.format("new_technical_debt", "Effort to Reach Maintainability Rating A",
      Metric.ValueType.WORK_DUR.name(), Condition.Operator.GREATER_THAN, "9000");

    assertThat(result).isEqualTo("Effort to Reach Maintainability Rating A > 2h 30min");
    verify(durations).format(Duration.create(9000L));
  }

  @Test
  void format_returns_raw_threshold_for_non_work_dur_metrics() {
    when(i18n.message(any(Locale.class), any(String.class), any(String.class)))
      .thenReturn("Coverage");

    String result = underTest.format("coverage", "Coverage", "PERCENT",
      Condition.Operator.LESS_THAN, "80");

    assertThat(result).isEqualTo("Coverage < 80");
  }

  @Test
  void format_handles_null_metric_type() {
    when(i18n.message(any(Locale.class), any(String.class), any(String.class)))
      .thenReturn("Custom Metric");

    String result = underTest.format("custom", "Custom Metric", null,
      Condition.Operator.LESS_THAN, "50");

    assertThat(result).isEqualTo("Custom Metric < 50");
  }

  @Test
  void format_handles_invalid_duration_threshold() {
    when(i18n.message(any(Locale.class), any(String.class), any(String.class)))
      .thenReturn("Technical Debt");

    // Invalid number format - should fallback to raw threshold
    String result = underTest.format("technical_debt", "Technical Debt",
      Metric.ValueType.WORK_DUR.name(), Condition.Operator.GREATER_THAN, "invalid");

    assertThat(result).isEqualTo("Technical Debt > invalid");
  }

  @Test
  void buildAlertText_returns_empty_string_when_no_error_conditions() {
    Condition condition = new Condition("coverage", Condition.Operator.LESS_THAN, "80");
    QualityGate qualityGate = new QualityGate("1", "Default", Set.of(condition));
    EvaluatedQualityGate evaluatedQG = EvaluatedQualityGate.newBuilder()
      .setQualityGate(qualityGate)
      .setStatus(Metric.Level.OK)
      .addEvaluatedCondition(new EvaluatedCondition(
        condition,
        EvaluatedCondition.EvaluationStatus.OK,
        "85"))
      .build();

    MetricInfoProvider provider = metricKey -> new MetricInfo("Coverage", "PERCENT");

    String result = underTest.buildAlertText(evaluatedQG, provider);

    assertThat(result).isEmpty();
  }

  @Test
  void buildAlertText_returns_single_condition_when_one_error() {
    when(i18n.message(Locale.ENGLISH, "metric.coverage.name", "Coverage"))
      .thenReturn("Coverage");

    Condition condition = new Condition("coverage", Condition.Operator.LESS_THAN, "80");
    QualityGate qualityGate = new QualityGate("1", "Default", Set.of(condition));
    EvaluatedQualityGate evaluatedQG = EvaluatedQualityGate.newBuilder()
      .setQualityGate(qualityGate)
      .setStatus(Metric.Level.ERROR)
      .addEvaluatedCondition(new EvaluatedCondition(
        condition,
        EvaluatedCondition.EvaluationStatus.ERROR,
        "75"))
      .build();

    MetricInfoProvider provider = metricKey -> new MetricInfo("Coverage", "PERCENT");

    String result = underTest.buildAlertText(evaluatedQG, provider);

    assertThat(result).isEqualTo("Coverage < 80");
  }

  @Test
  void buildAlertText_joins_multiple_error_conditions_with_comma() {
    when(i18n.message(Locale.ENGLISH, "metric.coverage.name", "Coverage"))
      .thenReturn("Coverage");
    when(i18n.message(Locale.ENGLISH, "metric.duplicated_lines_density.name", "Duplicated Lines (%)"))
      .thenReturn("Duplicated Lines (%)");

    Condition coverageCondition = new Condition("coverage", Condition.Operator.LESS_THAN, "80");
    Condition duplicationCondition = new Condition("duplicated_lines_density", Condition.Operator.GREATER_THAN, "3");
    QualityGate qualityGate = new QualityGate("1", "Default", Set.of(coverageCondition, duplicationCondition));
    EvaluatedQualityGate evaluatedQG = EvaluatedQualityGate.newBuilder()
      .setQualityGate(qualityGate)
      .setStatus(Metric.Level.ERROR)
      .addEvaluatedCondition(new EvaluatedCondition(
        coverageCondition,
        EvaluatedCondition.EvaluationStatus.ERROR,
        "75"))
      .addEvaluatedCondition(new EvaluatedCondition(
        duplicationCondition,
        EvaluatedCondition.EvaluationStatus.ERROR,
        "5"))
      .build();

    MetricInfoProvider provider = metricKey -> {
      if ("coverage".equals(metricKey)) {
        return new MetricInfo("Coverage", "PERCENT");
      }
      return new MetricInfo("Duplicated Lines (%)", "PERCENT");
    };

    String result = underTest.buildAlertText(evaluatedQG, provider);

    assertThat(result).isEqualTo("Coverage < 80, Duplicated Lines (%) > 3");
  }

  @Test
  void buildAlertText_filters_out_ok_conditions() {
    when(i18n.message(Locale.ENGLISH, "metric.coverage.name", "Coverage"))
      .thenReturn("Coverage");
    when(i18n.message(Locale.ENGLISH, "metric.bugs.name", "Bugs"))
      .thenReturn("Bugs");

    Condition coverageCondition = new Condition("coverage", Condition.Operator.LESS_THAN, "80");
    Condition bugsCondition = new Condition("bugs", Condition.Operator.GREATER_THAN, "0");
    QualityGate qualityGate = new QualityGate("1", "Default", Set.of(coverageCondition, bugsCondition));
    EvaluatedQualityGate evaluatedQG = EvaluatedQualityGate.newBuilder()
      .setQualityGate(qualityGate)
      .setStatus(Metric.Level.ERROR)
      .addEvaluatedCondition(new EvaluatedCondition(
        coverageCondition,
        EvaluatedCondition.EvaluationStatus.ERROR,
        "75"))
      .addEvaluatedCondition(new EvaluatedCondition(
        bugsCondition,
        EvaluatedCondition.EvaluationStatus.OK,
        "0"))
      .build();

    MetricInfoProvider provider = metricKey -> {
      if ("coverage".equals(metricKey)) {
        return new MetricInfo("Coverage", "PERCENT");
      }
      return new MetricInfo("Bugs", "INT");
    };

    String result = underTest.buildAlertText(evaluatedQG, provider);

    assertThat(result).isEqualTo("Coverage < 80");
  }

  @Test
  void buildAlertText_handles_work_dur_metrics() {
    when(i18n.message(Locale.ENGLISH, "metric.new_technical_debt.name", "Added Technical Debt"))
      .thenReturn("Added Technical Debt");
    when(durations.format(any(Duration.class))).thenReturn("2h 30min");

    Condition condition = new Condition("new_technical_debt", Condition.Operator.GREATER_THAN, "9000");
    QualityGate qualityGate = new QualityGate("1", "Default", Set.of(condition));
    EvaluatedQualityGate evaluatedQG = EvaluatedQualityGate.newBuilder()
      .setQualityGate(qualityGate)
      .setStatus(Metric.Level.ERROR)
      .addEvaluatedCondition(new EvaluatedCondition(
        condition,
        EvaluatedCondition.EvaluationStatus.ERROR,
        "10000"))
      .build();

    MetricInfoProvider provider = metricKey -> new MetricInfo("Added Technical Debt", Metric.ValueType.WORK_DUR.name());

    String result = underTest.buildAlertText(evaluatedQG, provider);

    assertThat(result).isEqualTo("Added Technical Debt > 2h 30min");
    verify(durations).format(Duration.create(9000L));
  }

  @Test
  void buildAlertText_handles_null_metric_name() {
    when(i18n.message(Locale.ENGLISH, "metric.custom_metric.name", "custom_metric"))
      .thenReturn("custom_metric");

    Condition condition = new Condition("custom_metric", Condition.Operator.GREATER_THAN, "100");
    QualityGate qualityGate = new QualityGate("1", "Default", Set.of(condition));
    EvaluatedQualityGate evaluatedQG = EvaluatedQualityGate.newBuilder()
      .setQualityGate(qualityGate)
      .setStatus(Metric.Level.ERROR)
      .addEvaluatedCondition(new EvaluatedCondition(
        condition,
        EvaluatedCondition.EvaluationStatus.ERROR,
        "150"))
      .build();

    MetricInfoProvider provider = metricKey -> new MetricInfo(null, "INT");

    String result = underTest.buildAlertText(evaluatedQG, provider);

    assertThat(result).isEqualTo("custom_metric > 100");
  }

  @Test
  void buildAlertText_handles_null_metric_type() {
    when(i18n.message(Locale.ENGLISH, "metric.custom.name", "Custom"))
      .thenReturn("Custom");

    Condition condition = new Condition("custom", Condition.Operator.LESS_THAN, "50");
    QualityGate qualityGate = new QualityGate("1", "Default", Set.of(condition));
    EvaluatedQualityGate evaluatedQG = EvaluatedQualityGate.newBuilder()
      .setQualityGate(qualityGate)
      .setStatus(Metric.Level.ERROR)
      .addEvaluatedCondition(new EvaluatedCondition(
        condition,
        EvaluatedCondition.EvaluationStatus.ERROR,
        "40"))
      .build();

    MetricInfoProvider provider = metricKey -> new MetricInfo("Custom", null);

    String result = underTest.buildAlertText(evaluatedQG, provider);

    assertThat(result).isEqualTo("Custom < 50");
  }

}
