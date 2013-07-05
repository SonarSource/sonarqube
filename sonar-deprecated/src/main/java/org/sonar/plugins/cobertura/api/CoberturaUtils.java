/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.cobertura.api;

import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Logs;
import org.sonar.plugins.cobertura.base.CoberturaConstants;

import java.io.File;

/**
 * @since 2.4
 * @deprecated since 3.7 Used to keep backward compatibility since extraction
 * of Cobertura plugin.
 */
@Deprecated
public final class CoberturaUtils {

  public static final String COBERTURA_GROUP_ID = MavenUtils.GROUP_ID_CODEHAUS_MOJO;
  public static final String COBERTURA_ARTIFACT_ID = "cobertura-maven-plugin";

  /**
   * @deprecated in 2.8, because assumes that Sonar executed from Maven. Not used any more in sonar-cobertura-plugin.
   *             See http://jira.codehaus.org/browse/SONAR-2321
   */
  @Deprecated
  public static File getReport(Project project) {
    File report = getReportFromProperty(project);
    if (report == null) {
      report = getReportFromPluginConfiguration(project);
    }
    if (report == null) {
      report = getReportFromDefaultPath(project);
    }

    if (report == null || !report.exists() || !report.isFile()) {
      Logs.INFO.warn("Cobertura report not found at {}", report);
      report = null;
    }
    return report;
  }

  private static File getReportFromProperty(Project project) {
    String path = (String) project.getProperty(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY);
    if (path != null) {
      return project.getFileSystem().resolvePath(path);
    }
    return null;
  }

  private static File getReportFromPluginConfiguration(Project project) {
    MavenPlugin mavenPlugin = MavenPlugin.getPlugin(project.getPom(), COBERTURA_GROUP_ID, COBERTURA_ARTIFACT_ID);
    if (mavenPlugin != null) {
      String path = mavenPlugin.getParameter("outputDirectory");
      if (path != null) {
        return new File(project.getFileSystem().resolvePath(path), "coverage.xml");
      }
    }
    return null;
  }

  private static File getReportFromDefaultPath(Project project) {
    return new File(project.getFileSystem().getReportOutputDir(), "cobertura/coverage.xml");
  }

  private CoberturaUtils() {
  }

}
