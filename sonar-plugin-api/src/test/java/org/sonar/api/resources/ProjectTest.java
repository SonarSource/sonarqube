/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.resources;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.test.MavenTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ProjectTest {
  @Test
  public void equalsProject() {
    Project project1 = MavenTestUtils.loadProjectFromPom(getClass(), "equalsProject/pom.xml");
    Project project2 = MavenTestUtils.loadProjectFromPom(getClass(), "equalsProject/pom.xml");
    assertEquals(project1, project2);
    assertFalse("foo:bar".equals(project1));
    assertEquals(project1.hashCode(), project2.hashCode());
  }

  @Test
  public void effectiveKeyShouldEqualKey() {
    assertThat(new Project("my:project").getEffectiveKey(), is("my:project"));
  }

  @Test
  public void createFromMavenIds() {
    Project project = Project.createFromMavenIds("my", "artifact");
    assertThat(project.getKey(), is("my:artifact"));
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2261
   * Note that several exclusions separated by comma would be correctly trimmed by commons-configuration library.
   * So issue is only with a single pattern, which contains spaces.
   */
  @Test
  public void shouldTrimExclusionPatterns() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "  **/*Foo.java   , **/Bar.java    ");
    Project project = new Project("foo").setConfiguration(conf);

    String[] exclusions = project.getExclusionPatterns();

    assertThat(exclusions.length, Is.is(2));
    assertThat(exclusions[0], Is.is("**/*Foo.java"));
    assertThat(exclusions[1], Is.is("**/Bar.java"));
  }

  @Test
  public void testNoExclusionPatterns() {
    Project project = new Project("key").setConfiguration(new PropertiesConfiguration());

    MatcherAssert.assertThat(project.getExclusionPatterns().length, Is.is(0));
  }

  @Test
  public void testManyExclusionPatterns() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*,foo,*/bar");

    Project project = new Project("key").setConfiguration(conf);

    MatcherAssert.assertThat(project.getExclusionPatterns().length, Is.is(3));
    MatcherAssert.assertThat(project.getExclusionPatterns()[0], Is.is("**/*"));
    MatcherAssert.assertThat(project.getExclusionPatterns()[1], Is.is("foo"));
    MatcherAssert.assertThat(project.getExclusionPatterns()[2], Is.is("*/bar"));
  }

  @Test
  public void testSetExclusionPatterns() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    Project project = new Project("key").setConfiguration(conf);

    project.setExclusionPatterns(new String[]{"**/*Foo.java", "**/*Bar.java"});
    MatcherAssert.assertThat(project.getExclusionPatterns().length, Is.is(2));
    MatcherAssert.assertThat(project.getExclusionPatterns()[0], Is.is("**/*Foo.java"));
    MatcherAssert.assertThat(project.getExclusionPatterns()[1], Is.is("**/*Bar.java"));
  }
}
