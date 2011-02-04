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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.batch.maven.MavenSurefireUtils;
import org.sonar.api.resources.Project;
import org.sonar.java.api.JavaUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class CloverMavenPluginHandler implements MavenPluginHandler {

  private Configuration conf;

  public CloverMavenPluginHandler(Configuration conf) {
    this.conf = conf;
  }

  public String getGroupId() {
    return CloverConstants.MAVEN_GROUP_ID;
  }

  public String getArtifactId() {
    return CloverConstants.MAVEN_ARTIFACT_ID;
  }

  public String getVersion() {
    return conf.getString(CloverConstants.VERSION_PROPERTY, CloverConstants.DEFAULT_VERSION);
  }

  public boolean isFixedVersion() {
    return false;
  }

  public String[] getGoals() {
    return new String[] { "instrument", "clover" };
  }

  public void configure(Project project, MavenPlugin cloverPlugin) {
    configureParameters(project, cloverPlugin);
    configureLicense(project, cloverPlugin);
    configureSurefire(project, cloverPlugin);
  }

  private void configureSurefire(Project project, MavenPlugin cloverPlugin) {
    MavenSurefireUtils.configure(project);
    boolean skipCloverLaunch = project.getConfiguration().getBoolean("maven.clover.skip", false);
    if (!skipCloverLaunch) {
      String skipInPomConfig = cloverPlugin.getParameter("skip");
      skipCloverLaunch = StringUtils.isNotBlank(skipInPomConfig) ? Boolean.parseBoolean(skipInPomConfig) : false;
    }
    if (!project.getConfiguration().containsKey(CoreProperties.SUREFIRE_REPORTS_PATH_PROPERTY) && !skipCloverLaunch) {
      project.getConfiguration().setProperty(CoreProperties.SUREFIRE_REPORTS_PATH_PROPERTY,
          new File(project.getFileSystem().getBuildDir(), "clover/surefire-reports").getAbsolutePath());
    }
  }

  protected void configureParameters(Project project, MavenPlugin cloverPlugin) {
    cloverPlugin.setParameter("generateXml", "true");
    String javaVersion = JavaUtils.getTargetVersion(project);
    if (javaVersion != null) {
      cloverPlugin.setParameter("jdk", javaVersion);
    }
    if (project.getExclusionPatterns() != null) {
      for (String pattern : project.getExclusionPatterns()) {
        cloverPlugin.addParameter("excludes/exclude", pattern);
      }
    }
  }

  protected void configureLicense(Project project, MavenPlugin cloverPlugin) {
    if (!hasLicense(cloverPlugin) && getGlobalLicense(project) != null) {
      String license = getGlobalLicense(project);
      File licenseFile = writeLicenseToDisk(project, license);
      cloverPlugin.setParameter("licenseLocation", licenseFile.getAbsolutePath());
    }
  }

  private boolean hasLicense(MavenPlugin cloverPlugin) {
    return StringUtils.isNotBlank(cloverPlugin.getParameter("license"))
        || StringUtils.isNotBlank(cloverPlugin.getParameter("licenseLocation"));
  }

  private File writeLicenseToDisk(Project project, String license) {
    try {
      File file = new File(project.getFileSystem().getSonarWorkingDirectory(), "clover.license");
      FileUtils.writeStringToFile(file, license);
      return file;
    } catch (IOException e) {
      throw new RuntimeException("can not write the clover license", e);
    }
  }

  private String getGlobalLicense(Project project) {
    Object value = project.getProperty(CloverConstants.LICENSE_PROPERTY);
    if (value != null) {
      if (value instanceof String) {
        return (String) value;

      } else if (value instanceof Collection) {
        return StringUtils.join(((Collection) value), "");
      }
    }
    return null;
  }

}
