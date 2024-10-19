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
package org.sonar.auth.gitlab;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOWED_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_APPLICATION_ID;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SECRET;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SYNC_USER_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;

public class IntegrationTest {

  private static final String ANY_CODE_VALUE = "ANY_CODE";

  @Rule
  public MockWebServer gitlab = new MockWebServer();

  private final MapSettings mapSettings = new MapSettings();

  private final GitLabSettings gitLabSettings = new GitLabSettings(mapSettings.asConfig());

  private String gitLabUrl;

  private final GitLabIdentityProvider gitLabIdentityProvider = new GitLabIdentityProvider(gitLabSettings,
    new GitLabRestClient(gitLabSettings),
    new ScribeGitLabOauth2Api(gitLabSettings));

  @Before
  public void setUp() {
    this.gitLabUrl = format("http://%s:%d", gitlab.getHostName(), gitlab.getPort());
    mapSettings
      .setProperty(GITLAB_AUTH_ENABLED, "true")
      .setProperty(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, "true")
      .setProperty(GITLAB_AUTH_URL, gitLabUrl)
      .setProperty(GITLAB_AUTH_APPLICATION_ID, "123")
      .setProperty(GITLAB_AUTH_SECRET, "456")
      .setProperty(GITLAB_AUTH_ALLOWED_GROUPS, "group1,group2")
      .setProperty(GITLAB_AUTH_SYNC_USER_GROUPS, "true");
  }

  @Test
  public void callback_whenAllowedUser_shouldAuthenticate() {
    OAuth2IdentityProvider.CallbackContext callbackContext = mockCallbackContext();

    mockAccessTokenResponse();
    mockUserResponse();
    mockSingleGroupReponse("group1");

    gitLabIdentityProvider.callback(callbackContext);

    ArgumentCaptor<UserIdentity> argument = ArgumentCaptor.forClass(UserIdentity.class);
    verify(callbackContext).authenticate(argument.capture());
    assertThat(argument.getValue()).isNotNull();
    assertThat(argument.getValue().getProviderId()).isEqualTo("123");
    assertThat(argument.getValue().getProviderLogin()).isEqualTo("toto");
    assertThat(argument.getValue().getName()).isEqualTo("Toto Toto");
    assertThat(argument.getValue().getEmail()).isEqualTo("toto@toto.com");
    verify(callbackContext).redirectToRequestedPage();
  }

  @Test
  public void callback_whenGroupNotAllowedAndGroupSyncEnabled_shouldThrow() {
    OAuth2IdentityProvider.CallbackContext callbackContext = mockCallbackContext();

    mockAccessTokenResponse();
    mockUserResponse();
    mockSingleGroupReponse("wrong-group");

    assertThatThrownBy(() -> gitLabIdentityProvider.callback(callbackContext))
      .isInstanceOf((UnauthorizedException.class))
      .hasMessage("You are not allowed to authenticate");
  }

  @Test
  public void callback_whenGroupNotAllowedAndGroupSyncDisabled_shouldThrow() {
    mapSettings.setProperty(GITLAB_AUTH_SYNC_USER_GROUPS, "false");
    OAuth2IdentityProvider.CallbackContext callbackContext = mockCallbackContext();

    mockAccessTokenResponse();
    mockUserResponse();
    mockSingleGroupReponse("wrong-group");

    gitLabIdentityProvider.callback(callbackContext);

    verify(callbackContext).authenticate(any());
    verify(callbackContext).redirectToRequestedPage();
  }

  @Test
  public void callback_whenAllowedUserBySubgroupMembership_shouldAuthenticate() {
    OAuth2IdentityProvider.CallbackContext callbackContext = mockCallbackContext();

    mockAccessTokenResponse();
    mockUserResponse();
    mockSingleGroupReponse("group1/subgroup");

    gitLabIdentityProvider.callback(callbackContext);

    verify(callbackContext).authenticate(any());
    verify(callbackContext).redirectToRequestedPage();
  }


  @Test
  public void callback_shouldSynchronizeGroups() throws InterruptedException {
    mapSettings.setProperty(GITLAB_AUTH_SYNC_USER_GROUPS, "true");
    OAuth2IdentityProvider.CallbackContext callbackContext = mockCallbackContext();

    mockAccessTokenResponse();
    mockUserResponse();
    // Response for /groups
    gitlab.enqueue(new MockResponse().setBody("""
      [
        {
          "id": 1,
          "full_path": "group1"
        },
        {
          "id": 2,
          "full_path": "group2"
        }
      ]
      """));

    gitLabIdentityProvider.callback(callbackContext);

    ArgumentCaptor<UserIdentity> captor = ArgumentCaptor.forClass(UserIdentity.class);
    verify(callbackContext).authenticate(captor.capture());
    UserIdentity value = captor.getValue();
    assertThat(value.getGroups()).contains("group1", "group2");
    assertThat(gitlab.takeRequest().getPath()).isEqualTo("/oauth/token");
    assertThat(gitlab.takeRequest().getPath()).isEqualTo("/api/v4/user");
    assertThat(gitlab.takeRequest().getPath()).isEqualTo("/api/v4/groups?min_access_level=10&per_page=100");
  }

  @Test
  public void callback_whenMultiplePagesOfGroups_shouldSynchronizeAllGroups() {
    mapSettings.setProperty(GITLAB_AUTH_SYNC_USER_GROUPS, "true");
    OAuth2IdentityProvider.CallbackContext callbackContext = mockCallbackContext();

    mockAccessTokenResponse();
    mockUserResponse();
    // Response for /groups, first page
    gitlab.enqueue(new MockResponse()
      .setBody("""
        [
          {
            "id": 1,
            "full_path": "group1"
          },
          {
            "id": 2,
            "full_path": "group2"
          }
        ]
        """)
      .setHeader("Link", format(" <%s/groups?per_page=100&page=2>; rel=\"next\"," +
        "  <%s/groups?per_page=100&&page=3>; rel=\"last\"," +
        "  <%s/groups?per_page=100&&page=1>; rel=\"first\"", gitLabUrl, gitLabUrl, gitLabUrl)));
    // Response for /groups, page 2
    gitlab.enqueue(new MockResponse()
      .setBody("""
        [
          {
            "id": 3,
            "full_path": "group3"
          },
          {
            "id": 4,
            "full_path": "group4"
          }
        ]
        """)
      .setHeader("Link", format("<%s/groups?per_page=100&page=3>; rel=\"next\"," +
        "  <%s/groups?per_page=100&&page=3>; rel=\"last\"," +
        "  <%s/groups?per_page=100&&page=1>; rel=\"first\"", gitLabUrl, gitLabUrl, gitLabUrl)));
    // Response for /groups, page 3
    gitlab.enqueue(new MockResponse()
      .setBody("""
        [
          {
            "id": 5,
            "full_path": "group5"
          },
          {
            "id": 6,
            "full_path": "group6"
          }
        ]
        """)
      .setHeader("Link", format("<%s/groups?per_page=100&&page=3>; rel=\"last\"," +
        "  <%s/groups?per_page=100&&page=1>; rel=\"first\"", gitLabUrl, gitLabUrl)));

    gitLabIdentityProvider.callback(callbackContext);

    ArgumentCaptor<UserIdentity> captor = ArgumentCaptor.forClass(UserIdentity.class);
    verify(callbackContext).authenticate(captor.capture());
    UserIdentity value = captor.getValue();
    assertThat(value.getGroups()).contains("group1", "group2", "group3", "group4", "group5", "group6");
  }

  @Test
  public void callback_whenNoUser_shouldThrow() {
    OAuth2IdentityProvider.CallbackContext callbackContext = mockCallbackContext();

    mockAccessTokenResponse();
    // Response for /user
    gitlab.enqueue(new MockResponse().setResponseCode(404).setBody("empty"));

    assertThatThrownBy(() -> gitLabIdentityProvider.callback(callbackContext))
      .hasMessage("Fail to execute request '" + gitLabSettings.url() + "/api/v4/user'. HTTP code: 404, response: empty")
      .isInstanceOf((IllegalStateException.class));
  }

  private static OAuth2IdentityProvider.CallbackContext mockCallbackContext() {
    OAuth2IdentityProvider.CallbackContext callbackContext = Mockito.mock(OAuth2IdentityProvider.CallbackContext.class);
    when(callbackContext.getCallbackUrl()).thenReturn("http://server/callback");

    HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
    when(httpRequest.getParameter("code")).thenReturn(ANY_CODE_VALUE);
    when(callbackContext.getHttpRequest()).thenReturn(httpRequest);
    return callbackContext;
  }

  private void mockAccessTokenResponse() {
    // Response for OAuth access token
    gitlab.enqueue(new MockResponse().setBody("""
      {
        "access_token": "de6780bc506a0446309bd9362820ba8aed28aa506c71eedbe1c5c4f9dd350e54",
        "token_type": "bearer",
        "expires_in": 7200,
        "refresh_token": "8257e65c97202ed1726cf9571600918f3bffb2544b26e00a61df9897668c33a1"
      }
      """));
  }

  private void mockUserResponse() {
    // Response for /user
    gitlab.enqueue(new MockResponse().setBody("""
      {
        "id": 123,
        "username": "toto",
        "name": "Toto Toto",
        "email": "toto@toto.com"
      }
      """));
  }

  private void mockSingleGroupReponse(String group) {
    // Response for /groups
    gitlab.enqueue(new MockResponse().setBody("""
      [
        {
          "id": 1,
          "full_path": "%s"
        }
      ]
      """.formatted(group)));
  }

}
