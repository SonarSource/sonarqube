/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

@DependsUpon(value=DecoratorBarriers.END_OF_ALERTS_GENERATION)
public class GlobalAlertDecorator implements Decorator {

  @DependedUpon
  public Metric generatesAlertStatus() {
    return CoreMetrics.ALERT_STATUS;
  }
  
  public boolean shouldExecuteOnProject(Project project) {
    return ResourceUtils.isRootProject(project);
  }

  public void decorate(final Resource resource, final DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      decorateResource(context);
    }
  }

  private void decorateResource(DecoratorContext context) {
    Metric.Level globalLevel = Metric.Level.OK;
    List<String> labels = Lists.newArrayList();

    for (final Measure measure : context.getMeasures(MeasuresFilters.all())) {
      if (measure.getAlertStatus() != null) {          
        Metric.Level level = measure.getAlertStatus();
        String text = measure.getAlertText();
        if (!StringUtils.isBlank(text)) {
          labels.add(text);
        }

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


  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
