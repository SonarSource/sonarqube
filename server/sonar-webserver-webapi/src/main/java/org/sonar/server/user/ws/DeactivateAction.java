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
package org.sonar.server.user.ws;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationHelper;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_ENABLED;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class DeactivateAction implements UsersWsAction {

  private static final Logger LOGGER = Loggers.get(DeactivateAction.class);

  private static final String PARAM_LOGIN = "login";

  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final boolean isSonarCloud;

  public DeactivateAction(DbClient dbClient, UserIndexer userIndexer, UserSession userSession, UserJsonWriter userWriter,
    DefaultOrganizationProvider defaultOrganizationProvider, Configuration configuration) {
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.userSession = userSession;
    this.userWriter = userWriter;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.isSonarCloud = configuration.getBoolean(SONARCLOUD_ENABLED.getKey()).orElse(false);
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("deactivate")
      .setDescription("Deactivate a user. Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setResponseExample(getClass().getResource("deactivate-example.json"))
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String login;

    if (isSonarCloud) {
      login = request.mandatoryParam(PARAM_LOGIN);
      if (!login.equals(userSession.getLogin()) && !userSession.checkLoggedIn().isSystemAdministrator()) {
        throw new ForbiddenException("Insufficient privileges");
      }
    } else {
      userSession.checkLoggedIn().checkIsSystemAdministrator();
      login = request.mandatoryParam(PARAM_LOGIN);
      checkRequest(!login.equals(userSession.getLogin()), "Self-deactivation is not possible");
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      checkFound(user, "User '%s' doesn't exist", login);

      ensureNotLastAdministrator(dbSession, user);

      Integer userId = user.getId();
      dbClient.userTokenDao().deleteByUser(dbSession, user);
      dbClient.propertiesDao().deleteByKeyAndValue(dbSession, DEFAULT_ISSUE_ASSIGNEE, user.getLogin());
      dbClient.propertiesDao().deleteByQuery(dbSession, PropertyQuery.builder().setUserId(userId).build());
      dbClient.userGroupDao().deleteByUserId(dbSession, userId);
      dbClient.userPermissionDao().deleteByUserId(dbSession, userId);
      dbClient.permissionTemplateDao().deleteUserPermissionsByUserId(dbSession, userId);
      dbClient.qProfileEditUsersDao().deleteByUser(dbSession, user);
      dbClient.organizationMemberDao().deleteByUserId(dbSession, userId);
      dbClient.userPropertiesDao().deleteByUser(dbSession, user);
      deactivateUser(dbSession, user);
      userIndexer.commitAndIndex(dbSession, user);

      LOGGER.info("Deactivate user: {}; by admin: {}", login, userSession.isSystemAdministrator());
    }

    writeResponse(response, login);
  }

  private void deactivateUser(DbSession dbSession, UserDto user) {
    if (isSonarCloud) {
      dbClient.userDao().deactivateSonarCloudUser(dbSession, user);
    } else {
      dbClient.userDao().deactivateUser(dbSession, user);
    }
  }

  private void writeResponse(Response response, String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      // safeguard. It exists as the check has already been done earlier
      // when deactivating user
      checkFound(user, "User '%s' doesn't exist", login);

      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        json.name("user");
        Set<String> groups = new HashSet<>();
        groups.addAll(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(login)).get(login));
        userWriter.write(json, user, groups, UserJsonWriter.FIELDS);
        json.endObject();
      }
    }
  }

  private void ensureNotLastAdministrator(DbSession dbSession, UserDto user) {
    List<OrganizationDto> problematicOrgs = new OrganizationHelper(dbClient).selectOrganizationsWithLastAdmin(dbSession, user.getId());
    if (problematicOrgs.isEmpty()) {
      return;
    }
    checkRequest(problematicOrgs.size() != 1 || !defaultOrganizationProvider.get().getUuid().equals(problematicOrgs.get(0).getUuid()),
      "User is last administrator, and cannot be deactivated");
    String keys = problematicOrgs
      .stream()
      .map(OrganizationDto::getKey)
      .sorted()
      .collect(Collectors.joining(", "));
    throw BadRequestException.create(format("User '%s' is last administrator of organizations [%s], and cannot be deactivated", user.getLogin(), keys));
  }

}
