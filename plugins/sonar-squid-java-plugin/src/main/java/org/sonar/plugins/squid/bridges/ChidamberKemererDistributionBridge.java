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
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.indexer.QueryByMeasure;
import org.sonar.squid.indexer.QueryByParent;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.math.MeasuresDistribution;
import org.sonar.squid.measures.Metric;

import java.util.Map;

public class ChidamberKemererDistributionBridge extends Bridge {

  public static final int[] LCOM4_LIMITS = {2, 3, 4, 5, 10};// 1 is excluded
  public static final int[] RFC_LIMITS = {0,5,10,20,30,50,90,150};

  protected ChidamberKemererDistributionBridge() {
    super(true);
  }

  @Override
  public final void onPackage(SourcePackage squidPackage, Resource sonarPackage) {
    context.saveMeasure(sonarPackage, new Measure(CoreMetrics.LCOM4_DISTRIBUTION, getDistribution(squidPackage, Metric.LCOM4, LCOM4_LIMITS)));
    context.saveMeasure(sonarPackage, new Measure(CoreMetrics.RFC_DISTRIBUTION, getDistribution(squidPackage, Metric.RFC, RFC_LIMITS)));
  }

  private String getDistribution(SourcePackage squidPackage, Metric metric, int[] limits) {
    MeasuresDistribution distribution = new MeasuresDistribution(squid.search(new QueryByParent(squidPackage), new QueryByType(SourceClass.class),
        new QueryByMeasure(metric, QueryByMeasure.Operator.GREATER_THAN_EQUALS, 0)));
    Map<Integer, Integer> distrib = distribution.distributeAccordingTo(metric, limits);
    StringBuilder distribString = new StringBuilder(32);
    for (Map.Entry<Integer, Integer> entry : distrib.entrySet()) {
      distribString.append(entry.getKey()).append("=").append(entry.getValue().toString()).append(";");
    }
    distribString.setLength(distribString.length() - 1);
    return distribString.toString();
  }
}
