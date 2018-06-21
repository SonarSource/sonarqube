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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.ERROR;

public final class EvaluationResultTextConverterImpl implements EvaluationResultTextConverter {
  private static final String VARIATION_METRIC_PREFIX = "new_";
  private static final String VARIATION = "variation";
  private static final Map<Condition.Operator, String> OPERATOR_LABELS = ImmutableMap.of(
    Condition.Operator.EQUALS, "=",
    Condition.Operator.NOT_EQUALS, "!=",
    Condition.Operator.GREATER_THAN, ">",
    Condition.Operator.LESS_THAN, "<");

  private final I18n i18n;
  private final Durations durations;
  private final Periods periods;
  private final PeriodHolder periodHolder;

  public EvaluationResultTextConverterImpl(I18n i18n, Durations durations, Periods periods, PeriodHolder periodHolder) {
    this.i18n = i18n;
    this.durations = durations;
    this.periods = periods;
    this.periodHolder = periodHolder;
  }

  @Override
  @CheckForNull
  public String asText(Condition condition, EvaluationResult evaluationResult) {
    requireNonNull(condition);
    if (evaluationResult.getLevel() == Measure.Level.OK) {
      return null;
    }
    return getAlertLabel(condition, evaluationResult.getLevel());
  }

  private String getAlertLabel(Condition condition, Measure.Level level) {
    boolean hasPeriod = condition.hasPeriod();
    String metric = i18n.message(Locale.ENGLISH, "metric." + condition.getMetric().getKey() + ".name", condition.getMetric().getName());

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(metric);

    if (hasPeriod && !condition.getMetric().getKey().startsWith(VARIATION_METRIC_PREFIX)) {
      String variation = i18n.message(Locale.ENGLISH, VARIATION, VARIATION).toLowerCase(Locale.ENGLISH);
      stringBuilder.append(" ").append(variation);
    }

    stringBuilder
      .append(" ").append(OPERATOR_LABELS.get(condition.getOperator())).append(" ")
      .append(alertValue(condition, level));

    if (hasPeriod) {
      Period period = periodHolder.getPeriod();
      stringBuilder.append(" ").append(periods.label(period.getMode(), period.getModeParameter(), DateUtils.longToDate(period.getSnapshotDate())));
    }

    return stringBuilder.toString();
  }

  private String alertValue(Condition condition, Measure.Level level) {
    String value = level == ERROR ? condition.getErrorThreshold() : condition.getWarningThreshold();
    if (condition.getMetric().getType() == Metric.MetricType.WORK_DUR) {
      return formatDuration(value);
    }
    return value;
  }

  private String formatDuration(String value) {
    return durations.format(Duration.create(Long.parseLong(value)));
  }
}
