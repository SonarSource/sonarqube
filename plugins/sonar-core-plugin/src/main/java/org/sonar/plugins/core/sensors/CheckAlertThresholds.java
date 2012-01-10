/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.List;

public class CheckAlertThresholds implements Decorator {

  private final RulesProfile profile;

  public CheckAlertThresholds(RulesProfile profile) {
    this.profile = profile;
  }

  @DependedUpon
  public Metric generatesAlertStatus() {
    return CoreMetrics.ALERT_STATUS;
  }

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    List<Metric> metrics = Lists.newLinkedList();
    for (Alert alert : profile.getAlerts()) {
      metrics.add(alert.getMetric());
    }
    return metrics;
  }


  public boolean shouldExecuteOnProject(Project project) {
    return profile != null
        && profile.getAlerts() != null
        && profile.getAlerts().size() > 0
        && ResourceUtils.isRootProject(project);
  }

  public void decorate(final Resource resource, final DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      decorateResource(context);
    }
  }

  private void decorateResource(DecoratorContext context) {
    Metric.Level globalLevel = Metric.Level.OK;
    List<String> labels = Lists.newArrayList();

    for (final Alert alert : profile.getAlerts()) {
      Measure measure = context.getMeasure(alert.getMetric());
      if (measure != null) {
        Metric.Level level = AlertUtils.getLevel(alert, measure);

        measure.setAlertStatus(level);
        String text = getText(alert, level);
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
      }
    }

    Measure globalMeasure = new Measure(CoreMetrics.ALERT_STATUS, globalLevel);
    globalMeasure.setAlertStatus(globalLevel);
    globalMeasure.setAlertText(StringUtils.join(labels, ", "));
    context.saveMeasure(globalMeasure);
  }

  private boolean shouldDecorateResource(final Resource resource) {
    return ResourceUtils.isRootProject(resource);
  }

  private String getText(Alert alert, Metric.Level level) {
    if (level == Metric.Level.OK) {
      return null;
    }
    return alert.getAlertLabel(level);
  }


  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
