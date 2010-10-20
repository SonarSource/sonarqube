package org.sonar.plugins.cobertura.api;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Logs;
import org.sonar.plugins.cobertura.CoberturaMavenPluginHandler;

import java.io.File;

/**
 * @since 2.4
 */
public final class CoberturaUtils {

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
    String path = (String) project.getProperty(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY);
    if (path != null) {
      return project.getFileSystem().resolvePath(path);
    }
    return null;
  }

  private static File getReportFromPluginConfiguration(Project project) {
    MavenPlugin mavenPlugin = MavenPlugin.getPlugin(project.getPom(), CoberturaMavenPluginHandler.GROUP_ID, CoberturaMavenPluginHandler.ARTIFACT_ID);
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
