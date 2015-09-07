/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.permission.ws.template.DefaultPermissionTemplateFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.WsPermissionParameters.createTemplateParameters;
import static org.sonar.server.permission.ws.WsTemplateRef.fromRequest;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class DeleteTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionDependenciesFinder finder;
  private final DefaultPermissionTemplateFinder defaultPermissionTemplateFinder;

  public DeleteTemplateAction(DbClient dbClient, UserSession userSession, PermissionDependenciesFinder finder, DefaultPermissionTemplateFinder defaultPermissionTemplateFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.finder = finder;
    this.defaultPermissionTemplateFinder = defaultPermissionTemplateFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("delete_template")
      .setDescription("Delete a permission template.<br />" +
        "It requires administration permissions to access.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createTemplateParameters(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkGlobalAdminUser(userSession);

    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionTemplateDto template = finder.getTemplate(dbSession, fromRequest(wsRequest));
      checkTemplateUuidIsNotDefault(template.getUuid());
      dbClient.permissionTemplateDao().deleteById(dbSession, template.getId());
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }

    wsResponse.noContent();
  }

  private void checkTemplateUuidIsNotDefault(String key) {
    Set<String> defaultTemplateUuids = defaultPermissionTemplateFinder.getDefaultTemplateUuids();
    checkRequest(!defaultTemplateUuids.contains(key), "It is not possible to delete a default template");
  }
}
