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
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import java.util.List;

/**
 * Compute overall coverage when it was not already saved by language plugin.
 */
public final class MissingOverallCoverageDecorator implements Decorator {

  @DependsUpon
  public Metric dependsUpon() {
    return CoreMetrics.LINES_TO_COVER;
  }

  @DependedUpon
  public List<Metric> provides() {
    return CoverageMeasuresBuilder.CoverageType.OVERALL.all();
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (Qualifiers.isFile(resource) && !MeasureUtils.hasValue(context.getMeasure(CoreMetrics.OVERALL_LINES_TO_COVER))) {
      copyMeasure(context, CoreMetrics.LINES_TO_COVER, CoreMetrics.OVERALL_LINES_TO_COVER);
      copyMeasure(context, CoreMetrics.UNCOVERED_LINES, CoreMetrics.OVERALL_UNCOVERED_LINES);
      copyMeasure(context, CoreMetrics.COVERAGE_LINE_HITS_DATA, CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA);
      copyMeasure(context, CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.OVERALL_CONDITIONS_TO_COVER);
      copyMeasure(context, CoreMetrics.UNCOVERED_CONDITIONS, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS);
      copyMeasure(context, CoreMetrics.CONDITIONS_BY_LINE, CoreMetrics.OVERALL_CONDITIONS_BY_LINE);
      copyMeasure(context, CoreMetrics.COVERED_CONDITIONS_BY_LINE, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE);
    }
  }

  private void copyMeasure(DecoratorContext context, Metric<?> from, Metric<?> to) {
    Measure sourceMeasure = context.getMeasure(from);
    if (sourceMeasure != null) {
      Double value = sourceMeasure.getValue();
      if (value != null) {
        context.saveMeasure(to, value);
      } else if (sourceMeasure.hasData()) {
        context.saveMeasure(new Measure(to, sourceMeasure.getData()));
      }
    }

  }
}
