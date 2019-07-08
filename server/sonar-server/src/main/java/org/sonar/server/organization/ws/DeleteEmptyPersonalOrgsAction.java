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
package org.sonar.server.organization.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.organization.OrganizationQuery;
import org.sonar.server.user.AbstractUserSession;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;

public class DeleteEmptyPersonalOrgsAction implements OrganizationsWsAction {

  private static final Logger LOGGER = Loggers.get(DeleteEmptyPersonalOrgsAction.class);

  private static final String ACTION = "delete_empty_personal_orgs";

  private final SystemPasscode passcode;
  private final UserSession userSession;
  private final OrganizationDeleter organizationDeleter;

  public DeleteEmptyPersonalOrgsAction(SystemPasscode passcode, UserSession userSession, OrganizationDeleter organizationDeleter) {
    this.passcode = passcode;
    this.userSession = userSession;
    this.organizationDeleter = organizationDeleter;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setDescription("Internal use. Requires system administration permission. Delete empty personal organizations.")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (!passcode.isValid(request) && !userSession.isSystemAdministrator()) {
      throw AbstractUserSession.insufficientPrivilegesException();
    }

    LOGGER.info("deleting empty personal organizations");

    OrganizationQuery query = OrganizationQuery.newOrganizationQueryBuilder()
      .setOnlyPersonal()
      .setWithoutProjects()
      .build();

    organizationDeleter.deleteByQuery(query);

    LOGGER.info("Deleted empty personal organizations");

    response.noContent();
  }

}
