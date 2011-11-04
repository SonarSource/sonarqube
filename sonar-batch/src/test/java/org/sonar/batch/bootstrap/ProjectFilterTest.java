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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.config.ProjectSettings;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ProjectFilterTest {

  @Test
  public void testSkippedModule() {
    Settings settings = new Settings();
    settings.setProperty("sonar.skippedModules", "foo,bar");

    ProjectFilter filter = new ProjectFilter(settings);
    assertTrue(filter.isExcluded(new Project("my:foo")));
  }

  @Test
  public void testNotExcluded() {
    Settings settings = new Settings();
    settings.setProperty("sonar.skippedModules", "foo,bar");

    ProjectFilter filter = new ProjectFilter(settings);
    assertFalse(filter.isExcluded(new Project("my:other")));
  }

  @Test
  public void testNoSkippedModules() {
    Settings settings = new Settings();

    ProjectFilter filter = new ProjectFilter(settings);
    assertFalse(filter.isExcluded(new Project("my:other")));
  }

  @Test
  public void testIncludedModules() {
    Settings settings = new Settings();
    settings.setProperty("sonar.includedModules", "foo");

    ProjectFilter filter = new ProjectFilter(settings);
    assertFalse(filter.isExcluded(new Project("my:foo")));

    filter = new ProjectFilter(settings);
    assertTrue(filter.isExcluded(new Project("my:bar")));
  }

  @Test
  public void shouldBeExcludedIfParentIsExcluded() {
    Settings settings = new Settings();
    settings.setProperty("sonar.skippedModules", "parent");

    Project parent = new Project("my:parent");
    Project child = new Project("my:child");
    child.setParent(parent);

    ProjectFilter filter = new ProjectFilter(settings);
    assertTrue(filter.isExcluded(child));
  }

  @Test
  public void testGetArtifactId() {
    assertThat(ProjectFilter.getArtifactId(new Project("org:foo")), is("foo"));
    assertThat(ProjectFilter.getArtifactId(new Project("foo")), is("foo"));
    assertThat(ProjectFilter.getArtifactId(new Project("org:foo:1.x").setBranch("1.x")), is("foo"));
  }
}
