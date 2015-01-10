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
package org.sonar.plugins.core.security;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.security.ResourcePermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ApplyProjectRolesDecoratorTest {

  private ResourcePermissions resourcePermissions;
  private ApplyProjectRolesDecorator decorator;

  @Before
  public void init() {
    resourcePermissions = mock(ResourcePermissions.class);
    decorator = new ApplyProjectRolesDecorator(resourcePermissions);
  }

  @Test
  public void alwaysExecute() {
    assertThat(decorator.shouldExecuteOnProject(new Project("project"))).isTrue();
  }

  @Test
  public void doNotGrantDefaultRolesWhenExistingPermissions() {
    Project project = new Project("project");
    project.setId(10);
    when(resourcePermissions.hasRoles(project)).thenReturn(true);

    decorator.decorate(project, null);

    verify(resourcePermissions, never()).grantDefaultRoles(project);
  }

  @Test
  public void doNotApplySecurityOnModules() {
    Project project = new Project("project");
    Project module = new Project("module").setParent(project);
    module.setId(10);
    when(resourcePermissions.hasRoles(project)).thenReturn(false);

    decorator.decorate(module, null);

    verify(resourcePermissions, never()).grantDefaultRoles(module);
  }

  @Test
  public void grantDefaultRolesWhenNoPermissions() {
    Project project = new Project("project");
    project.setId(10);
    when(resourcePermissions.hasRoles(project)).thenReturn(false);

    decorator.decorate(project, null);

    verify(resourcePermissions).grantDefaultRoles(project);
  }

}
