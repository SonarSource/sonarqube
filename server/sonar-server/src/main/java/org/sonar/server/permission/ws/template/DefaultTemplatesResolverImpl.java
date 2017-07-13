/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.permission.ws.template;

import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.db.organization.DefaultTemplates;

import static java.util.Optional.ofNullable;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.VIEW;

public class DefaultTemplatesResolverImpl implements DefaultTemplatesResolver {
  private final ResourceTypes resourceTypes;

  public DefaultTemplatesResolverImpl(ResourceTypes resourceTypes) {
    this.resourceTypes = resourceTypes;
  }

  @Override
  public ResolvedDefaultTemplates resolve(DefaultTemplates defaultTemplates) {
    String projectDefaultTemplate = defaultTemplates.getProjectUuid();

    return new ResolvedDefaultTemplates(
      projectDefaultTemplate,
      isViewsAvailable(resourceTypes) ? ofNullable(defaultTemplates.getViewUuid()).orElse(projectDefaultTemplate) : null,
      isApplicationsAvailable(resourceTypes) ? ofNullable(defaultTemplates.getApplicationUuid()).orElse(projectDefaultTemplate) : null);
  }

  private static boolean isViewsAvailable(ResourceTypes resourceTypes) {
    return resourceTypes.getRoots()
      .stream()
      .map(ResourceType::getQualifier)
      .anyMatch(VIEW::equals);
  }

  private static boolean isApplicationsAvailable(ResourceTypes resourceTypes) {
    return resourceTypes.getRoots()
      .stream()
      .map(ResourceType::getQualifier)
      .anyMatch(APP::equals);
  }

}
