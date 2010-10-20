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
