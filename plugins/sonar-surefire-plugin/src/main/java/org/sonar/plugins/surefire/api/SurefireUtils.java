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
package org.sonar.plugins.surefire.api;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenSurefireUtils;
import org.sonar.api.resources.Project;

import java.io.File;

/**
 * @since 2.4
 */
public final class SurefireUtils {

  public static File getReportsDirectory(Project project) {
    File dir = getReportsDirectoryFromProperty(project);
    if (dir == null) {
      dir = getReportsDirectoryFromPluginConfiguration(project);
    }
    if (dir == null) {
      dir = getReportsDirectoryFromDefaultConfiguration(project);
    }
    return dir;
  }

  private static File getReportsDirectoryFromProperty(Project project) {
    String path = (String) project.getProperty(CoreProperties.SUREFIRE_REPORTS_PATH_PROPERTY);
    if (path != null) {
      return project.getFileSystem().resolvePath(path);
    }
    return null;
  }

  private static File getReportsDirectoryFromPluginConfiguration(Project project) {
    MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), MavenSurefireUtils.GROUP_ID, MavenSurefireUtils.ARTIFACT_ID);
    if (plugin != null) {
      String path = plugin.getParameter("reportsDirectory");
      if (path != null) {
        return project.getFileSystem().resolvePath(path);
      }
    }
    return null;
  }

  private static File getReportsDirectoryFromDefaultConfiguration(Project project) {
    return new File(project.getFileSystem().getBuildDir(), "surefire-reports");
  }

  private SurefireUtils() {
  }

}
