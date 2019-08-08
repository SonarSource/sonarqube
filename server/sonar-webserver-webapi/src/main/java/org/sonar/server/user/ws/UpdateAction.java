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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.sonar.server.user.UserUpdater.EMAIL_MAX_LENGTH;
import static org.sonar.server.user.UserUpdater.LOGIN_MAX_LENGTH;
import static org.sonar.server.user.UserUpdater.NAME_MAX_LENGTH;
import static org.sonar.server.user.ws.EmailValidator.isValidIfPresent;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_UPDATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS_DEPRECATED;

public class UpdateAction implements UsersWsAction {

  private final UserUpdater userUpdater;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;
  private final DbClient dbClient;

  public UpdateAction(UserUpdater userUpdater, UserSession userSession, UserJsonWriter userWriter, DbClient dbClient) {
    this.userUpdater = userUpdater;
    this.userSession = userSession;
    this.userWriter = userWriter;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_UPDATE)
      .setDescription("Update a user.<br/>" +
        "Requires Administer System permission")
      .setSince("3.7")
      .setChangelog(new Change("5.2", "User's password can only be changed using the 'change_password' action."))
      .setPost(true)
      .setHandler(this)
      .setResponseExample(getClass().getResource("update-example.json"));

    action.createParam(PARAM_LOGIN)
      .setRequired(true)
      .setMaximumLength(LOGIN_MAX_LENGTH)
      .setDescription("User login")
      .setExampleValue("myuser");

    action.createParam(PARAM_NAME)
      .setMaximumLength(NAME_MAX_LENGTH)
      .setDescription("User name")
      .setExampleValue("My Name");

    action.createParam(PARAM_EMAIL)
      .setMaximumLength(EMAIL_MAX_LENGTH)
      .setDescription("User email")
      .setExampleValue("myname@email.com");

    action.createParam(PARAM_SCM_ACCOUNTS)
      .setDescription("This parameter is deprecated, please use '%s' instead", PARAM_SCM_ACCOUNT)
      .setDeprecatedKey(PARAM_SCM_ACCOUNTS_DEPRECATED, "6.0")
      .setDeprecatedSince("6.1")
      .setExampleValue("myscmaccount1,myscmaccount2");

    action.createParam(PARAM_SCM_ACCOUNT)
      .setDescription("SCM accounts. To set several values, the parameter must be called once for each value.")
      .setExampleValue("scmAccount=firstValue&scmAccount=secondValue&scmAccount=thirdValue");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    UpdateRequest updateRequest = toWsRequest(request);
    checkArgument(isValidIfPresent(updateRequest.getEmail()), "Email '%s' is not valid", updateRequest.getEmail());
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = getUser(dbSession, updateRequest.getLogin());
      doHandle(dbSession, updateRequest);
      writeUser(dbSession, response, user.getUuid());
    }
  }

  private void doHandle(DbSession dbSession, UpdateRequest request) {
    String login = request.getLogin();
    UserDto user = getUser(dbSession, login);
    UpdateUser updateUser = new UpdateUser();
    if (request.getName() != null) {
      Preconditions.checkArgument(user.isLocal(), "Name cannot be updated for a non-local user");
      updateUser.setName(request.getName());
    }
    if (request.getEmail() != null) {
      Preconditions.checkArgument(user.isLocal(), "Email cannot be updated for a non-local user");
      updateUser.setEmail(emptyToNull(request.getEmail()));
    }
    if (!request.getScmAccounts().isEmpty()) {
      updateUser.setScmAccounts(request.getScmAccounts());
    }
    userUpdater.updateAndCommit(dbSession, user, updateUser, u -> {
    });
  }

  private UserDto getUser(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    if (user == null || !user.isActive()) {
      throw new NotFoundException(format("User '%s' doesn't exist", login));
    }
    return user;
  }

  private void writeUser(DbSession dbSession, Response response, String uuid) {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      json.name("user");
      UserDto user = dbClient.userDao().selectByUuid(dbSession, uuid);
      checkState(user != null, "User with uuid '%s' doesn't exist", uuid);
      Set<String> groups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(uuid)).get(uuid));
      userWriter.write(json, user, groups, UserJsonWriter.FIELDS);
      json.endObject().close();
    }
  }

  private static UpdateRequest toWsRequest(Request request) {
    return UpdateRequest.builder()
      .setLogin(request.mandatoryParam(PARAM_LOGIN))
      .setName(request.param(PARAM_NAME))
      .setEmail(request.param(PARAM_EMAIL))
      .setScmAccounts(getScmAccounts(request))
      .build();
  }

  private static List<String> getScmAccounts(Request request) {
    if (request.hasParam(PARAM_SCM_ACCOUNT)) {
      return new ArrayList<>(request.multiParam(PARAM_SCM_ACCOUNT));
    }
    List<String> oldScmAccounts = request.paramAsStrings(PARAM_SCM_ACCOUNTS);
    return oldScmAccounts != null ? oldScmAccounts : new ArrayList<>();
  }

  private static class UpdateRequest {

    private final String login;
    private final String name;
    private final String email;
    private final List<String> scmAccounts;

    private UpdateRequest(Builder builder) {
      this.login = builder.login;
      this.name = builder.name;
      this.email = builder.email;
      this.scmAccounts = builder.scmAccounts;
    }

    public String getLogin() {
      return login;
    }

    @CheckForNull
    public String getName() {
      return name;
    }

    @CheckForNull
    public String getEmail() {
      return email;
    }

    public List<String> getScmAccounts() {
      return scmAccounts;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private String login;
    private String name;
    private String email;
    private List<String> scmAccounts = emptyList();

    private Builder() {
      // enforce factory method use
    }

    public Builder setLogin(String login) {
      this.login = login;
      return this;
    }

    public Builder setName(@Nullable String name) {
      this.name = name;
      return this;
    }

    public Builder setEmail(@Nullable String email) {
      this.email = email;
      return this;
    }

    public Builder setScmAccounts(List<String> scmAccounts) {
      this.scmAccounts = scmAccounts;
      return this;
    }

    public UpdateRequest build() {
      checkArgument(!isNullOrEmpty(login), "Login is mandatory and must not be empty");
      return new UpdateRequest(this);
    }
  }
}
