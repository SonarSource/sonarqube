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
    File workDir = new File(baseDir, "sonar");
    project = new ProjectDefinition(baseDir, workDir, properties);
  }

  @Test
  public void minimal() {
    createRequiredProperties();

    MavenProject pom = create();

    assertThat(pom.getBasedir(), is(project.getBaseDir()));
    assertThat(pom.getGroupId(), is("org.example"));
    assertThat(pom.getArtifactId(), is("example"));
    assertThat(pom.getName(), is("Unnamed - org.example:example"));
    assertThat(pom.getDescription(), is(""));
    assertThat(pom.getProperties(), is(project.getProperties()));
    assertThat(pom.getBasedir(), is(project.getBaseDir()));
    String buildDirectory = project.getWorkDir().getAbsolutePath() + "/target";
    assertThat(pom.getBuild().getDirectory(), is(buildDirectory));
    assertThat(pom.getBuild().getOutputDirectory(), is(buildDirectory + "/classes"));
    assertThat(pom.getReporting().getOutputDirectory(), is(buildDirectory + "/site"));
  }

  @Test
  public void nameAndDescription() {
    createRequiredProperties();

    properties.setProperty(CoreProperties.PROJECT_NAME_PROPERTY, "Foo");
    properties.setProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY, "Bar");

    MavenProject pom = create();

    assertThat(pom.getName(), is("Foo"));
    assertThat(pom.getDescription(), is("Bar"));
  }

  @Test
  public void sourceDirectories() {
    createRequiredProperties();
    properties.setProperty("sonar.projectBinaries", "junit.jar");
    project.addSourceDir("src");
    project.addTestDir("test");

    MavenProject pom = create();

    assertThat(pom.getCompileSourceRoots().size(), is(1));
    assertThat((String) pom.getCompileSourceRoots().get(0), is("src"));

    assertThat(pom.getTestCompileSourceRoots().size(), is(1));
    assertThat((String) pom.getTestCompileSourceRoots().get(0), is("test"));
  }

  @Test
  public void classpath() throws Exception {
    createRequiredProperties();
    properties.setProperty("sonar.projectBinaries", "junit.jar");

    MavenProject pom = create();

    assertThat(pom.getCompileClasspathElements().size(), is(1));
    assertThat((String) pom.getCompileClasspathElements().get(0), is("junit.jar"));
  }

  @Test
  public void shouldNotFailIfNoBinaries() throws Exception {
    createRequiredProperties();

    MavenProject pom = create();
    assertThat(pom.getCompileClasspathElements().size(), is(0));
  }

  private void createRequiredProperties() {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "org.example:example");
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0-SNAPSHOT");
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

  private MavenProject create() {
    return new InMemoryPomCreator(project).create();
  }
}
