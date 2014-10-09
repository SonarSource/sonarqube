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
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.List;

public class CommentDensityDecorator implements Decorator {

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return Arrays.<Metric>asList(CoreMetrics.NCLOC, CoreMetrics.COMMENT_LINES, CoreMetrics.PUBLIC_API, CoreMetrics.PUBLIC_UNDOCUMENTED_API);
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return Arrays.<Metric>asList(CoreMetrics.COMMENT_LINES_DENSITY, CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY);
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    saveCommentsDensity(context);
    savePublicApiDensity(context);
  }

  private void saveCommentsDensity(DecoratorContext context) {
    if (context.getMeasure(CoreMetrics.COMMENT_LINES_DENSITY) != null) {
      return;
    }

    Measure ncloc = context.getMeasure(CoreMetrics.NCLOC);
    Measure comments = context.getMeasure(CoreMetrics.COMMENT_LINES);
    if (MeasureUtils.hasValue(ncloc) && MeasureUtils.hasValue(comments) && (comments.getValue() + ncloc.getValue()) > 0) {
      double val = 100.0 * (comments.getValue() / (comments.getValue() + ncloc.getValue()));
      context.saveMeasure(new Measure(CoreMetrics.COMMENT_LINES_DENSITY, val));
    }
  }

  private void savePublicApiDensity(DecoratorContext context) {
    if (context.getMeasure(CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY) != null) {
      return;
    }

    Measure publicApi = context.getMeasure(CoreMetrics.PUBLIC_API);
    Measure publicUndocApi = context.getMeasure(CoreMetrics.PUBLIC_UNDOCUMENTED_API);

    if (MeasureUtils.hasValue(publicApi) && MeasureUtils.hasValue(publicUndocApi) && publicApi.getValue() > 0) {
      double documentedAPI = publicApi.getValue() - publicUndocApi.getValue();
      Double value = 100.0 * (documentedAPI / publicApi.getValue());
      context.saveMeasure(new Measure(CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY, value));
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
