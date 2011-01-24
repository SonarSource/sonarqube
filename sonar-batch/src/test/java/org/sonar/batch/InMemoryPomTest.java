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

public class InMemoryPomTest {

  private Properties properties;
  private ProjectDefinition project;

  @Before
  public void setUp() {
    properties = new Properties();
    File baseDir = new File(".");
    project = new ProjectDefinition(baseDir, properties);
  }

  @Test
  public void shouldCreate() {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "org.example:example");
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0-SNAPSHOT");
    properties.setProperty("project.build.directory", "build");

    project.addSourceDir("src");
    project.addTestDir("test");

    MavenProject pom = ProjectTree.createInMemoryPom(project);

    assertThat(pom.getBasedir(), is(project.getBaseDir()));
    assertThat(pom.getGroupId(), is("org.example"));
    assertThat(pom.getArtifactId(), is("example"));
    assertThat(pom.getProperties(), is(project.getProperties()));
    assertThat(pom.getBuild().getDirectory(), is("build"));

    assertThat(pom.getCompileSourceRoots().size(), is(1));
    assertThat(pom.getTestCompileSourceRoots().size(), is(1));
  }

  @Test(expected = SonarException.class)
  public void shouldFailWhenKeyNotSpecified() {
    ProjectTree.createInMemoryPom(project);
  }

  @Test(expected = SonarException.class)
  public void shouldFailWhenVersionNotSpecified() {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "org.example:example");
    ProjectTree.createInMemoryPom(project);
  }

  @Test(expected = SonarException.class)
  public void shouldFailWhenBuildDirectoryNotSpecified() {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "org.example:example");
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "0.1");
    ProjectTree.createInMemoryPom(project);
  }

}
