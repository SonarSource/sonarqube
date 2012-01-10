/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

public class ChidamberKemererBridge extends Bridge {

  protected ChidamberKemererBridge() {
    super(true);
  }

  @Override
  public final void onFile(SourceFile squidFile, Resource sonarFile) {
    context.saveMeasure(sonarFile, CoreMetrics.DEPTH_IN_TREE, squidFile.getDouble(Metric.DIT));
    context.saveMeasure(sonarFile, CoreMetrics.NUMBER_OF_CHILDREN, squidFile.getDouble(Metric.NOC));
    context.saveMeasure(sonarFile, CoreMetrics.RFC, squidFile.getDouble(Metric.RFC));
    context.saveMeasure(sonarFile, CoreMetrics.LCOM4, getLcom4(squidFile));
  }

  static double getLcom4(SourceFile squidFile) {
    // Squid API does not support null values, so sometimes it returns LCOM4 values with value 0. It occurs
    // for example with files without any classes, like package-info.java.
    // Minimum value must be 1.
    return Math.max(squidFile.getDouble(Metric.LCOM4), 1.0);
  }
}
