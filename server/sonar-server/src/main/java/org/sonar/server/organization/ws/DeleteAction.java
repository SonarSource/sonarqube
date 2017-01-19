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
package org.sonar.server.organization.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

public class DeleteAction implements OrganizationsAction {
  private static final String ACTION = "delete";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final ComponentCleanerService componentCleanerService;

  public DeleteAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider,
    ComponentCleanerService componentCleanerService) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.componentCleanerService = componentCleanerService;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Delete an organization.<br/>" +
        "Require 'Administer System' permission on the specified organization.")
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Organization key")
      .setExampleValue("foo-company");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String key = request.mandatoryParam(PARAM_KEY);
    preventDeletionOfDefaultOrganization(key, defaultOrganizationProvider.get());

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organizationDto = checkFoundWithOptional(
        dbClient.organizationDao().selectByKey(dbSession, key),
        "Organization with key '%s' not found",
        key);

      userSession.checkOrganizationPermission(organizationDto.getUuid(), SYSTEM_ADMIN);

      deleteProjects(dbSession, organizationDto.getUuid());
      deletePermissions(dbSession, organizationDto.getUuid());
      deleteGroups(dbSession, organizationDto.getUuid());
      deleteOrganization(key, dbSession);

      response.noContent();
    }
  }

  private void deleteProjects(DbSession dbSession, String organizationUuid) {
    List<ComponentDto> roots = dbClient.componentDao().selectAllRootsByOrganization(dbSession, organizationUuid);
    componentCleanerService.delete(dbSession, roots);
  }

  private void deletePermissions(DbSession dbSession, String organizationUuid) {
    dbClient.permissionTemplateDao().deleteByOrganization(dbSession, organizationUuid);
    dbSession.commit();
    dbClient.userPermissionDao().deleteByOrganization(dbSession, organizationUuid);
    dbSession.commit();
    dbClient.groupPermissionDao().deleteByOrganization(dbSession, organizationUuid);
    dbSession.commit();
  }

  private void deleteGroups(DbSession dbSession, String organizationUuid) {
    dbClient.groupDao().deleteByOrganization(dbSession, organizationUuid);
    dbSession.commit();
  }

  private void deleteOrganization(String key, DbSession dbSession) {
    dbClient.organizationDao().deleteByKey(dbSession, key);
    dbSession.commit();
  }

  private static void preventDeletionOfDefaultOrganization(String key, DefaultOrganization defaultOrganization) {
    checkArgument(!defaultOrganization.getKey().equals(key), "Default Organization can't be deleted");
  }
}
