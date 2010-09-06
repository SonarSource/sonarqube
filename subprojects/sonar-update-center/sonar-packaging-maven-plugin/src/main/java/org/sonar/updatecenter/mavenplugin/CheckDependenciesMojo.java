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
package org.sonar.updatecenter.mavenplugin;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 * @goal check-dependencies
 * @requiresDependencyResolution runtime
 * @phase initialize
 */
public class CheckDependenciesMojo extends AbstractSonarPluginMojo {

  private static final String[] GWT_ARTIFACT_IDS = {"gwt-user", "gwt-dev", "sonar-gwt-api"};
  private static final String[] LOG_GROUP_IDS = {"log4j", "commons-logging"};

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!isSkipDependenciesPackaging()) {
      checkApiDependency();
      checkLogDependencies();
      checkGwtDependencies();
    }
  }

  private void checkApiDependency() throws MojoExecutionException {
    Artifact sonarApi = getSonarPluginApiArtifact();

    if (sonarApi == null) {
      throw new MojoExecutionException(
          SONAR_GROUPID + ":" + SONAR_PLUGIN_API_ARTIFACTID + " should be declared in dependencies"
      );
    }
  }

  private void checkLogDependencies() throws MojoExecutionException {
    List<String> ids = new ArrayList<String>();
    for (Artifact dep : getIncludedArtifacts()) {
      if (ArrayUtils.contains(LOG_GROUP_IDS, dep.getGroupId())) {
        ids.add(dep.getDependencyConflictId());
      }
    }
    if (!ids.isEmpty()) {
      StringBuilder message = new StringBuilder();
      message.append("Dependencies on the following log libraries should be excluded or declared with scope 'provided':")
          .append("\n\t")
          .append(StringUtils.join(ids, ", "))
          .append('\n');
      getLog().warn(message.toString());
    }
  }

  private void checkGwtDependencies() {
    List<String> ids = new ArrayList<String>();
    for (Artifact dep : getDependencyArtifacts(Artifact.SCOPE_COMPILE)) {
      if (ArrayUtils.contains(GWT_ARTIFACT_IDS, dep.getArtifactId())) {
        ids.add(dep.getDependencyConflictId());
      }
    }
    if (!ids.isEmpty()) {
      getLog().warn(getMessage("GWT dependencies should be defined with scope 'provided':", ids));
    }
  }
}
