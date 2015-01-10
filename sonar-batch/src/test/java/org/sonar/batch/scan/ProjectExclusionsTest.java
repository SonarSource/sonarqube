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
package org.sonar.batch.scan;

import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectExclusionsTest {

  ProjectReactor newReactor(String rootKey, String... moduleKeys) {
    ProjectDefinition root = ProjectDefinition.create().setKey(rootKey);
    for (String moduleKey : moduleKeys) {
      ProjectDefinition module = ProjectDefinition.create().setKey(moduleKey);
      root.addSubProject(module);
    }
    return new ProjectReactor(root);
  }

  @Test
  public void testSkippedModules() {
    Settings settings = new Settings();
    settings.setProperty("sonar.skippedModules", "sub1,sub3");

    ProjectReactor reactor = newReactor("root", "sub1", "sub2");

    ProjectExclusions exclusions = new ProjectExclusions(settings, reactor, null);
    exclusions.apply();

    assertThat(reactor.getProject("root")).isNotNull();
    assertThat(reactor.getProject("sub1")).isNull();
    assertThat(reactor.getProject("sub2")).isNotNull();
  }

  @Test
  public void testNoSkippedModules() {
    Settings settings = new Settings();
    ProjectReactor reactor = newReactor("root", "sub1", "sub2");
    ProjectExclusions exclusions = new ProjectExclusions(settings, reactor, null);
    exclusions.apply();

    assertThat(reactor.getProject("root")).isNotNull();
    assertThat(reactor.getProject("sub1")).isNotNull();
    assertThat(reactor.getProject("sub2")).isNotNull();
  }

  @Test
  public void testIncludedModules() {
    Settings settings = new Settings();
    settings.setProperty("sonar.includedModules", "sub1");
    ProjectReactor reactor = newReactor("root", "sub1", "sub2");
    ProjectExclusions exclusions = new ProjectExclusions(settings, reactor, null);
    exclusions.apply();

    assertThat(reactor.getProject("root")).isNotNull();
    assertThat(reactor.getProject("sub1")).isNotNull();
    assertThat(reactor.getProject("sub2")).isNull();
  }

  @Test
  public void shouldBeExcludedIfParentIsExcluded() {
    ProjectDefinition sub11 = ProjectDefinition.create().setKey("sub11");
    ProjectDefinition sub1 = ProjectDefinition.create().setKey("sub1").addSubProject(sub11);
    ProjectDefinition root = ProjectDefinition.create().setKey("root").addSubProject(sub1);

    Settings settings = new Settings();
    settings.setProperty("sonar.skippedModules", "sub1");

    ProjectReactor reactor = new ProjectReactor(root);
    ProjectExclusions exclusions = new ProjectExclusions(settings, reactor, null);
    exclusions.apply();

    assertThat(reactor.getProject("root")).isNotNull();
    assertThat(reactor.getProject("sub1")).isNull();
    assertThat(reactor.getProject("sub11")).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfExcludingRoot() {
    Settings settings = new Settings();
    settings.setProperty("sonar.skippedModules", "sub1,root");

    ProjectReactor reactor = newReactor("root", "sub1", "sub2");
    ProjectExclusions exclusions = new ProjectExclusions(settings, reactor, null);
    exclusions.apply();
  }

  @Test
  public void shouldIgnoreMavenGroupId() {
    ProjectReactor reactor = newReactor("org.apache.struts:struts", "org.apache.struts:struts-core", "org.apache.struts:struts-taglib");

    Settings settings = new Settings();
    settings.setProperty("sonar.skippedModules", "struts-taglib");

    ProjectExclusions exclusions = new ProjectExclusions(settings, reactor, null);
    exclusions.apply();

    assertThat(reactor.getProject("org.apache.struts:struts")).isNotNull();
    assertThat(reactor.getProject("org.apache.struts:struts-core")).isNotNull();
    assertThat(reactor.getProject("org.apache.struts:struts-taglib")).isNull();
  }
}
