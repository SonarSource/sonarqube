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
package org.sonar.batch;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.batch.bootstrapper.ProjectDefinition;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

public class MavenProjectConverterTest {
  @Test
  public void test2() {
    MavenProject root = new MavenProject();
    root.setFile(new File("/foo/pom.xml"));
    root.getModules().add("module");
    MavenProject module = new MavenProject();
    module.setFile(new File("/foo/module/pom.xml"));
    ProjectDefinition project = MavenProjectConverter.convert(Arrays.asList(root, module));

    assertThat(project.getModules().size(), is(1));
  }

  @Test
  public void test() {
    MavenProject pom = new MavenProject();
    pom.setGroupId("foo");
    pom.setArtifactId("bar");
    pom.setVersion("1.0.1");
    pom.setName("Test");
    pom.setDescription("just test");
    ProjectDefinition project = MavenProjectConverter.convert(pom);

    Properties properties = project.getProperties();
    assertThat(properties.getProperty(CoreProperties.PROJECT_KEY_PROPERTY), is("foo:bar"));
    assertThat(properties.getProperty(CoreProperties.PROJECT_VERSION_PROPERTY), is("1.0.1"));
    assertThat(properties.getProperty(CoreProperties.PROJECT_NAME_PROPERTY), is("Test"));
    assertThat(properties.getProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY), is("just test"));
  }
}
