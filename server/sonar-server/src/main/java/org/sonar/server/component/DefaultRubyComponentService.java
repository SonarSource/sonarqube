/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.component;

import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static org.sonar.server.component.NewComponent.newComponentBuilder;

/**
 * Used in GOV
 */
@ServerSide
public class DefaultRubyComponentService {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentUpdater componentUpdater;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public DefaultRubyComponentService(UserSession userSession, DbClient dbClient, ComponentUpdater componentUpdater,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentUpdater = componentUpdater;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  // Used in GOV
  /**
   * @deprecated Use {@link ComponentUpdater#create(DbSession, NewComponent, Integer)} instead
   */
  @Deprecated
  public Long createComponent(String key, String name, String qualifier) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      NewComponent newComponent = newComponentBuilder()
        .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
        .setKey(key)
        .setName(name)
        .setQualifier(qualifier)
        .build();
      return componentUpdater.create(
        dbSession,
        newComponent,
        userSession.isLoggedIn() ? userSession.getUserId() : null).getId();
    }
  }

}
