/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.db.organization.DefaultTemplates;

import static java.util.Optional.ofNullable;

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
      isViewsEnabled(resourceTypes) ? ofNullable(defaultTemplates.getApplicationsUuid()).orElse(projectDefaultTemplate) : null,
      isViewsEnabled(resourceTypes) ? ofNullable(defaultTemplates.getPortfoliosUuid()).orElse(projectDefaultTemplate) : null);
  }

  private static boolean isViewsEnabled(ResourceTypes resourceTypes) {
    return resourceTypes.getRoots()
      .stream()
      .map(ResourceType::getQualifier)
      .anyMatch(Qualifiers.VIEW::equals);
  }

}
