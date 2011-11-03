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
package org.sonar.plugins.core.security;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public class ApplyProjectRolesDecorator implements Decorator {

  private RoleManager roleManager;

  ApplyProjectRolesDecorator(RoleManager roleManager) {
    this.roleManager = roleManager;
  }

  public ApplyProjectRolesDecorator(DatabaseSession session) {
    this.roleManager = new RoleManager(session);
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      Project project = (Project) resource;
      roleManager.affectDefaultRolesToResource(project.getId());
    }
  }

  private boolean shouldDecorateResource(Resource resource) {
    if (isProject(resource)) {
      Project project = (Project) resource;
      return project.getId() != null && countRoles(project.getId()) == 0;
    }
    return false;
  }

  private boolean isProject(Resource resource) {
    if (Resource.QUALIFIER_PROJECT.equals(resource.getQualifier()) ||
        Resource.QUALIFIER_VIEW.equals(resource.getQualifier()) ||
        Resource.QUALIFIER_SUBVIEW.equals(resource.getQualifier())) {
      return resource instanceof Project;
    }
    return false;
  }

  private int countRoles(int resourceId) {
    return roleManager.getUserRoles(resourceId).size() + roleManager.getGroupRoles(resourceId).size();
  }
}
