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

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganisationSupport;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;

public class EnableSupportAction implements OrganizationsWsAction {
  private static final String ACTION = "enable_support";

  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganisationSupport organisationSupport;

  public EnableSupportAction(UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider, OrganisationSupport organisationSupport) {
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organisationSupport = organisationSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setPost(true)
      .setDescription("Enable support of organizations.<br />" +
        "'Administer System' permission is required. The logged-in user will be flagged as root and will be able to manage organizations and other root users.")
      .setInternal(true)
      .setPost(true)
      .setSince("6.3")
      .setChangelog(new Change("6.4", "Create default 'Members' group"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    verifySystemAdministrator();

    organisationSupport.enable(requireNonNull(userSession.getLogin()));
    response.noContent();
  }

  private void verifySystemAdministrator() {
    userSession.checkLoggedIn().checkPermission(OrganizationPermission.ADMINISTER, defaultOrganizationProvider.get().getUuid());
  }

}
