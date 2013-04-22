/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.design.batch;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;

import java.util.Collection;
import java.util.List;

public class SuspectLcom4DensityDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public final Metric generatesMetric() {
    return CoreMetrics.SUSPECT_LCOM4_DENSITY;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (ResourceUtils.isFile(resource)) {
      // do nothing
    } else if (Scopes.isDirectory(resource)) {
      decorateDirectory(context);

    } else if (Scopes.isProject(resource)) {
      decorateProject(context);
    }
  }

  private void decorateProject(DecoratorContext context) {
    double total = 0.0;
    int totalFiles = 0;

    List<DecoratorContext> children = context.getChildren();
    boolean hasLcom4=false;
    for (DecoratorContext child : children) {
      int files = MeasureUtils.getValue(child.getMeasure(CoreMetrics.FILES), 0.0).intValue();
      totalFiles += files;
      Measure childSuspectDensity = child.getMeasure(CoreMetrics.SUSPECT_LCOM4_DENSITY);
      if (childSuspectDensity!=null && childSuspectDensity.getValue()!=null) {
        hasLcom4=true;
        total += childSuspectDensity.getValue() * files;
      }
    }

    if (hasLcom4 && totalFiles > 0) {
      context.saveMeasure(CoreMetrics.SUSPECT_LCOM4_DENSITY, (total / totalFiles));
    }
  }

  private void decorateDirectory(DecoratorContext context) {
    Collection<Measure> fileLcoms = context.getChildrenMeasures(CoreMetrics.LCOM4);
    double files = MeasureUtils.getValue(context.getMeasure(CoreMetrics.FILES), 0.0);
    if (!fileLcoms.isEmpty() && files>0.0) {
      double suspectFiles = 0.0;

      // directory children are files
      for (Measure fileLcom : fileLcoms) {
        if (MeasureUtils.getValue(fileLcom, 0.0) > 1.0) {
          suspectFiles++;
        }
      }
      double density = (suspectFiles / files) * 100.0;
      context.saveMeasure(CoreMetrics.SUSPECT_LCOM4_DENSITY, density);
    }
  }
}
