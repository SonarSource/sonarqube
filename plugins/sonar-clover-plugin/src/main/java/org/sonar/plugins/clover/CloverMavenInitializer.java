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
package org.sonar.plugins.clover;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

/**
 * Provides {@link CloverMavenPluginHandler} and configures correct path to report.
 * Enabled only in Maven environment.
 */
public class CloverMavenInitializer extends Initializer implements CoverageExtension, DependsUponMavenPlugin {

  private CloverMavenPluginHandler handler;

  public CloverMavenInitializer(CloverMavenPluginHandler handler) {
    this.handler = handler;
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    if (project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)) {
      return handler;
    }
    return null;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return project.getAnalysisType().isDynamic(true) &&
        project.getFileSystem().hasJavaSourceFiles();
  }

  @Override
  public void execute(Project project) {
    Configuration conf = project.getConfiguration();
    if (!conf.containsKey(CloverConstants.REPORT_PATH_PROPERTY)) {
      String report = getReportPathFromPluginConfiguration(project);
      if (report == null) {
        report = getDefaultReportPath(project);
      }
      conf.setProperty(CloverConstants.REPORT_PATH_PROPERTY, report);
    }
  }

  private String getDefaultReportPath(Project project) {
    return project.getFileSystem().getReportOutputDir() + "/clover/clover.xml";
  }

  private String getReportPathFromPluginConfiguration(Project project) {
    MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), CloverConstants.MAVEN_GROUP_ID, CloverConstants.MAVEN_ARTIFACT_ID);
    if (plugin != null) {
      String path = plugin.getParameter("outputDirectory");
      if (path != null) {
        return path + "/clover.xml";
      }
    }
    return null;
  }
}
