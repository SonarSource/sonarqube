/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.qualitygate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.timemachine.Periods;

public class QualityGateVerifier implements Decorator {

  private static final String VARIATION_METRIC_PREFIX = "new_";
  private static final String VARIATION = "variation";
  private static final Map<String, String> OPERATOR_LABELS = ImmutableMap.of(
    QualityGateConditionDto.OPERATOR_EQUALS, "=",
    QualityGateConditionDto.OPERATOR_NOT_EQUALS, "!=",
    QualityGateConditionDto.OPERATOR_GREATER_THAN, ">",
    QualityGateConditionDto.OPERATOR_LESS_THAN, "<");

  private QualityGate qualityGate;

  private Periods periods;
  private I18n i18n;
  private Durations durations;
  private BatchComponentCache resourceCache;

  public QualityGateVerifier(QualityGate qualityGate, BatchComponentCache resourceCache, Periods periods, I18n i18n, Durations durations) {
    this.qualityGate = qualityGate;
    this.resourceCache = resourceCache;
    this.periods = periods;
    this.i18n = i18n;
    this.durations = durations;
  }

  @DependedUpon
  public Metric generatesQualityGateStatus() {
    return CoreMetrics.ALERT_STATUS;
  }

  @DependsUpon
  public String dependsOnVariations() {
    return DecoratorBarriers.END_OF_TIME_MACHINE;
  }

  @DependsUpon
  public Collection<Metric> dependsUponMetrics() {
    Set<Metric> metrics = Sets.newHashSet();
    for (ResolvedCondition condition : qualityGate.conditions()) {
      metrics.add(condition.metric());
    }
    return metrics;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return qualityGate.isEnabled();
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (ResourceUtils.isRootProject(resource)) {
      checkProjectConditions(resource, context);
    }
  }

  private void checkProjectConditions(Resource project, DecoratorContext context) {
    Metric.Level globalLevel = Metric.Level.OK;
    QualityGateDetails details = new QualityGateDetails();
    List<String> labels = Lists.newArrayList();

    for (ResolvedCondition condition : qualityGate.conditions()) {
      Measure measure = context.getMeasure(condition.metric());
      if (measure != null) {
        Metric.Level level = ConditionUtils.getLevel(condition, measure);

        measure.setAlertStatus(level);
        String text = getText(project, condition, level);
        if (!StringUtils.isBlank(text)) {
          measure.setAlertText(text);
          labels.add(text);
        }

        context.saveMeasure(measure);

        if (Metric.Level.WARN == level && globalLevel != Metric.Level.ERROR) {
          globalLevel = Metric.Level.WARN;

        } else if (Metric.Level.ERROR == level) {
          globalLevel = Metric.Level.ERROR;
        }

        details.addCondition(condition, level, ConditionUtils.getValue(condition, measure));
      }
    }

    Measure globalMeasure = new Measure(CoreMetrics.ALERT_STATUS, globalLevel);
    globalMeasure.setAlertStatus(globalLevel);
    globalMeasure.setAlertText(StringUtils.join(labels, ", "));
    context.saveMeasure(globalMeasure);

    details.setLevel(globalLevel);
    Measure detailsMeasure = new Measure(CoreMetrics.QUALITY_GATE_DETAILS, details.toJson());
    context.saveMeasure(detailsMeasure);

  }

  private String getText(Resource project, ResolvedCondition condition, Metric.Level level) {
    if (level == Metric.Level.OK) {
      return null;
    }
    return getAlertLabel(project, condition, level);
  }

  private String getAlertLabel(Resource project, ResolvedCondition condition, Metric.Level level) {
    Integer alertPeriod = condition.period();
    String metric = i18n.message(Locale.ENGLISH, "metric." + condition.metricKey() + ".name", condition.metric().getName());

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(metric);

    if (alertPeriod != null && !condition.metricKey().startsWith(VARIATION_METRIC_PREFIX)) {
      String variation = i18n.message(Locale.ENGLISH, VARIATION, VARIATION).toLowerCase();
      stringBuilder.append(" ").append(variation);
    }

    stringBuilder
      .append(" ").append(operatorLabel(condition.operator())).append(" ")
      .append(alertValue(condition, level));

    // Disabled because snapshot is no more created by the batch
//    if (alertPeriod != null) {
//      Snapshot snapshot = resourceCache.get(project).snapshot();
//      stringBuilder.append(" ").append(periods.label(snapshot, alertPeriod));
//    }

    return stringBuilder.toString();
  }

  private String alertValue(ResolvedCondition condition, Metric.Level level) {
    String value = level.equals(Metric.Level.ERROR) ? condition.errorThreshold() : condition.warningThreshold();
    if (condition.metric().getType().equals(Metric.ValueType.WORK_DUR)) {
      return formatDuration(value);
    } else {
      return value;
    }
  }

  private String formatDuration(String value) {
    return durations.format(Locale.ENGLISH, Duration.create(Long.parseLong(value)), Durations.DurationFormat.SHORT);
  }

  private String operatorLabel(String operator) {
    return OPERATOR_LABELS.get(operator);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
