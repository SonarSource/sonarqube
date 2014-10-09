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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Collection;

/**
 * @since 2.2
 */
public final class FilesDecorator implements Decorator {

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public Metric generateDirectoriesMetric() {
    return CoreMetrics.FILES;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (MeasureUtils.hasValue(context.getMeasure(CoreMetrics.FILES))) {
      return;
    }

    if (Resource.QUALIFIER_CLASS.equals(resource.getQualifier()) || Resource.QUALIFIER_FILE.equals(resource.getQualifier())) {
      context.saveMeasure(CoreMetrics.FILES, 1.0);

    } else {
      Collection<Measure> childrenMeasures = context.getChildrenMeasures(CoreMetrics.FILES);
      Double sum = MeasureUtils.sum(false, childrenMeasures);
      if (sum != null) {
        context.saveMeasure(CoreMetrics.FILES, sum);
      }
    }
  }
}
