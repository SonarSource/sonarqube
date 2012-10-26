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
package org.sonar.plugins.cobertura;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cobertura.api.CoberturaUtils;
import org.sonar.plugins.java.api.JavaSettings;

/**
 * Provides {@link CoberturaMavenPluginHandler} and configures correct path to report.
 * Enabled only in Maven environment.
 */
public class CoberturaMavenInitializer extends Initializer implements CoverageExtension, DependsUponMavenPlugin {

  private CoberturaMavenPluginHandler handler;
  private JavaSettings javaSettings;

  public CoberturaMavenInitializer(CoberturaMavenPluginHandler handler, JavaSettings javaSettings) {
    this.handler = handler;
    this.javaSettings = javaSettings;
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    if (project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)) {
      return handler;
    }
    return null;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey())
      && CoberturaPlugin.PLUGIN_KEY.equals(javaSettings.getEnabledCoveragePlugin())
      && !project.getFileSystem().mainFiles(Java.KEY).isEmpty()
      && project.getAnalysisType().isDynamic(true);
  }

  @Override
  public void execute(Project project) {
    Configuration conf = project.getConfiguration();
    if (!conf.containsKey(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY)) {
      String report = getReportPathFromPluginConfiguration(project);
      if (report == null) {
        report = getDefaultReportPath(project);
      }
      conf.setProperty(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY, report);
    }
  }

  private static String getDefaultReportPath(Project project) {
    return project.getFileSystem().getReportOutputDir() + "/cobertura/coverage.xml";
  }

  private static String getReportPathFromPluginConfiguration(Project project) {
    MavenPlugin mavenPlugin = MavenPlugin.getPlugin(
        project.getPom(),
        CoberturaUtils.COBERTURA_GROUP_ID,
        CoberturaUtils.COBERTURA_ARTIFACT_ID);
    if (mavenPlugin != null) {
      String path = mavenPlugin.getParameter("outputDirectory");
      if (path != null) {
        return path + "/coverage.xml";
      }
    }
    return null;
  }

}
