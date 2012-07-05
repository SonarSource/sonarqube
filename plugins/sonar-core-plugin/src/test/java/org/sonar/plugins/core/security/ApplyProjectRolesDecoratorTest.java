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
package org.sonar.plugins.core.security;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.security.ResourcePermissioning;

import static org.mockito.Mockito.*;

public class ApplyProjectRolesDecoratorTest {

  private ResourcePermissioning resourcePermissioning;
  private ApplyProjectRolesDecorator decorator;

  @Before
  public void before() {
    resourcePermissioning = mock(ResourcePermissioning.class);
    decorator = new ApplyProjectRolesDecorator(resourcePermissioning);
  }

  @Test
  public void doNotApplySecurityWhenExistingPermissions() {
    Project project = new Project("project");
    project.setId(10);
    when(resourcePermissioning.hasPermissions(project)).thenReturn(true);

    decorator.decorate(project, null);

    verify(resourcePermissioning, never()).grantDefaultPermissions(project);
  }

  @Test
  public void doNotApplySecurityOnModules() {
    Project project = new Project("project");
    Project module = new Project("module").setParent(project);
    module.setId(10);
    when(resourcePermissioning.hasPermissions(project)).thenReturn(false);

    decorator.decorate(module, null);

    verify(resourcePermissioning, never()).grantDefaultPermissions(module);
  }

  @Test
  public void applySecurityWhenNoPermissions() {
    Project project = new Project("project");
    project.setId(10);
    when(resourcePermissioning.hasPermissions(project)).thenReturn(false);

    decorator.decorate(project, null);

    verify(resourcePermissioning).grantDefaultPermissions(project);
  }

}
