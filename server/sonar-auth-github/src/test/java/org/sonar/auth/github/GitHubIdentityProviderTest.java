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

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.auth.github.GithubBinding.Permissions;
import org.sonar.auth.github.scribe.ScribeServiceBuilder;
import org.sonar.db.DbClient;
import org.sonar.server.property.InternalProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.authentication.OAuth2IdentityProvider.CallbackContext;
import static org.sonar.api.server.authentication.OAuth2IdentityProvider.InitContext;
import static org.sonar.auth.github.GitHubSettings.GITHUB_APP_ID;
import static org.sonar.auth.github.GitHubSettings.GITHUB_CLIENT_ID;
import static org.sonar.auth.github.GitHubSettings.GITHUB_CLIENT_SECRET;
import static org.sonar.auth.github.GitHubSettings.GITHUB_ENABLED;
import static org.sonar.auth.github.GitHubSettings.GITHUB_ORGANIZATIONS;
import static org.sonar.auth.github.GitHubSettings.GITHUB_PRIVATE_KEY;

public class GitHubIdentityProviderTest {

  private MapSettings settings = new MapSettings();
  private InternalProperties internalProperties = mock(InternalProperties.class);
  private GitHubSettings gitHubSettings = new GitHubSettings(settings.asConfig(), internalProperties, mock(DbClient.class));
  private UserIdentityFactoryImpl userIdentityFactory = mock(UserIdentityFactoryImpl.class);
  private ScribeGitHubApi scribeApi = new ScribeGitHubApi(gitHubSettings);
  private GitHubRestClient gitHubRestClient = mock();
  private GithubApplicationClient githubAppClient = mock();

  private ScribeServiceBuilder scribeServiceBuilder = mock();
  private GitHubIdentityProvider underTest = new GitHubIdentityProvider(gitHubSettings, userIdentityFactory, scribeApi, gitHubRestClient, githubAppClient, scribeServiceBuilder);

  @Test
  public void check_fields() {
    assertThat(underTest.getKey()).isEqualTo("github");
    assertThat(underTest.getName()).isEqualTo("GitHub");
    assertThat(underTest.getDisplay().getIconPath()).isEqualTo("/images/alm/github.svg");
    assertThat(underTest.getDisplay().getBackgroundColor()).isEqualTo("#444444");
  }

  @Test
  public void is_enabled() {
    settings.setProperty("sonar.auth.github.clientId.secured", "id");
    settings.setProperty("sonar.auth.github.clientSecret.secured", "secret");
    settings.setProperty("sonar.auth.github.enabled", true);
    assertThat(underTest.isEnabled()).isTrue();

    settings.setProperty("sonar.auth.github.enabled", false);
    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void should_allow_users_to_signup() {
    assertThat(underTest.allowsUsersToSignUp()).as("default").isFalse();

    settings.setProperty("sonar.auth.github.allowUsersToSignUp", true);
    assertThat(underTest.allowsUsersToSignUp()).isTrue();
  }

  @Test
  public void init() {
    setSettings(true);
    InitContext context = mock(InitContext.class);
    when(context.generateCsrfState()).thenReturn("state");
    when(context.getCallbackUrl()).thenReturn("http://localhost/callback");
    settings.setProperty("sonar.auth.github.webUrl", "https://github.com/");

    underTest.init(context);

    verify(context).redirectTo("https://github.com/login/oauth/authorize" +
      "?response_type=code" +
      "&client_id=id" +
      "&redirect_uri=http%3A%2F%2Flocalhost%2Fcallback&scope=user%3Aemail" +
      "&state=state");
  }

  @Test
  public void init_when_group_sync() {
    setSettings(true);
    settings.setProperty("sonar.auth.github.groupsSync", "true");
    settings.setProperty("sonar.auth.github.webUrl", "https://github.com/");
    InitContext context = mock(InitContext.class);
    when(context.generateCsrfState()).thenReturn("state");
    when(context.getCallbackUrl()).thenReturn("http://localhost/callback");

    underTest.init(context);

    verify(context).redirectTo("https://github.com/login/oauth/authorize" +
      "?response_type=code" +
      "&client_id=id" +
      "&redirect_uri=http%3A%2F%2Flocalhost%2Fcallback&scope=user%3Aemail%2Cread%3Aorg" +
      "&state=state");
  }

  @Test
  public void init_when_organizations() {
    setSettings(true);
    settings.setProperty("sonar.auth.github.organizations", "example");
    settings.setProperty("sonar.auth.github.webUrl", "https://github.com/");
    InitContext context = mock(InitContext.class);
    when(context.generateCsrfState()).thenReturn("state");
    when(context.getCallbackUrl()).thenReturn("http://localhost/callback");

    underTest.init(context);

    verify(context).redirectTo("https://github.com/login/oauth/authorize" +
      "?response_type=code" +
      "&client_id=id" +
      "&redirect_uri=http%3A%2F%2Flocalhost%2Fcallback" +
      "&scope=user%3Aemail%2Cread%3Aorg" +
      "&state=state");
  }

  @Test
  public void fail_to_init_when_disabled() {
    setSettings(false);
    InitContext context = mock(InitContext.class);

    assertThatThrownBy(() -> underTest.init(context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("GitHub authentication is disabled");
  }

  @Test
  public void scope_includes_org_when_necessary() {
    setSettings(false);

    settings.setProperty("sonar.auth.github.groupsSync", false);
    settings.setProperty("sonar.auth.github.organizations", "");
    assertThat(underTest.getScope()).isEqualTo("user:email");

    settings.setProperty("sonar.auth.github.groupsSync", true);
    settings.setProperty("sonar.auth.github.organizations", "");
    assertThat(underTest.getScope()).isEqualTo("user:email,read:org");

    settings.setProperty("sonar.auth.github.groupsSync", false);
    settings.setProperty("sonar.auth.github.organizations", "example");
    assertThat(underTest.getScope()).isEqualTo("user:email,read:org");

    settings.setProperty("sonar.auth.github.groupsSync", true);
    settings.setProperty("sonar.auth.github.organizations", "example");
    assertThat(underTest.getScope()).isEqualTo("user:email,read:org");
  }

  @Test
  public void callback_whenOrganizationsAreDefinedAndUserBelongsToOne_shouldAuthenticateAndRedirect() throws IOException, ExecutionException, InterruptedException {
    UserIdentity userIdentity = mock(UserIdentity.class);
    CallbackContext context = mockUserBelongingToOrganization(userIdentity);

    settings.setProperty(GITHUB_ORGANIZATIONS, "organization1,organization2");
    underTest.callback(context);

    verify(context).authenticate(userIdentity);
    verify(context).redirectToRequestedPage();
  }

  @Test
  public void callback_whenOrganizationsAreDefinedAndDoesntBelongToOne_shouldThrow() throws IOException, ExecutionException, InterruptedException {
    UserIdentity userIdentity = mock(UserIdentity.class);
    CallbackContext context = mockUserNotBelongingToOrganization(userIdentity);

    settings.setProperty(GITHUB_ORGANIZATIONS, "organization1,organization2");

    assertThatThrownBy(() -> underTest.callback(context))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("'login' must be a member of at least one organization: 'organization1', 'organization2'");
  }

  @Test
  public void callback_whenOrganizationsAreNotDefinedAndUserBelongsToInstallationOrganization_shouldAuthenticateAndRedirect()
    throws IOException, ExecutionException, InterruptedException {
    UserIdentity userIdentity = mock(UserIdentity.class);
    CallbackContext context = mockUserBelongingToOrganization(userIdentity);

    mockInstallations();

    underTest.callback(context);

    verify(context).authenticate(userIdentity);
    verify(context).redirectToRequestedPage();
  }

  @Test
  public void callback_whenOrganizationsAreNotDefinedAndUserDoesntBelongToInstallationOrganization_shouldThrow() throws IOException, ExecutionException, InterruptedException {
    UserIdentity userIdentity = mock(UserIdentity.class);
    CallbackContext context = mockUserNotBelongingToOrganization(userIdentity);

    mockInstallations();

    assertThatThrownBy(() -> underTest.callback(context))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("'login' must be a member of at least one organization which has installed the SonarQube GitHub app");
  }

  private CallbackContext mockUserBelongingToOrganization(UserIdentity userIdentity) throws IOException, InterruptedException, ExecutionException {
    setSettings(true);
    CallbackContext context = mock();
    HttpRequest httpRequest = mock();
    OAuth20Service scribeService = mock();
    GsonUser user = new GsonUser("id", "login", "name", "email");

    OAuth2AccessToken accessToken = mockAccessToken(scribeService, context, httpRequest, user);

    when(gitHubRestClient.getUserOrganizations(scribeService, accessToken)).thenReturn(List.of(new GsonOrganization("organization2")));

    when(userIdentityFactory.create(user, "email", null)).thenReturn(userIdentity);
    return context;
  }

  private CallbackContext mockUserNotBelongingToOrganization(UserIdentity userIdentity) throws IOException, InterruptedException, ExecutionException {
    setSettings(true);
    CallbackContext context = mock();
    HttpRequest httpRequest = mock();
    OAuth20Service scribeService = mock();
    GsonUser user = new GsonUser("id", "login", "name", "email");

    OAuth2AccessToken accessToken = mockAccessToken(scribeService, context, httpRequest, user);

    when(gitHubRestClient.getUserOrganizations(scribeService, accessToken)).thenReturn(List.of(new GsonOrganization("organization3")));

    when(userIdentityFactory.create(user, "email", null)).thenReturn(userIdentity);
    return context;
  }

  private OAuth2AccessToken mockAccessToken(OAuth20Service scribeService, CallbackContext context, HttpRequest httpRequest, GsonUser user)
    throws IOException, InterruptedException, ExecutionException {
    String callbackUrl = "http://localhost/callback";
    when(scribeServiceBuilder.buildScribeService("id", "secret", callbackUrl, scribeApi)).thenReturn(scribeService);
    when(context.getHttpRequest()).thenReturn(httpRequest);
    when(context.getCallbackUrl()).thenReturn(callbackUrl);

    when(httpRequest.getParameter("code")).thenReturn("code");
    OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
    when(scribeService.getAccessToken("code")).thenReturn(accessToken);

    when(gitHubRestClient.getUser(scribeService, accessToken)).thenReturn(user);
    return accessToken;
  }

  private void mockInstallations() {
    when(githubAppClient.getWhitelistedGithubAppInstallations(any())).thenReturn(List.of(
      new GithubAppInstallation("1", "organization1", new Permissions(), false),
      new GithubAppInstallation("2", "organization2", new Permissions(), false)));
  }

  private void setSettings(boolean enabled) {
    if (enabled) {
      settings.setProperty(GITHUB_CLIENT_ID, "id");
      settings.setProperty(GITHUB_CLIENT_SECRET, "secret");
      settings.setProperty(GITHUB_ENABLED, true);
      settings.setProperty(GITHUB_APP_ID, "1");
      settings.setProperty(GITHUB_PRIVATE_KEY, "private");
    } else {
      settings.setProperty(GITHUB_ENABLED, false);
    }
  }
}
