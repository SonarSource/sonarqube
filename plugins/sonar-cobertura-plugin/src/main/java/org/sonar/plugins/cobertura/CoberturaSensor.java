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
package org.sonar.plugins.cobertura;

import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.io.File;

public class CoberturaSensor extends AbstractCoverageExtension implements Sensor, DependsUponMavenPlugin {

  private CoberturaMavenPluginHandler handler;

  public CoberturaSensor(CoberturaMavenPluginHandler handler) {
    this.handler = handler;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return super.shouldExecuteOnProject(project) && project.getFileSystem().hasJavaSourceFiles();
  }

  public void analyse(Project project, SensorContext context) {
    File report = getReport(project);
    if (report != null) {
      parseReport(report, context);
    }
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    if (project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)) {
      return handler;
    }
    return null;
  }

  protected File getReport(Project project) {
    File report = getReportFromProperty(project);
    if (report == null) {
      report = getReportFromPluginConfiguration(project);
    }
    if (report == null) {
      report = getReportFromDefaultPath(project);
    }

    if (report == null || !report.exists() || !report.isFile()) {
      LoggerFactory.getLogger(CoberturaSensor.class).warn("Cobertura report not found at {}", report);
      report = null;
    }
    return report;
  }

  private File getReportFromProperty(Project project) {
    String path = (String) project.getProperty(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY);
    if (path != null) {
      return project.getFileSystem().resolvePath(path);
    }
    return null;
  }

  private File getReportFromPluginConfiguration(Project project) {
    MavenPlugin mavenPlugin = MavenPlugin.getPlugin(project.getPom(), CoberturaMavenPluginHandler.GROUP_ID,
        CoberturaMavenPluginHandler.ARTIFACT_ID);
    if (mavenPlugin != null) {
      String path = mavenPlugin.getParameter("outputDirectory");
      if (path != null) {
        return new File(project.getFileSystem().resolvePath(path), "coverage.xml");
      }
    }
    return null;
  }

  private File getReportFromDefaultPath(Project project) {
    return new File(project.getFileSystem().getReportOutputDir(), "cobertura/coverage.xml");
  }

  protected void parseReport(File xmlFile, final SensorContext context) {
    LoggerFactory.getLogger(CoberturaSensor.class).info("parsing {}", xmlFile);
    new AbstractCoberturaParser() {
      @Override
      protected Resource<?> getResource(String fileName) {
        return new JavaFile(fileName);
      }
    }.parseReport(xmlFile, context);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
