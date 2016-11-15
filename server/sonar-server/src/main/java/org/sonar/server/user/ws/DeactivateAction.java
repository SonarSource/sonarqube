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
package org.sonar.server.user.ws;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class DeactivateAction implements UsersWsAction {

  private static final String PARAM_LOGIN = "login";

  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public DeactivateAction(DbClient dbClient, UserIndexer userIndexer, UserSession userSession, UserJsonWriter userWriter,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.userSession = userSession;
    this.userWriter = userWriter;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("deactivate")
      .setDescription("Deactivate a user. Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setResponseExample(getClass().getResource("example-deactivate.json"))
      .setHandler(this);

    action.createParam("login")
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkPermission(SYSTEM_ADMIN);

    String login = request.mandatoryParam(PARAM_LOGIN);
    checkRequest(!login.equals(userSession.getLogin()), "Self-deactivation is not possible");

    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      checkFound(user, "User '%s' doesn't exist", login);

      ensureNotLastAdministrator(dbSession, user);

      dbClient.userTokenDao().deleteByLogin(dbSession, login);
      dbClient.userDao().deactivateUserByLogin(dbSession, login);
      dbSession.commit();
    }

    userIndexer.index();
    writeResponse(response, login);
  }

  private void writeResponse(Response response, String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      // safeguard. It exists as the check has already been done earlier
      // when deactivating user
      checkFound(user, "User '%s' doesn't exist", login);

      JsonWriter json = response.newJsonWriter().beginObject();
      json.name("user");
      Set<String> groups = Sets.newHashSet();
      groups.addAll(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(login)).get(login));
      userWriter.write(json, user, groups, UserJsonWriter.FIELDS);
      json.endObject().close();
    }
  }

  private void ensureNotLastAdministrator(DbSession dbSession, UserDto user) {
    List<String> problematicOrgs = selectOrganizationsWithNoMoreAdministrators(dbSession, user);
    if (!problematicOrgs.isEmpty()) {
      if (problematicOrgs.size() == 1 && defaultOrganizationProvider.get().getUuid().equals(problematicOrgs.get(0))) {
        throw new BadRequestException("User is last administrator, and cannot be deactivated");
      }
      String keys = problematicOrgs
        .stream()
        .map(orgUuid -> selectOrganizationByUuid(dbSession, orgUuid, user))
        .map(OrganizationDto::getKey)
        .sorted()
        .collect(Collectors.joining(", "));
      throw new BadRequestException(format("User is last administrator of organizations [%s], and cannot be deactivated", keys));

    }
  }

  private List<String> selectOrganizationsWithNoMoreAdministrators(DbSession dbSession, UserDto user) {
    Set<String> organizationUuids = dbClient.authorizationDao().selectOrganizationUuidsOfUserWithGlobalPermission(
      dbSession, user.getId(), SYSTEM_ADMIN);
    List<String> problematicOrganizations = new ArrayList<>();
    for (String organizationUuid : organizationUuids) {
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUser(dbSession, organizationUuid, SYSTEM_ADMIN, user.getId());
      if (remaining == 0) {
        problematicOrganizations.add(organizationUuid);
      }
    }
    return problematicOrganizations;
  }

  private OrganizationDto selectOrganizationByUuid(DbSession dbSession, String orgUuid, UserDto user) {
    return dbClient.organizationDao()
      .selectByUuid(dbSession, orgUuid)
      .orElseThrow(() -> new IllegalStateException("Organization with UUID " + orgUuid + " does not exist in DB but is referenced in permissions of user " + user.getLogin()));
  }
}
