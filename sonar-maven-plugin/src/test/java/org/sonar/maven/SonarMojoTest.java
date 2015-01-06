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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SonarMojoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testFailureWithMaven2() throws MojoExecutionException {
    SonarMojo mojo = new SonarMojo();
    mojo.runtimeInformation = new RuntimeInformation() {

      @Override
      public ArtifactVersion getApplicationVersion() {
        return new DefaultArtifactVersion("2.2.1");
      }
    };
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("Please use at least Maven 3.x to perform SonarQube analysis");
    mojo.execute();
  }

  @Test
  public void testFailureWithMaven3() throws MojoExecutionException {
    SonarMojo mojo = new SonarMojo();
    mojo.runtimeInformation = new RuntimeInformation() {

      @Override
      public ArtifactVersion getApplicationVersion() {
        return new DefaultArtifactVersion("3.0.5");
      }
    };
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("Please update sonar-maven-plugin to at least version 2.3");
    mojo.execute();
  }

}
