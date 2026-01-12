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
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.i18n.I18n;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

/**
 * Shared utility for formatting quality gate condition alerts.
 * Currently used by web server for UI-triggered QG changes.
 */
@ServerSide
@ComputeEngineSide
public class QualityGateConditionFormatter {

  private static final Logger LOGGER = LoggerFactory.getLogger(QualityGateConditionFormatter.class);
  private static final Map<Condition.Operator, String> OPERATOR_LABELS = Map.of(
    Condition.Operator.GREATER_THAN, ">",
    Condition.Operator.LESS_THAN, "<");

  private final I18n i18n;
  private final Durations durations;

  public QualityGateConditionFormatter(I18n i18n, Durations durations) {
    this.i18n = i18n;
    this.durations = durations;
  }

  public String buildAlertText(EvaluatedQualityGate evaluatedQG, MetricInfoProvider metricInfoProvider) {
    return evaluatedQG.getEvaluatedConditions().stream()
      .filter(c -> c.getStatus() == EvaluatedCondition.EvaluationStatus.ERROR)
      .map(c -> formatCondition(c, metricInfoProvider))
      .collect(Collectors.joining(", "));
  }

  private String formatCondition(EvaluatedCondition evaluatedCondition, MetricInfoProvider metricInfoProvider) {
    Condition condition = evaluatedCondition.getCondition();
    MetricInfo metricInfo = metricInfoProvider.getMetricInfo(condition.getMetricKey());

    return format(
      condition.getMetricKey(),
      metricInfo.name(),
      metricInfo.type(),
      condition.getOperator(),
      condition.getErrorThreshold()
    );
  }

  public interface MetricInfoProvider {
    MetricInfo getMetricInfo(String metricKey);
  }

  public record MetricInfo(@Nullable String name, @Nullable String type) {
  }

  public String format(String metricKey, @Nullable String metricName, @Nullable String metricType, Condition.Operator operator, String threshold) {
    String translatedMetricName = getTranslatedMetricName(metricKey, metricName);
    String operatorLabel = OPERATOR_LABELS.get(operator);
    String formattedThreshold = formatThreshold(threshold, metricType);

    return translatedMetricName + " " + operatorLabel + " " + formattedThreshold;
  }

  private String getTranslatedMetricName(String metricKey, @Nullable String defaultName) {
    String fallback = defaultName != null ? defaultName : metricKey;
    return i18n.message(Locale.ENGLISH, "metric." + metricKey + ".name", fallback);
  }

  private String formatThreshold(String threshold, @Nullable String metricType) {
    if (Metric.ValueType.WORK_DUR.name().equals(metricType)) {
      try {
        long durationInSeconds = Long.parseLong(threshold);
        return durations.format(Duration.create(durationInSeconds));
      } catch (NumberFormatException e) {
        LOGGER.warn("Failed to parse duration threshold: {}, using raw value instead", threshold, e);
      }
    }
    return threshold;
  }

}
