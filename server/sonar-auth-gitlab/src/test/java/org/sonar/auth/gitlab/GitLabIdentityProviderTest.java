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

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(DataProviderRunner.class)
public class GitLabIdentityProviderTest {

  private static final String OAUTH_CODE = "code fdsojfsjodfg";
  private static final String AUTHORIZATION_URL = "AUTHORIZATION_URL";
  private static final String CALLBACK_URL = "CALLBACK_URL";
  private static final String STATE = "State request";

  @Mock
  private GitLabRestClient gitLabRestClient;
  @Mock
  private GitLabSettings gitLabSettings;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private GitLabIdentityProvider.ScribeFactory scribeFactory;
  @Mock
  private OAuth2IdentityProvider.InitContext initContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private OAuth2IdentityProvider.CallbackContext callbackContext;
  @Mock
  private OAuth20Service scribe;
  @Mock
  private ScribeGitLabOauth2Api scribeApi;
  @Mock
  private OAuth2AccessToken accessToken;

  private GitLabIdentityProvider gitLabIdentityProvider;

  @Before
  public void setup() throws IOException, ExecutionException, InterruptedException {
    openMocks(this);
    gitLabIdentityProvider = new GitLabIdentityProvider(gitLabSettings, gitLabRestClient, scribeApi, scribeFactory);

    when(initContext.generateCsrfState()).thenReturn(STATE);
    when(initContext.getCallbackUrl()).thenReturn(CALLBACK_URL);

    when(callbackContext.getCallbackUrl()).thenReturn(CALLBACK_URL);
    when(callbackContext.getHttpRequest().getParameter(OAuthConstants.CODE)).thenReturn(OAUTH_CODE);

    when(scribeFactory.newScribe(gitLabSettings, CALLBACK_URL, scribeApi)).thenReturn(scribe);
    when(scribe.getAccessToken(OAUTH_CODE)).thenReturn(accessToken);
    when(scribe.getAuthorizationUrl(STATE)).thenReturn(AUTHORIZATION_URL);
  }

  @Test
  public void test_identity_provider() {
    when(gitLabSettings.isEnabled()).thenReturn(true);
    when(gitLabSettings.allowUsersToSignUp()).thenReturn(true);

    assertThat(gitLabIdentityProvider.getKey()).isEqualTo("gitlab");
    assertThat(gitLabIdentityProvider.getName()).isEqualTo("GitLab");
    Display display = gitLabIdentityProvider.getDisplay();
    assertThat(display.getIconPath()).isEqualTo("/images/alm/gitlab.svg");
    assertThat(display.getBackgroundColor()).isEqualTo("#6a4fbb");
    assertThat(gitLabIdentityProvider.isEnabled()).isTrue();
    assertThat(gitLabIdentityProvider.allowsUsersToSignUp()).isTrue();
  }

  @Test
  public void init_whenSuccessful_redirectsToUrl() {
    gitLabIdentityProvider.init(initContext);

    verify(initContext).generateCsrfState();
    verify(initContext).redirectTo(AUTHORIZATION_URL);
  }

  @Test
  public void init_whenErrorWhileBuildingScribe_shouldReThrow() {
    IllegalStateException exception = new IllegalStateException("GitLab authentication is disabled");
    when(scribeFactory.newScribe(any(), any(), any())).thenThrow(exception);
    when(initContext.getCallbackUrl()).thenReturn("http://server/callback");

    assertThatIllegalStateException()
      .isThrownBy(() -> gitLabIdentityProvider.init(initContext))
      .isEqualTo(exception);
  }

  @Test
  public void onCallback_withGroupSyncDisabledAndNoAllowedGroups_redirectsToRequestedPage() {
    GsonUser gsonUser = mockGsonUser();

    gitLabIdentityProvider.callback(callbackContext);

    verifyAuthenticateIsCalledWithExpectedIdentity(callbackContext, gsonUser, Set.of());
    verify(callbackContext).redirectToRequestedPage();
    verify(gitLabRestClient, never()).getGroups(any(), any());
  }

  @Test
  public void onCallback_withGroupSyncDisabledAndAllowedGroups_redirectsToRequestedPage() {
    when(gitLabSettings.syncUserGroups()).thenReturn(false);

    GsonUser gsonUser = mockGsonUser();

    gitLabIdentityProvider.callback(callbackContext);

    verifyAuthenticateIsCalledWithExpectedIdentity(callbackContext, gsonUser, Set.of());
    verify(callbackContext).redirectToRequestedPage();
    verify(gitLabRestClient, never()).getGroups(any(), any());
  }

  @Test
  @UseDataProvider("allowedGroups")
  public void onCallback_withGroupSyncAndAllowedGroupsMatching_redirectsToRequestedPage(Set<String> allowedGroups) {
    when(gitLabSettings.syncUserGroups()).thenReturn(true);
    when(gitLabSettings.allowedGroups()).thenReturn(allowedGroups);

    GsonUser gsonUser = mockGsonUser();
    Set<GsonGroup> gsonGroups = mockGitlabGroups();

    gitLabIdentityProvider.callback(callbackContext);

    verifyAuthenticateIsCalledWithExpectedIdentity(callbackContext, gsonUser, gsonGroups);
    verify(callbackContext).redirectToRequestedPage();
  }

  @DataProvider
  public static Object[][] allowedGroups() {
    return new Object[][]{
      {Set.of()},
      {Set.of("path")}
    };
  }

  @Test
  public void onCallback_withGroupSyncAndAllowedGroupsNotMatching_shouldThrow() {
    when(gitLabSettings.syncUserGroups()).thenReturn(true);
    when(gitLabSettings.allowedGroups()).thenReturn(Set.of("path2"));

    mockGsonUser();
    mockGitlabGroups();

    assertThatExceptionOfType(UnauthorizedException.class)
      .isThrownBy(() -> gitLabIdentityProvider.callback(callbackContext))
      .withMessage("You are not allowed to authenticate");
  }

  @Test
  public void onCallback_ifScribeFactoryFails_shouldThrow() {
    IllegalStateException exception = new IllegalStateException("message");
    when(scribeFactory.newScribe(any(), any(), any())).thenThrow(exception);

    assertThatIllegalStateException()
      .isThrownBy(() -> gitLabIdentityProvider.callback(callbackContext))
      .isEqualTo(exception);
  }

  private Set<GsonGroup> mockGitlabGroups() {
    GsonGroup gsonGroup = mock(GsonGroup.class);
    when(gsonGroup.getFullPath()).thenReturn("path/to/group");
    GsonGroup gsonGroup2 = mock(GsonGroup.class);
    when(gsonGroup2.getFullPath()).thenReturn("path/to/group2");
    when(gitLabRestClient.getGroups(scribe, accessToken)).thenReturn(List.of(gsonGroup, gsonGroup2));
    return Set.of(gsonGroup, gsonGroup2);
  }

  private static void verifyAuthenticateIsCalledWithExpectedIdentity(OAuth2IdentityProvider.CallbackContext callbackContext,
    GsonUser gsonUser, Set<GsonGroup> gsonGroups) {
    ArgumentCaptor<UserIdentity> userIdentityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
    verify(callbackContext).authenticate(userIdentityCaptor.capture());

    UserIdentity actualIdentity = userIdentityCaptor.getValue();

    assertThat(actualIdentity.getProviderId()).asLong().isEqualTo(gsonUser.getId());
    assertThat(actualIdentity.getProviderLogin()).isEqualTo(gsonUser.getUsername());
    assertThat(actualIdentity.getName()).isEqualTo(gsonUser.getName());
    assertThat(actualIdentity.getEmail()).isEqualTo(gsonUser.getEmail());
    assertThat(actualIdentity.getGroups()).isEqualTo(gsonGroups.stream().map(GsonGroup::getFullPath).collect(toSet()));
  }

  private GsonUser mockGsonUser() {
    GsonUser gsonUser = mock();
    when(gsonUser.getId()).thenReturn(432423L);
    when(gsonUser.getUsername()).thenReturn("userName");
    when(gsonUser.getName()).thenReturn("name");
    when(gsonUser.getEmail()).thenReturn("toto@gitlab.com");
    when(gitLabRestClient.getUser(scribe, accessToken)).thenReturn(gsonUser);
    return gsonUser;
  }

  @Test
  public void newScribe_whenGitLabAuthIsDisabled_throws() {
    when(gitLabSettings.isEnabled()).thenReturn(false);

    assertThatIllegalStateException()
      .isThrownBy(() -> new GitLabIdentityProvider.ScribeFactory().newScribe(gitLabSettings, CALLBACK_URL, new ScribeGitLabOauth2Api(gitLabSettings)))
      .withMessage("GitLab authentication is disabled");
  }

  @Test
  @UseDataProvider("groupsSyncToScope")
  public void newScribe_whenGitLabSettingsValid_shouldUseCorrectScopeDependingOnGroupSync(boolean groupSyncEnabled, String expectedScope) {
    setupGitlabSettingsWithGroupSync(groupSyncEnabled);


    OAuth20Service realScribe = new GitLabIdentityProvider.ScribeFactory().newScribe(gitLabSettings, CALLBACK_URL,
      new ScribeGitLabOauth2Api(gitLabSettings));

    assertThat(realScribe).isNotNull();
    assertThat(realScribe.getCallback()).isEqualTo(CALLBACK_URL);
    assertThat(realScribe.getApiSecret()).isEqualTo(gitLabSettings.secret());
    assertThat(realScribe.getDefaultScope()).isEqualTo(expectedScope);
  }

  @DataProvider
  public static Object[][] groupsSyncToScope() {
    return new Object[][]{
      {false, "read_user"},
      {true, "api"}
    };
  }

  private void setupGitlabSettingsWithGroupSync(boolean enableGroupSync) {
    when(gitLabSettings.isEnabled()).thenReturn(true);
    when(gitLabSettings.applicationId()).thenReturn("123");
    when(gitLabSettings.secret()).thenReturn("456");
    when(gitLabSettings.syncUserGroups()).thenReturn(enableGroupSync);
  }
}
