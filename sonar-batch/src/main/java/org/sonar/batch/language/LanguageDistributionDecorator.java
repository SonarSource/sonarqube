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

package org.sonar.batch.language;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.CountDistributionBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

public class LanguageDistributionDecorator implements Decorator {

  private static final String UNKNOWN_LANGUAGE_KEY = "<null>";

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependsUpon
  public Metric dependsUponMetric() {
    return CoreMetrics.LINES;
  }

  @DependedUpon
  public Metric generatesMetric() {
    return CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    CountDistributionBuilder nclocDistribution = new CountDistributionBuilder(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION);
    if (ResourceUtils.isFile(resource)) {
      Language language = resource.getLanguage();
      Measure ncloc = context.getMeasure(CoreMetrics.NCLOC);
      if (ncloc != null) {
        nclocDistribution.add(language != null ? language.getKey() : UNKNOWN_LANGUAGE_KEY, ncloc.getIntValue());
      }
    } else {
      for (Measure measure : context.getChildrenMeasures(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)) {
        nclocDistribution.add(measure);
      }
    }
    Measure measure = nclocDistribution.build(false);
    if (measure != null) {
      context.saveMeasure(measure);
    }
  }

}
