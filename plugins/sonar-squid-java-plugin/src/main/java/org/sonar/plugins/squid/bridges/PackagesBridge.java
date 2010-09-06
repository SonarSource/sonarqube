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

import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.measures.Metric;

public class PackagesBridge extends Bridge {

  protected PackagesBridge() {
    super(false);
  }

  @Override
  public void onProject(SourceProject squidProject, Project sonarProject) {
    context.saveMeasure(sonarProject, CoreMetrics.PACKAGES, squidProject.getDouble(Metric.PACKAGES));
  }

  @Override
  public void onPackage(SourcePackage squidPackage, Resource sonarPackage) {
    context.saveMeasure(sonarPackage, CoreMetrics.PACKAGES, squidPackage.getDouble(Metric.PACKAGES));
  }
}
