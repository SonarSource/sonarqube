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
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.indexer.QueryByMeasure;
import org.sonar.squid.indexer.QueryByParent;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.math.MeasuresDistribution;
import org.sonar.squid.measures.Metric;

import java.util.Map;

public class FunctionComplexityDistributionBridge extends Bridge {

  public static final int[] LIMITS = {1, 2, 4, 6, 8, 10, 12};

  protected FunctionComplexityDistributionBridge() {
    super(false);
  }

  @Override
  public final void onPackage(SourcePackage squidPackage, Resource sonarPackage) {
    if (squidPackage.getDouble(Metric.METHODS) > 0) {
      context.saveMeasure(sonarPackage, new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, getFunctionComplexityDistribution(squidPackage)));
    }
  }

  private String getFunctionComplexityDistribution(SourceCode unit) {
    MeasuresDistribution distribution = new MeasuresDistribution(squid.search(new QueryByParent(unit), new QueryByType(SourceMethod.class),
        new QueryByMeasure(Metric.ACCESSORS, QueryByMeasure.Operator.EQUALS, 0)));
    Map<Integer, Integer> distrib = distribution.distributeAccordingTo(Metric.COMPLEXITY, LIMITS);
    StringBuilder distribString = new StringBuilder(32);
    for (Map.Entry<Integer, Integer> entry : distrib.entrySet()) {
      distribString.append(entry.getKey()).append("=").append(entry.getValue().toString()).append(";");
    }
    distribString.setLength(distribString.length() - 1);
    return distribString.toString();
  }
}
