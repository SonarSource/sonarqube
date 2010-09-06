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
package org.sonar.plugins.clover;

import org.slf4j.LoggerFactory;
import org.sonar.api.Plugins;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

import java.io.File;

public class CloverSensor extends AbstractCoverageExtension implements Sensor, DependsUponMavenPlugin {

  private CloverMavenPluginHandler handler;

  public CloverSensor(Plugins plugins, CloverMavenPluginHandler handler) {
    super(plugins);
    this.handler = handler;
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    if (project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)) {
      return handler;
    }
    return null;
  }

  public void analyse(Project project, SensorContext context) {
    File report = getReport(project);
    if (reportExists(report)) {
      new XmlReportParser(context).collect(report);
    } else {
      LoggerFactory.getLogger(getClass()).info("Clover XML report not found");
    }
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return super.shouldExecuteOnProject(project) &&
        project.getFileSystem().hasJavaSourceFiles();
  }

  protected File getReport(Project pom) {
    File report = getReportFromProperty(pom);
    if (report == null) {
      report = getReportFromPluginConfiguration(pom);
    }
    if (report == null) {
      report = getReportFromDefaultPath(pom);
    }
    return report;
  }

  private File getReportFromProperty(Project pom) {
    String path = (String) pom.getProperty(CloverConstants.REPORT_PATH_PROPERTY);
    if (path != null) {
      return pom.getFileSystem().resolvePath(path);
    }
    return null;
  }

  private File getReportFromPluginConfiguration(Project pom) {
    MavenPlugin plugin = MavenPlugin.getPlugin(pom.getPom(), CloverConstants.MAVEN_GROUP_ID, CloverConstants.MAVEN_ARTIFACT_ID);
    if (plugin != null) {
      String path = plugin.getParameter("outputDirectory");
      if (path != null) {
        return new File(pom.getFileSystem().resolvePath(path), "clover.xml");
      }
    }
    return null;
  }

  private File getReportFromDefaultPath(Project pom) {
    return new File(pom.getFileSystem().getReportOutputDir(), "clover/clover.xml");
  }

  private boolean reportExists(File report) {
    return report != null && report.exists() && report.isFile();
  }
}
