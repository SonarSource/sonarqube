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

import com.google.common.collect.ImmutableList;
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

import java.util.List;

public class LanguageDistributionDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return ImmutableList.of(CoreMetrics.LINES);
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return ImmutableList.of(
      CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION
    );
  }

  public void decorate(Resource resource, DecoratorContext context) {
    CountDistributionBuilder nclocDistribution = new CountDistributionBuilder(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION);
    if (ResourceUtils.isFile(resource)) {
      Language language = resource.getLanguage();
      Measure ncloc = context.getMeasure(CoreMetrics.NCLOC);
      if (language != null && ncloc != null) {
        nclocDistribution.add(language.getKey(), ncloc.getIntValue());
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
