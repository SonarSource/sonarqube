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
package org.sonar.plugins.squid.bridges;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.java.api.JavaClass;
import org.sonar.java.api.JavaMethod;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.measures.Metric;

public final class CopyBasicMeasuresBridge extends Bridge {

  protected CopyBasicMeasuresBridge() {
    super(false);
  }

  @Override
  public void onFile(SourceFile squidFile, Resource sonarResource) {
    copyStandard(squidFile, sonarResource);
    copy(squidFile, sonarResource, Metric.FILES, CoreMetrics.FILES);
    context.saveMeasure(sonarResource, CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY, ParsingUtils.scaleValue(squidFile.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY) * 100, 2));
  }

  @Override
  public void onClass(SourceClass squidClass, JavaClass sonarClass) {
    copyStandard(squidClass, sonarClass);
  }

  @Override
  public void onMethod(SourceMethod squidMethod, JavaMethod sonarMethod) {
    copyStandard(squidMethod, sonarMethod);
  }

  private void copyStandard(SourceCode squidCode, Resource sonarResource) {
    copy(squidCode, sonarResource, Metric.LINES_OF_CODE, CoreMetrics.NCLOC);
    copy(squidCode, sonarResource, Metric.LINES, CoreMetrics.LINES);
    copy(squidCode, sonarResource, Metric.COMMENT_LINES_WITHOUT_HEADER, CoreMetrics.COMMENT_LINES);
    copy(squidCode, sonarResource, Metric.PUBLIC_API, CoreMetrics.PUBLIC_API);
    copy(squidCode, sonarResource, Metric.COMPLEXITY, CoreMetrics.COMPLEXITY);
    copy(squidCode, sonarResource, Metric.STATEMENTS, CoreMetrics.STATEMENTS);
    copy(squidCode, sonarResource, Metric.COMMENTED_OUT_CODE_LINES, CoreMetrics.COMMENTED_OUT_CODE_LINES);
  }

  private void copy(SourceCode squidResource, Resource sonarResource, Metric squidMetric, org.sonar.api.measures.Metric sonarMetric) {
    context.saveMeasure(sonarResource, sonarMetric, squidResource.getDouble(squidMetric));
  }
}
