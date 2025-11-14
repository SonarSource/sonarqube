/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.auth.ldap.LdapRealm.DEFAULT_LDAP_IDENTITY_PROVIDER_ID;
import static org.sonar.auth.ldap.LdapRealm.LDAP_SECURITY_REALM;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_UPDATE_IDENTITY_PROVIDER;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NEW_EXTERNAL_IDENTITY;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NEW_EXTERNAL_PROVIDER;

public class UpdateIdentityProviderAction implements UsersWsAction {

  private final DbClient dbClient;
  private final IdentityProviderRepository identityProviderRepository;
  private final UserUpdater userUpdater;
  private final UserSession userSession;
  private final ManagedInstanceChecker managedInstanceChecker;

  public UpdateIdentityProviderAction(DbClient dbClient, IdentityProviderRepository identityProviderRepository,
    UserUpdater userUpdater, UserSession userSession, ManagedInstanceChecker managedInstanceChecker) {
    this.dbClient = dbClient;
    this.identityProviderRepository = identityProviderRepository;
    this.userUpdater = userUpdater;
    this.userSession = userSession;
    this.managedInstanceChecker = managedInstanceChecker;
  }

  @Override
  public void define(NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_UPDATE_IDENTITY_PROVIDER)
      .setDescription("Update identity provider information. <br/>"
        + "It's only possible to migrate to an installed identity provider. "
        + "Be careful that as soon as this information has been updated for a user, "
        + "the user will only be able to authenticate on the new identity provider. "
        + "It is not possible to migrate external user to local one.<br/>"
        + "Requires Administer System permission.")
      .setSince("8.7")
      .setInternal(false)
      .setPost(true)
      .setHandler(this)
      .setDeprecatedSince("10.4")
      .setChangelog(new Change("10.4", "Deprecated. Use PATCH api/v2/users-management/users/{id} instead"));

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true);

    action.createParam(PARAM_NEW_EXTERNAL_PROVIDER)
      .setRequired(true)
      .setDescription("New external provider. Only authentication system installed are available. " +
        "Use 'LDAP' identity provider for single server LDAP setup." +
        "Use 'LDAP_{serverKey}' identity provider for multiple LDAP servers setup.");

    action.setChangelog(
      new Change("9.8", String.format("Use of 'sonarqube' for the value of '%s' is deprecated.", PARAM_NEW_EXTERNAL_PROVIDER)));

    action.createParam(PARAM_NEW_EXTERNAL_IDENTITY)
      .setDescription("New external identity, usually the login used in the authentication system. "
        + "If not provided previous identity will be used.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    managedInstanceChecker.throwIfInstanceIsManaged();
    UpdateIdentityProviderRequest wsRequest = toWsRequest(request);
    doHandle(wsRequest);
    response.noContent();
  }

  private void doHandle(UpdateIdentityProviderRequest request) {
    checkEnabledIdentityProviders(request.newExternalProvider);
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = getUser(dbSession, request.login);
      UpdateUser updateUser = toUpdateUser(request, user);
      userUpdater.updateAndCommit(dbSession, user, updateUser, u -> {
      });
    }
  }

  private void checkEnabledIdentityProviders(String newExternalProvider) {
    List<String> allowedIdentityProviders = getAvailableIdentityProviders();

    boolean isAllowedProvider = allowedIdentityProviders.contains(newExternalProvider) || isLdapIdentityProvider(newExternalProvider);
    checkArgument(isAllowedProvider, "Value of parameter 'newExternalProvider' (%s) must be one of: [%s] or [%s]", newExternalProvider,
      String.join(", ", allowedIdentityProviders), String.join(", ", "LDAP", "LDAP_{serverKey}"));
  }

  private List<String> getAvailableIdentityProviders() {
    return identityProviderRepository.getAllEnabledAndSorted()
      .stream()
      .map(IdentityProvider::getKey)
      .toList();
  }

  private static boolean isLdapIdentityProvider(String identityProviderKey) {
    return identityProviderKey.startsWith(LDAP_SECURITY_REALM);
  }

  private UserDto getUser(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    if (user == null || !user.isActive()) {
      throw new NotFoundException(format("User '%s' doesn't exist", login));
    }
    return user;
  }

  private static UpdateUser toUpdateUser(UpdateIdentityProviderRequest request, UserDto user) {
    return new UpdateUser()
      .setExternalIdentityProvider(request.newExternalProvider)
      .setExternalIdentityProviderLogin(Optional.ofNullable(request.newExternalIdentity).orElse(user.getExternalLogin())
      );
  }

  private static UpdateIdentityProviderRequest toWsRequest(Request request) {
    return UpdateIdentityProviderRequest.builder()
      .setLogin(request.mandatoryParam(PARAM_LOGIN))
      .setNewExternalProvider(replaceDeprecatedSonarqubeIdentityProviderByLdapForSonar17508(request.mandatoryParam(PARAM_NEW_EXTERNAL_PROVIDER)))
      .setNewExternalIdentity(request.param(PARAM_NEW_EXTERNAL_IDENTITY))
      .build();
  }

  private static String replaceDeprecatedSonarqubeIdentityProviderByLdapForSonar17508(String newExternalProvider) {
    return newExternalProvider.equals(SQ_AUTHORITY) || newExternalProvider.equals(LDAP_SECURITY_REALM) ? DEFAULT_LDAP_IDENTITY_PROVIDER_ID : newExternalProvider;
  }

  static class UpdateIdentityProviderRequest {
    private final String login;
    private final String newExternalProvider;
    private final String newExternalIdentity;

    public UpdateIdentityProviderRequest(Builder builder) {
      this.login = builder.login;
      this.newExternalProvider = builder.newExternalProvider;
      this.newExternalIdentity = builder.newExternalIdentity;
    }

    public static UpdateIdentityProviderRequest.Builder builder() {
      return new UpdateIdentityProviderRequest.Builder();
    }

    static class Builder {
      private String login;
      private String newExternalProvider;
      private String newExternalIdentity;

      private Builder() {
        // enforce factory method use
      }

      public Builder setLogin(String login) {
        this.login = login;
        return this;
      }

      public Builder setNewExternalProvider(String newExternalProvider) {
        this.newExternalProvider = newExternalProvider;
        return this;
      }

      public Builder setNewExternalIdentity(@Nullable String newExternalIdentity) {
        this.newExternalIdentity = newExternalIdentity;
        return this;
      }

      public UpdateIdentityProviderRequest build() {
        checkArgument(!isNullOrEmpty(login), "Login is mandatory and must not be empty");
        checkArgument(!isNullOrEmpty(newExternalProvider), "New External Provider is mandatory and must not be empty");
        return new UpdateIdentityProviderRequest(this);
      }
    }
  }

}
