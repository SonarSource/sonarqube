/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.permission.PermissionTemplateService;

import static org.sonar.server.component.NewComponent.newComponentBuilder;

/**
 * Used in GOV
 */
public class DefaultRubyComponentService {

  private final DbClient dbClient;
  private final ComponentService componentService;
  private final PermissionTemplateService permissionTemplateService;
  private final FavoriteUpdater favoriteUpdater;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public DefaultRubyComponentService(DbClient dbClient, ComponentService componentService,
    PermissionTemplateService permissionTemplateService, FavoriteUpdater favoriteUpdater,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.componentService = componentService;
    this.permissionTemplateService = permissionTemplateService;
    this.favoriteUpdater = favoriteUpdater;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  // Used in GOV
  @CheckForNull
  public Long createComponent(String key, String name, String qualifier) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return createComponent(dbSession, key, null, name, qualifier);
    }
  }

  public long createComponent(DbSession dbSession, String key, @Nullable String branch, String name, @Nullable String qualifier) {
    ComponentDto provisionedComponent = componentService.create(
      dbSession,
      newComponentBuilder()
        .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
        .setKey(key)
        .setName(name)
        .setQualifier(qualifier)
        .setBranch(branch)
        .build());
    String organizationUuid = defaultOrganizationProvider.get().getUuid();
    permissionTemplateService.applyDefaultPermissionTemplate(dbSession, organizationUuid, provisionedComponent.getKey());
    if (Qualifiers.PROJECT.equals(provisionedComponent.qualifier())
      && permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(dbSession, organizationUuid, provisionedComponent)) {
      // TODO Set userId as null as this method will be removed in a next commit
      favoriteUpdater.add(dbSession, provisionedComponent, null);
      dbSession.commit();
    }

    return provisionedComponent.getId();
  }

}
