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

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

public final class LinesToCoverDecorator implements Decorator {

  private final Settings settings;

  public LinesToCoverDecorator(Settings settings) {
    this.settings = settings;
  }

  @DependsUpon
  public Metric dependsUpon() {
    return CoreMetrics.NCLOC;
  }

  @DependedUpon
  public Metric provides() {
    return CoreMetrics.LINES_TO_COVER;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return !settings.getBoolean(CoreProperties.COVERAGE_UNFORCED_KEY);
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (Qualifiers.isFile(resource)) {
      if (!MeasureUtils.hasValue(context.getMeasure(CoreMetrics.LINES_TO_COVER))) {
        double ncloc = MeasureUtils.getValue(context.getMeasure(CoreMetrics.NCLOC), 0.0);
        context.saveMeasure(CoreMetrics.LINES_TO_COVER, ncloc);
        context.saveMeasure(CoreMetrics.UNCOVERED_LINES, ncloc);
      }
    }
  }
}
