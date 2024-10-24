/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.common.permission;

import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentType;
import org.sonar.server.component.ComponentTypes;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.property.InternalProperties;

public class DefaultTemplatesResolverImpl implements DefaultTemplatesResolver {

  private final DbClient dbClient;
  private final ComponentTypes componentTypes;

  public DefaultTemplatesResolverImpl(DbClient dbClient, ComponentTypes componentTypes) {
    this.dbClient = dbClient;
    this.componentTypes = componentTypes;
  }

  @Override
  public ResolvedDefaultTemplates resolve(DbSession dbSession) {
    String defaultProjectTemplate = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.DEFAULT_PROJECT_TEMPLATE).orElseThrow(() -> {
      throw new IllegalStateException("Default template for project is missing");
    });

    String defaultPortfolioTemplate = null;
    String defaultApplicationTemplate = null;

    if (isPortfolioEnabled(componentTypes)) {
      defaultPortfolioTemplate = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.DEFAULT_PORTFOLIO_TEMPLATE).orElse(defaultProjectTemplate);
    }
    if (isApplicationEnabled(componentTypes)) {
      defaultApplicationTemplate = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.DEFAULT_APPLICATION_TEMPLATE).orElse(defaultProjectTemplate);
    }
    return new ResolvedDefaultTemplates(defaultProjectTemplate, defaultApplicationTemplate, defaultPortfolioTemplate);
  }

  private static boolean isPortfolioEnabled(ComponentTypes componentTypes) {
    return componentTypes.getRoots()
      .stream()
      .map(ComponentType::getQualifier)
      .anyMatch(ComponentQualifiers.VIEW::equals);
  }

  private static boolean isApplicationEnabled(ComponentTypes componentTypes) {
    return componentTypes.getRoots()
            .stream()
            .map(ComponentType::getQualifier)
            .anyMatch(ComponentQualifiers.APP::equals);
  }

}
