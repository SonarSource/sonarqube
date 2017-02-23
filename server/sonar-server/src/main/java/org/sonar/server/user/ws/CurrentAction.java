/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Collection;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_EXTERNAL_IDENTITY;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_EXTERNAL_PROVIDER;

public class CurrentAction implements UsersWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public CurrentAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(NewController context) {
    context.createAction("current")
      .setDescription("Get the details of the current authenticated user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("current-example.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<UserDto> user = Optional.empty();
      Collection<String> groups = emptyList();
      if (userSession.isLoggedIn()) {
        user = selectCurrentUser(dbSession);
        groups = selectGroups(dbSession);
      }
      writeResponse(response, user, groups);
    }
  }

  private Optional<UserDto> selectCurrentUser(DbSession dbSession) {
    return Optional.ofNullable(dbClient.userDao().selectActiveUserByLogin(dbSession, userSession.getLogin()));
  }

  private Collection<String> selectGroups(DbSession dbSession) {
    return dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(userSession.getLogin()))
      .get(userSession.getLogin());
  }

  private void writeResponse(Response response, Optional<UserDto> user, Collection<String> groups) {
    JsonWriter json = response.newJsonWriter().beginObject();
    writeUserDetails(json, user, groups);
    json.endObject().close();
  }

  private void writeUserDetails(JsonWriter json, Optional<UserDto> optionalUser, Collection<String> groups) {
    json
      .prop("isLoggedIn", userSession.isLoggedIn())
      .prop("login", userSession.getLogin())
      .prop("name", userSession.getName());
    if (optionalUser.isPresent()) {
      UserDto user = optionalUser.get();
      if (!isNullOrEmpty(user.getEmail())) {
        json.prop("email", user.getEmail());
      }
      json.prop("local", user.isLocal());
      json.prop(FIELD_EXTERNAL_IDENTITY, user.getExternalIdentity());
      json.prop(FIELD_EXTERNAL_PROVIDER, user.getExternalIdentityProvider());
    }

    writeScmAccounts(json, optionalUser);
    writeGroups(json, groups);
    writePermissions(json);
  }

  private static void writeScmAccounts(JsonWriter json, Optional<UserDto> optionalUser) {
    json.name("scmAccounts");
    json.beginArray();
    if (optionalUser.isPresent()) {
      for (String scmAccount : optionalUser.get().getScmAccountsAsList()) {
        json.value(scmAccount);
      }
    }
    json.endArray();
  }

  private static void writeGroups(JsonWriter json, Collection<String> groups) {
    json.name("groups");
    json.beginArray();
    for (String group : groups) {
      json.value(group);
    }
    json.endArray();
  }

  private void writePermissions(JsonWriter json) {
    json.name("permissions").beginObject();
    writeGlobalPermissions(json);
    json.endObject();
  }

  private void writeGlobalPermissions(JsonWriter json) {
    json.name("global").beginArray();

    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    OrganizationPermission.all()
      .filter(permission -> userSession.hasPermission(permission, defaultOrganizationUuid))
      .forEach(permission -> json.value(permission.getKey()));

    json.endArray();
  }

}
