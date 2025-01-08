/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.auth.github.scribe.ScribeServiceBuilder;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static org.sonar.auth.github.GitHubSettings.DEFAULT_API_URL;

public class GitHubIdentityProvider implements OAuth2IdentityProvider {

  public static final String KEY = "github";

  private final GitHubSettings settings;
  private final UserIdentityFactory userIdentityFactory;
  private final ScribeGitHubApi scribeApi;
  private final GitHubRestClient gitHubRestClient;
  private final GithubApplicationClient githubAppClient;
  private final ScribeServiceBuilder scribeServiceBuilder;

  public GitHubIdentityProvider(GitHubSettings settings, UserIdentityFactory userIdentityFactory, ScribeGitHubApi scribeApi, GitHubRestClient gitHubRestClient,
    GithubApplicationClient githubAppClient, ScribeServiceBuilder scribeServiceBuilder) {
    this.settings = settings;
    this.userIdentityFactory = userIdentityFactory;
    this.scribeApi = scribeApi;
    this.gitHubRestClient = gitHubRestClient;
    this.githubAppClient = githubAppClient;
    this.scribeServiceBuilder = scribeServiceBuilder;
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
      .setIconPath("/images/alm/github.svg")
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

    HttpRequest request = context.getHttpRequest();
    OAuth20Service scribe = scribeServiceBuilder.buildScribeService(settings.clientId(), settings.clientSecret(), context.getCallbackUrl(), scribeApi);

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

  private void check(OAuth20Service scribe, OAuth2AccessToken accessToken, GsonUser user) throws InterruptedException, ExecutionException, IOException {
    if (!isUserAuthorized(scribe, accessToken, user.getLogin())) {
      String message = settings.getOrganizations().isEmpty()
        ? format("'%s' must be a member of at least one organization which has installed the SonarQube GitHub app", user.getLogin())
        : format("'%s' must be a member of at least one organization: '%s'", user.getLogin(), String.join("', '", settings.getOrganizations().stream().sorted().toList()));
      throw new UnauthorizedException(message);
    }
  }

  private boolean isUserAuthorized(OAuth20Service scribe, OAuth2AccessToken accessToken, String login) throws IOException, ExecutionException, InterruptedException {
    if (isOrganizationMembershipRequired()) {
      return isOrganizationsMember(scribe, accessToken, login);
    } else {
      return isMemberOfInstallationOrganization(scribe, accessToken, login);
    }
  }

  private boolean isOrganizationMembershipRequired() {
    return !settings.getOrganizations().isEmpty();
  }

  private boolean isOrganizationsMember(OAuth20Service scribe, OAuth2AccessToken accessToken, String login) throws IOException, ExecutionException, InterruptedException {
    for (String organization : settings.getOrganizations()) {
      if (gitHubRestClient.isOrganizationMember(scribe, accessToken, organization, login)) {
        return true;
      }
    }
    return false;
  }

  private boolean isMemberOfInstallationOrganization(OAuth20Service scribe, OAuth2AccessToken accessToken, String login)
    throws IOException, ExecutionException, InterruptedException {
    GithubAppConfiguration githubAppConfiguration = githubAppConfiguration();
    List<GithubAppInstallation> githubAppInstallations = githubAppClient.getWhitelistedGithubAppInstallations(githubAppConfiguration);
    for (GithubAppInstallation installation : githubAppInstallations) {
      if (gitHubRestClient.isOrganizationMember(scribe, accessToken, installation.organizationName(), login)) {
        return true;
      }
    }
    return false;
  }

  private GithubAppConfiguration githubAppConfiguration() {
    String apiEndpoint = Optional.ofNullable(settings.apiURL()).orElse(DEFAULT_API_URL);
    try {
      return new GithubAppConfiguration(parseLong(settings.appId()), settings.privateKey(), apiEndpoint);
    } catch (NumberFormatException numberFormatException) {
      throw new IllegalStateException("Github configuration is not complete. Please check your configuration under the Authentication > GitHub tab");
    }
  }

  private ServiceBuilder newScribeBuilder(OAuth2IdentityProvider.OAuth2Context context) {
    checkState(isEnabled(), "GitHub authentication is disabled");
    return new ServiceBuilder(settings.clientId())
      .apiSecret(settings.clientSecret())
      .callback(context.getCallbackUrl());
  }

}
