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
package org.sonar.auth.gitlab;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitLabIdentityProviderTest {

  @Test
  public void test_identity_provider() {
    GitLabSettings gitLabSettings = mock(GitLabSettings.class);
    when(gitLabSettings.isEnabled()).thenReturn(true);
    when(gitLabSettings.allowUsersToSignUp()).thenReturn(true);
    GitLabIdentityProvider gitLabIdentityProvider = new GitLabIdentityProvider(gitLabSettings, new GitLabRestClient(gitLabSettings),
      new ScribeGitLabOauth2Api(gitLabSettings));

    assertThat(gitLabIdentityProvider.getKey()).isEqualTo("gitlab");
    assertThat(gitLabIdentityProvider.getName()).isEqualTo("GitLab");
    Display display = gitLabIdentityProvider.getDisplay();
    assertThat(display.getIconPath()).isEqualTo("/images/gitlab-icon-rgb.svg");
    assertThat(display.getBackgroundColor()).isEqualTo("#6a4fbb");
    assertThat(gitLabIdentityProvider.isEnabled()).isTrue();
    assertThat(gitLabIdentityProvider.allowsUsersToSignUp()).isTrue();
  }

  @Test
  public void test_init() {
    GitLabSettings gitLabSettings = mock(GitLabSettings.class);
    when(gitLabSettings.isEnabled()).thenReturn(true);
    when(gitLabSettings.allowUsersToSignUp()).thenReturn(true);
    when(gitLabSettings.applicationId()).thenReturn("123");
    when(gitLabSettings.secret()).thenReturn("456");
    when(gitLabSettings.url()).thenReturn("http://server");
    when(gitLabSettings.syncUserGroups()).thenReturn(true);
    GitLabIdentityProvider gitLabIdentityProvider = new GitLabIdentityProvider(gitLabSettings, new GitLabRestClient(gitLabSettings),
      new ScribeGitLabOauth2Api(gitLabSettings));

    OAuth2IdentityProvider.InitContext initContext = mock(OAuth2IdentityProvider.InitContext.class);
    when(initContext.getCallbackUrl()).thenReturn("http://server/callback");

    gitLabIdentityProvider.init(initContext);

    verify(initContext).redirectTo("http://server/oauth/authorize?response_type=code&client_id=123&redirect_uri=http%3A%2F%2Fserver%2Fcallback&scope=api");
  }

  @Test
  public void test_init_without_sync() {
    GitLabSettings gitLabSettings = mock(GitLabSettings.class);
    when(gitLabSettings.isEnabled()).thenReturn(true);
    when(gitLabSettings.allowUsersToSignUp()).thenReturn(true);
    when(gitLabSettings.applicationId()).thenReturn("123");
    when(gitLabSettings.secret()).thenReturn("456");
    when(gitLabSettings.url()).thenReturn("http://server");
    when(gitLabSettings.syncUserGroups()).thenReturn(false);
    GitLabIdentityProvider gitLabIdentityProvider = new GitLabIdentityProvider(gitLabSettings, new GitLabRestClient(gitLabSettings),
      new ScribeGitLabOauth2Api(gitLabSettings));

    OAuth2IdentityProvider.InitContext initContext = mock(OAuth2IdentityProvider.InitContext.class);
    when(initContext.getCallbackUrl()).thenReturn("http://server/callback");

    gitLabIdentityProvider.init(initContext);

    verify(initContext).redirectTo("http://server/oauth/authorize?response_type=code&client_id=123&redirect_uri=http%3A%2F%2Fserver%2Fcallback&scope=read_user");
  }

  @Test
  public void fail_to_init() {
    GitLabSettings gitLabSettings = mock(GitLabSettings.class);
    when(gitLabSettings.isEnabled()).thenReturn(false);
    when(gitLabSettings.allowUsersToSignUp()).thenReturn(true);
    when(gitLabSettings.applicationId()).thenReturn("123");
    when(gitLabSettings.secret()).thenReturn("456");
    when(gitLabSettings.url()).thenReturn("http://server");
    GitLabIdentityProvider gitLabIdentityProvider = new GitLabIdentityProvider(gitLabSettings, new GitLabRestClient(gitLabSettings),
      new ScribeGitLabOauth2Api(gitLabSettings));

    OAuth2IdentityProvider.InitContext initContext = mock(OAuth2IdentityProvider.InitContext.class);
    when(initContext.getCallbackUrl()).thenReturn("http://server/callback");

    Assertions.assertThatThrownBy(() -> gitLabIdentityProvider.init(initContext))
      .hasMessage("GitLab authentication is disabled")
      .isInstanceOf(IllegalStateException.class);
  }
}
