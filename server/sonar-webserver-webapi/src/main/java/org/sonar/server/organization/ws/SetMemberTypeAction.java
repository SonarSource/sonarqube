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

import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.organization.ws.MemberUpdater.MemberType;
import org.sonar.db.permission.GlobalPermission;

public class SetMemberTypeAction implements OrganizationsWsAction {

    private static final String ACTION = "set_member_type";
    private static final String SYSTEM_ADMIN_CANNOT_ACT_AS_PLATFORM_USER_ERROR_MSG =
            "You are a System Admin. You are required to have a Standard User License.";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_LOGIN = "login";
    public static final String PARAM_ORG_KEE = "organization";

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
            OrganizationDto organizationDto = getOrganization(dbSession, orgKee);
            UserDto userDto = dbClient.userDao().selectByLogin(dbSession, login);
            userSession.checkPermission(OrganizationPermission.ADMINISTER, organizationDto);
            Set<String> permissions = dbClient.authorizationDao().selectOrganizationPermissions(dbSession,organizationDto.getUuid(), userDto.getUuid());
            checkRequest(!(permissions.contains(GlobalPermission.ADMINISTER.name()) && type.equals(MemberType.PLATFORM.name())), SYSTEM_ADMIN_CANNOT_ACT_AS_PLATFORM_USER_ERROR_MSG);
            dbClient.organizationMemberDao().updateOrgMemberType(dbSession, orgKee, login, type);
            dbSession.commit();
        }

        response.noContent();
    }

    private OrganizationDto getOrganization(DbSession dbSession, @Nullable String organizationKey) {
        return checkFoundWithOptional(
                dbClient.organizationDao().selectByKey(dbSession, organizationKey),
                "No organization with key '%s'", organizationKey);
    }
}

