/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.squid.bridges;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;

public class OneToOneBridge extends BasicBridge {

  protected OneToOneBridge() {
    super(false);
  }

  @Override
  protected void onResource(SourceCode squidResource, Resource sonarResource) {
    context.saveMeasure(sonarResource, CoreMetrics.NCLOC, squidResource.getDouble(Metric.LINES_OF_CODE));
    context.saveMeasure(sonarResource, CoreMetrics.LINES, squidResource.getDouble(Metric.LINES));
    context.saveMeasure(sonarResource, CoreMetrics.COMMENT_LINES, squidResource.getDouble(Metric.COMMENT_LINES_WITHOUT_HEADER));
    context.saveMeasure(sonarResource, CoreMetrics.FUNCTIONS, squidResource.getDouble(Metric.METHODS));
    context.saveMeasure(sonarResource, CoreMetrics.ACCESSORS, squidResource.getDouble(Metric.ACCESSORS));
    context.saveMeasure(sonarResource, CoreMetrics.PUBLIC_API, squidResource.getDouble(Metric.PUBLIC_API));
    context.saveMeasure(sonarResource, CoreMetrics.CLASSES, squidResource.getDouble(Metric.CLASSES));
    context.saveMeasure(sonarResource, CoreMetrics.COMPLEXITY, squidResource.getDouble(Metric.COMPLEXITY));
    context.saveMeasure(sonarResource, CoreMetrics.STATEMENTS, squidResource.getDouble(Metric.STATEMENTS));
    context.saveMeasure(sonarResource, CoreMetrics.FILES, squidResource.getDouble(Metric.FILES));
    context.saveMeasure(sonarResource, CoreMetrics.COMMENTED_OUT_CODE_LINES, squidResource.getDouble(Metric.COMMENTED_OUT_CODE_LINES));
    context.saveMeasure(sonarResource, CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY, ParsingUtils.scaleValue(squidResource.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY) * 100, 2));
  }
}
