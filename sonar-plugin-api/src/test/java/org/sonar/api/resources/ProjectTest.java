/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.resources;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.test.MavenTestUtils;

import static org.fest.assertions.Assertions.assertThat;

public class ProjectTest {
  PropertiesConfiguration conf = new PropertiesConfiguration();

  @Test
  public void equalsProject() {
    Project project1 = MavenTestUtils.loadProjectFromPom(getClass(), "equalsProject/pom.xml");
    Project project2 = MavenTestUtils.loadProjectFromPom(getClass(), "equalsProject/pom.xml");

    assertThat(project1).isEqualTo(project2);
    assertThat(project1).isNotEqualTo("foo:bar");
    assertThat(project1.hashCode()).isEqualTo(project2.hashCode());
  }

  @Test
  public void effectiveKeyShouldEqualKey() {
    assertThat(new Project("my:project").getEffectiveKey()).isEqualTo("my:project");
  }

  @Test
  public void createFromMavenIds() {
    Project project = Project.createFromMavenIds("my", "artifact");

    assertThat(project.getKey()).isEqualTo("my:artifact");
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2261
   * Note that several exclusions separated by comma would be correctly trimmed by commons-configuration library.
   * So issue is only with a single pattern, which contains spaces.
   */
  @Test
  public void shouldTrimExclusionPatterns() {
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "  **/*Foo.java   , **/Bar.java    ");
    conf.setProperty(CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY, "  **/*Test.java   ");

    Project project = new Project("foo").setConfiguration(conf);
    String[] exclusions = project.getExclusionPatterns();

    assertThat(exclusions).containsOnly("**/*Foo.java", "**/Bar.java", "**/*Test.java");
  }

  @Test
  public void testNoExclusionPatterns() {
    Project project = new Project("key").setConfiguration(conf);

    assertThat(project.getExclusionPatterns()).isEmpty();
    assertThat(project.getTestExclusionPatterns()).isEmpty();
  }

  @Test
  public void should_exclude_many_patterns() {
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*,foo,*/bar");
    conf.setProperty(CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY, "*/exclude");

    Project project = new Project("key").setConfiguration(conf);

    assertThat(project.getExclusionPatterns()).containsOnly("**/*", "foo", "*/bar", "*/exclude");
  }

  @Test
  public void should_exclude_test_patterns() {
    conf.setProperty(CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY, "**/*Test.java, **/*IntegrationTest.java");
    conf.setProperty(CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY, "**/*FunctionalTest.java");

    Project project = new Project("key").setConfiguration(conf);

    assertThat(project.getTestExclusionPatterns()).containsOnly("**/*Test.java", "**/*IntegrationTest.java", "**/*FunctionalTest.java");
  }
}
