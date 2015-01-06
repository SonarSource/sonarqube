/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.maven;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal sonar
 * @aggregator
 */
public final class SonarMojo extends AbstractMojo {

  /**
   * @component
   * @required
   * @readonly
   * @VisibleForTesting
   */
  RuntimeInformation runtimeInformation;

  @Override
  public void execute() throws MojoExecutionException {
    ArtifactVersion mavenVersion = getMavenVersion();
    if (mavenVersion.getMajorVersion() < 3) {
      throw new MojoExecutionException("Please use at least Maven 3.x to perform SonarQube analysis");
    }
    throw new MojoExecutionException("Please update sonar-maven-plugin to at least version 2.3");
  }

  private ArtifactVersion getMavenVersion() {
    return runtimeInformation.getApplicationVersion();
  }

}
