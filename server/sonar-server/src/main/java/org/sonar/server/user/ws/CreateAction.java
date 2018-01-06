/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;
import org.sonarqube.ws.Users.CreateWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;
import static org.sonar.server.user.UserUpdater.EMAIL_MAX_LENGTH;
import static org.sonar.server.user.UserUpdater.LOGIN_MAX_LENGTH;
import static org.sonar.server.user.UserUpdater.NAME_MAX_LENGTH;
import static org.sonar.server.user.ws.EmailValidator.isValidIfPresent;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOCAL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS_DEPRECATED;

public class CreateAction implements UsersWsAction {

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final UserSession userSession;

  public CreateAction(DbClient dbClient, UserUpdater userUpdater, UserSession userSession) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_CREATE)
      .setDescription("Create a user.<br/>" +
        "If a deactivated user account exists with the given login, it will be reactivated.<br/>" +
        "Requires Administer System permission")
      .setSince("3.7")
      .setChangelog(
        new Change("6.3", "The password is only mandatory when creating local users, and should not be set on non local users"),
        new Change("6.3", "The 'infos' message is no more returned when a user is reactivated"))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setRequired(true)
      .setMaximumLength(LOGIN_MAX_LENGTH)
      .setDescription("User login")
      .setExampleValue("myuser");

    action.createParam(PARAM_PASSWORD)
      .setDescription("User password. Only mandatory when creating local user, otherwise it should not be set")
      .setExampleValue("mypassword");

    action.createParam(PARAM_NAME)
      .setRequired(true)
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

    action.createParam(PARAM_LOCAL)
      .setDescription("Specify if the user should be authenticated from SonarQube server or from an external authentication system. " +
        "Password should not be set when local is set to false.")
      .setSince("6.3")
      .setDefaultValue("true")
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    CreateRequest createRequest = toWsRequest(request);
    checkArgument(isValidIfPresent(createRequest.getEmail()), "Email '%s' is not valid", createRequest.getEmail());
    writeProtobuf(doHandle(createRequest), request, response);
  }

  private CreateWsResponse doHandle(CreateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      NewUser.Builder newUser = NewUser.builder()
        .setLogin(request.getLogin())
        .setName(request.getName())
        .setEmail(request.getEmail())
        .setScmAccounts(request.getScmAccounts())
        .setPassword(request.getPassword());
      if (!request.isLocal()) {
        newUser.setExternalIdentity(new ExternalIdentity(SQ_AUTHORITY, request.getLogin()));
      }
      UserDto createdUser = userUpdater.createAndCommit(dbSession, newUser.build(), u -> {});
      return buildResponse(createdUser);
    }
  }

  private static CreateWsResponse buildResponse(UserDto userDto) {
    CreateWsResponse.User.Builder userBuilder = CreateWsResponse.User.newBuilder()
      .setLogin(userDto.getLogin())
      .setName(userDto.getName())
      .setActive(userDto.isActive())
      .setLocal(userDto.isLocal())
      .addAllScmAccounts(userDto.getScmAccountsAsList());
    setNullable(emptyToNull(userDto.getEmail()), userBuilder::setEmail);
    return CreateWsResponse.newBuilder().setUser(userBuilder).build();
  }

  private static CreateRequest toWsRequest(Request request) {
    return CreateRequest.builder()
      .setLogin(request.mandatoryParam(PARAM_LOGIN))
      .setPassword(request.param(PARAM_PASSWORD))
      .setName(request.param(PARAM_NAME))
      .setEmail(request.param(PARAM_EMAIL))
      .setScmAccounts(getScmAccounts(request))
      .setLocal(request.mandatoryParamAsBoolean(PARAM_LOCAL))
      .build();
  }

  private static List<String> getScmAccounts(Request request) {
    if (request.hasParam(PARAM_SCM_ACCOUNT)) {
      return request.multiParam(PARAM_SCM_ACCOUNT);
    }
    List<String> oldScmAccounts = request.paramAsStrings(PARAM_SCM_ACCOUNTS);
    return oldScmAccounts != null ? oldScmAccounts : Collections.emptyList();
  }

  static class CreateRequest {

    private final String login;
    private final String password;
    private final String name;
    private final String email;
    private final List<String> scmAccounts;
    private final boolean local;

    private CreateRequest(Builder builder) {
      this.login = builder.login;
      this.password = builder.password;
      this.name = builder.name;
      this.email = builder.email;
      this.scmAccounts = builder.scmAccounts;
      this.local = builder.local;
    }

    public String getLogin() {
      return login;
    }

    @CheckForNull
    public String getPassword() {
      return password;
    }

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

    public boolean isLocal() {
      return local;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  static class Builder {
    private String login;
    private String password;
    private String name;
    private String email;
    private List<String> scmAccounts = emptyList();
    private boolean local = true;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLogin(String login) {
      this.login = login;
      return this;
    }

    public Builder setPassword(@Nullable String password) {
      this.password = password;
      return this;
    }

    public Builder setName(String name) {
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

    public Builder setLocal(boolean local) {
      this.local = local;
      return this;
    }

    public CreateRequest build() {
      checkArgument(!isNullOrEmpty(login), "Login is mandatory and must not be empty");
      checkArgument(!isNullOrEmpty(name), "Name is mandatory and must not be empty");
      checkArgument(!local || !isNullOrEmpty(password), "Password is mandatory and must not be empty");
      checkArgument(local || isNullOrEmpty(password), "Password should only be set on local user");
      return new CreateRequest(this);
    }
  }
}
