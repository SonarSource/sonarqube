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
import org.sonar.api.security.GroupRole;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Mockito.*;

public class ApplyProjectRolesDecoratorTest {

  private RoleManager roleManager;
  private ApplyProjectRolesDecorator decorator;

  @Before
  public void before() {
    roleManager = mock(RoleManager.class);
    decorator = new ApplyProjectRolesDecorator(roleManager);
  }

  @Test
  public void doNotApplySecurityWhenExistingRoles() {
    Project project = new Project("project");
    project.setId(10);
    when(roleManager.getGroupRoles(10)).thenReturn(Arrays.<GroupRole>asList(new GroupRole()));

    decorator.decorate(project, null);

    verify(roleManager, never()).affectDefaultRolesToResource(anyInt());
  }

  @Test
  public void doNotApplySecurityOnModules() {
    Project project = new Project("project");
    Project module = new Project("module").setParent(project);
    module.setId(10);

    when(roleManager.getGroupRoles(10)).thenReturn(Arrays.<GroupRole>asList());

    decorator.decorate(module, null);

    verify(roleManager, never()).affectDefaultRolesToResource(anyInt());
  }

  @Test
  public void applySecurityWhenNoRoles() {
    Project project = new Project("project");
    project.setId(10);
    when(roleManager.getGroupRoles(10)).thenReturn(new ArrayList<GroupRole>());

    decorator.decorate(project, null);

    verify(roleManager).affectDefaultRolesToResource(10);
  }

}
