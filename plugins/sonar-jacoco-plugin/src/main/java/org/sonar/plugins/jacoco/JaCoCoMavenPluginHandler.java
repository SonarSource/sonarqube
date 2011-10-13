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
package org.sonar.plugins.jacoco;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.batch.maven.MavenSurefireUtils;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

/**
 * @author Evgeny Mandrikov
 */
public class JaCoCoMavenPluginHandler implements MavenPluginHandler {

  private static final String ARG_LINE_PARAMETER = "argLine";
  private static final String TEST_FAILURE_IGNORE_PARAMETER = "testFailureIgnore";

  private final String groupId;
  private final String artifactId;
  private final String version;

  private JacocoConfiguration configuration;

  public JaCoCoMavenPluginHandler(JacocoConfiguration configuration) {
    this.configuration = configuration;
    groupId = MavenSurefireUtils.GROUP_ID;
    artifactId = MavenSurefireUtils.ARTIFACT_ID;
    version = MavenSurefireUtils.VERSION;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public boolean isFixedVersion() {
    return false;
  }

  public String[] getGoals() {
    return new String[] { "test" };
  }

  public void configure(Project project, MavenPlugin plugin) {
    // See SONARPLUGINS-600
    String destfilePath = configuration.getReportPath();
    File destfile = project.getFileSystem().resolvePath(destfilePath);
    if (destfile.exists() && destfile.isFile()) {
      JaCoCoUtils.LOG.info("Deleting {}", destfile);
      if (!destfile.delete()) {
        throw new SonarException("Unable to delete " + destfile);
      }
    }

    String argument = configuration.getJvmArgument();

    String argLine = plugin.getParameter(ARG_LINE_PARAMETER);
    argLine = StringUtils.isBlank(argLine) ? argument : argument + " " + argLine;
    JaCoCoUtils.LOG.info("JVM options: {}", argLine);
    plugin.setParameter(ARG_LINE_PARAMETER, argLine);

    plugin.setParameter(TEST_FAILURE_IGNORE_PARAMETER, "true");
  }

}
