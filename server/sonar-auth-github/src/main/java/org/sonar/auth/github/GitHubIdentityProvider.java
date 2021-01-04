/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.github;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class GitHubIdentityProvider implements OAuth2IdentityProvider {

  static final String KEY = "github";

  private final GitHubSettings settings;
  private final UserIdentityFactory userIdentityFactory;
  private final ScribeGitHubApi scribeApi;
  private final GitHubRestClient gitHubRestClient;

  public GitHubIdentityProvider(GitHubSettings settings, UserIdentityFactory userIdentityFactory, ScribeGitHubApi scribeApi, GitHubRestClient gitHubRestClient) {
    this.settings = settings;
    this.userIdentityFactory = userIdentityFactory;
    this.scribeApi = scribeApi;
    this.gitHubRestClient = gitHubRestClient;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return "GitHub";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      .setIconPath("/images/github.svg")
      .setBackgroundColor("#444444")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return settings.isEnabled();
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return settings.allowUsersToSignUp();
  }

  @Override
  public void init(InitContext context) {
    String state = context.generateCsrfState();
    OAuth20Service scribe = newScribeBuilder(context)
      .defaultScope(getScope())
      .build(scribeApi);
    String url = scribe.getAuthorizationUrl(state);
    context.redirectTo(url);
  }

  String getScope() {
    return (settings.syncGroups() || isOrganizationMembershipRequired()) ? "user:email,read:org" : "user:email";
  }

  @Override
  public void callback(CallbackContext context) {
    try {
      onCallback(context);
    } catch (IOException | ExecutionException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private void onCallback(CallbackContext context) throws InterruptedException, ExecutionException, IOException {
    context.verifyCsrfState();

    HttpServletRequest request = context.getRequest();
    OAuth20Service scribe = newScribeBuilder(context).build(scribeApi);
    String code = request.getParameter("code");
    OAuth2AccessToken accessToken = scribe.getAccessToken(code);

    GsonUser user = gitHubRestClient.getUser(scribe, accessToken);
    check(scribe, accessToken, user);

    final String email;
    if (user.getEmail() == null) {
      // if the user has not specified a public email address in their profile
      email = gitHubRestClient.getEmail(scribe, accessToken);
    } else {
      email = user.getEmail();
    }

    UserIdentity userIdentity = userIdentityFactory.create(user, email,
      settings.syncGroups() ? gitHubRestClient.getTeams(scribe, accessToken) : null);
    context.authenticate(userIdentity);
    context.redirectToRequestedPage();
  }

  boolean isOrganizationMembershipRequired() {
    return settings.organizations().length > 0;
  }

  private void check(OAuth20Service scribe, OAuth2AccessToken accessToken, GsonUser user) throws InterruptedException, ExecutionException, IOException {
    if (isUnauthorized(scribe, accessToken, user.getLogin())) {
      throw new UnauthorizedException(format("'%s' must be a member of at least one organization: '%s'", user.getLogin(), String.join("', '", settings.organizations())));
    }
  }

  private boolean isUnauthorized(OAuth20Service scribe, OAuth2AccessToken accessToken, String login) throws IOException, ExecutionException, InterruptedException {
    return isOrganizationMembershipRequired() && !isOrganizationsMember(scribe, accessToken, login);
  }

  private boolean isOrganizationsMember(OAuth20Service scribe, OAuth2AccessToken accessToken, String login) throws IOException, ExecutionException, InterruptedException {
    for (String organization : settings.organizations()) {
      if (gitHubRestClient.isOrganizationMember(scribe, accessToken, organization, login)) {
        return true;
      }
    }
    return false;
  }

  private ServiceBuilder newScribeBuilder(OAuth2IdentityProvider.OAuth2Context context) {
    checkState(isEnabled(), "GitHub authentication is disabled");
    return new ServiceBuilder(settings.clientId())
      .apiSecret(settings.clientSecret())
      .callback(context.getCallbackUrl());
  }

}
