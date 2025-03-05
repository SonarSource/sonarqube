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
package org.sonar.server.organization.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.user.UserSession;

public class SetMemberTypeAction implements OrganizationsWsAction {

    private static final String ACTION = "set_member_type";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_LOGIN = "login";
    public static final String PARAM_ORG_KEE = "orgKee";

    private final DbClient dbClient;
    private final UserSession userSession;

    public SetMemberTypeAction(DbClient dbClient, UserSession userSession) {
        this.dbClient = dbClient;
        this.userSession = userSession;
    }

    @Override
    public void define(WebService.NewController controller) {
        WebService.NewAction action = controller.createAction(ACTION)
                .setPost(true)
                .setInternal(true)
                .setDescription("Set member type.<br> " +
                        "Requires admin authentication.")
                .setSince("7.0")
                .setHandler(this);
        action.createParam(PARAM_ORG_KEE)
                .setDescription("Organization key")
                .setRequired(true);
        action.createParam(PARAM_LOGIN)
                .setDescription("Login name of member")
                .setRequired(true);
        action.createParam(PARAM_TYPE)
                .setDescription("Type of member")
                .setRequired(true);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        userSession.checkLoggedIn();
        String orgKee = request.mandatoryParam(PARAM_ORG_KEE);
        String login = request.mandatoryParam(PARAM_LOGIN);
        String type = request.mandatoryParam(PARAM_TYPE);

        try (DbSession dbSession = dbClient.openSession(false)) {
            dbClient.organizationMemberDao().updateOrgMemberType(dbSession, orgKee, login, type);
            dbSession.commit();
        }

        response.noContent();
    }

}

