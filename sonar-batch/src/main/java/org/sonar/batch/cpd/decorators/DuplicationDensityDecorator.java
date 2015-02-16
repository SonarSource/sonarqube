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
package org.sonar.batch.cpd.decorators;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.List;

public class DuplicationDensityDecorator implements Decorator {

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return Arrays.<Metric>asList(
      CoreMetrics.NCLOC,
      CoreMetrics.COMMENT_LINES,
      CoreMetrics.DUPLICATED_LINES,
      CoreMetrics.LINES);
  }

  @DependedUpon
  public Metric generatesMetric() {
    return CoreMetrics.DUPLICATED_LINES_DENSITY;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    Measure nbDuplicatedLines = context.getMeasure(CoreMetrics.DUPLICATED_LINES);
    if (nbDuplicatedLines == null) {
      return;
    }

    Double divisor = getNbLinesFromLocOrNcloc(context);
    if (divisor != null && divisor > 0.0) {
      context.saveMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY, calculate(nbDuplicatedLines.getValue(), divisor));
    }
  }

  private Double getNbLinesFromLocOrNcloc(DecoratorContext context) {
    Measure nbLoc = context.getMeasure(CoreMetrics.LINES);
    if (nbLoc != null) {
      // TODO test this branch
      return nbLoc.getValue();
    }
    Measure nbNcloc = context.getMeasure(CoreMetrics.NCLOC);
    if (nbNcloc != null) {
      Measure nbComments = context.getMeasure(CoreMetrics.COMMENT_LINES);
      Double nbLines = nbNcloc.getValue();
      return nbComments != null ? nbLines + nbComments.getValue() : nbLines;
    }
    return null;
  }

  protected Double calculate(Double dividend, Double divisor) {
    Double result = 100.0 * dividend / divisor;
    if (result < 100.0) {
      return result;
    }
    return 100.0;
  }

}
