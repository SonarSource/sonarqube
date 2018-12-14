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
package org.sonar.ce.task.projectanalysis.qualitygate;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;

import static java.util.Objects.requireNonNull;

public final class EvaluationResultTextConverterImpl implements EvaluationResultTextConverter {
  private static final Map<Condition.Operator, String> OPERATOR_LABELS = ImmutableMap.of(
    Condition.Operator.GREATER_THAN, ">",
    Condition.Operator.LESS_THAN, "<");

  private final I18n i18n;
  private final Durations durations;

  public EvaluationResultTextConverterImpl(I18n i18n, Durations durations) {
    this.i18n = i18n;
    this.durations = durations;
  }

  @Override
  @CheckForNull
  public String asText(Condition condition, EvaluationResult evaluationResult) {
    requireNonNull(condition);
    if (evaluationResult.getLevel() == Measure.Level.OK) {
      return null;
    }
    return getAlertLabel(condition);
  }

  private String getAlertLabel(Condition condition) {
    String metric = i18n.message(Locale.ENGLISH, "metric." + condition.getMetric().getKey() + ".name", condition.getMetric().getName());

    return metric +
      " " + OPERATOR_LABELS.get(condition.getOperator()) + " " +
      alertValue(condition);
  }

  private String alertValue(Condition condition) {
    if (condition.getMetric().getType() == Metric.MetricType.WORK_DUR) {
      return formatDuration(condition.getErrorThreshold());
    }
    return condition.getErrorThreshold();
  }

  private String formatDuration(String value) {
    return durations.format(Duration.create(Long.parseLong(value)));
  }
}
