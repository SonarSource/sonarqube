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

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.ResourcePermissioning;

public class ApplyProjectRolesDecorator implements Decorator {

  private final ResourcePermissioning resourcePermissioning;

  public ApplyProjectRolesDecorator(ResourcePermissioning resourcePermissioning) {
    this.resourcePermissioning = resourcePermissioning;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      resourcePermissioning.grantDefaultRoles(resource);
    }
  }

  private boolean shouldDecorateResource(Resource resource) {
    return resource.getId() != null && isProject(resource) && !resourcePermissioning.hasRoles(resource);
  }

  private boolean isProject(Resource resource) {
    return Qualifiers.PROJECT.equals(resource.getQualifier()) || Qualifiers.VIEW.equals(resource.getQualifier());
  }
}
