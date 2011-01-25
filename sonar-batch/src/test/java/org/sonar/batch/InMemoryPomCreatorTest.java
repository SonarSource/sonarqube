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
package org.sonar.batch;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.ProjectDefinition;

import java.io.File;
import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InMemoryPomCreatorTest {

  private Properties properties;
  private ProjectDefinition project;

  @Before
  public void setUp() {
    properties = new Properties();
    File baseDir = new File(".");
    project = new ProjectDefinition(baseDir, properties);
  }

  @Test
  public void shouldCreate() throws Exception {
    createRequiredProperties("org.example:example", "1.0-SNAPSHOT", "target");
    properties.setProperty("sonar.projectBinaries", "junit.jar");

    project.addSourceDir("src");
    project.addTestDir("test");

    MavenProject pom = create();

    assertThat(pom.getBasedir(), is(project.getBaseDir()));
    assertThat(pom.getGroupId(), is("org.example"));
    assertThat(pom.getArtifactId(), is("example"));
    assertThat(pom.getProperties(), is(project.getProperties()));
    assertThat(pom.getBuild().getDirectory(), is("target"));
    assertThat(pom.getBuild().getOutputDirectory(), is("target/classes"));
    assertThat(pom.getReporting().getOutputDirectory(), is("target/site"));

    assertThat(pom.getCompileSourceRoots().size(), is(1));
    assertThat((String) pom.getCompileSourceRoots().get(0), is("src"));

    assertThat(pom.getTestCompileSourceRoots().size(), is(1));
    assertThat((String) pom.getTestCompileSourceRoots().get(0), is("test"));

    assertThat(pom.getCompileClasspathElements().size(), is(1));
    assertThat((String) pom.getCompileClasspathElements().get(0), is("junit.jar"));
  }

  @Test
  public void nonStandardDirectoriesLayout() {
    createRequiredProperties("org.example:example", "1.0-SNAPSHOT", "build");
    properties.setProperty("project.build.outputDirectory", "bin");
    properties.setProperty("project.reporting.outputDirectory", "build/reports");

    MavenProject pom = create();

    assertThat(pom.getBuild().getDirectory(), is("build"));
    assertThat(pom.getBuild().getOutputDirectory(), is("bin"));
    assertThat(pom.getReporting().getOutputDirectory(), is("build/reports"));
  }

  @Test
  public void shouldNotFailIfNoBinaries() throws Exception {
    createRequiredProperties("org.example:example", "1.0-SNAPSHOT", "build");

    MavenProject pom = create();
    assertThat(pom.getCompileClasspathElements().size(), is(0));
  }

  private void createRequiredProperties(String key, String version, String buildDirectory) {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, key);
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, version);
    properties.setProperty("project.build.directory", buildDirectory);
  }

  @Test(expected = SonarException.class)
  public void shouldFailWhenKeyNotSpecified() {
    create();
  }

  @Test(expected = SonarException.class)
  public void shouldFailWhenVersionNotSpecified() {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "org.example:example");
    create();
  }

  @Test(expected = SonarException.class)
  public void shouldFailWhenBuildDirectoryNotSpecified() {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "org.example:example");
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "0.1");
    create();
  }

  private MavenProject create() {
    return new InMemoryPomCreator(project).create();
  }
}
