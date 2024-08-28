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
package org.sonar.auth.github;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoRefreshableAppTokenTest {

  @Mock
  private AppInstallationTokenGenerator appInstallationTokenGenerator;
  @Mock
  private GithubAppInstallation githubAppInstallation;

  @Test
  void getValue_ifTokenNull_shouldCreateAndDelegate() {
    ExpiringAppInstallationToken token = mockValidToken();

    AutoRefreshableAppToken autoRefreshableAppToken = new AutoRefreshableAppToken(appInstallationTokenGenerator, githubAppInstallation);
    String value = autoRefreshableAppToken.getValue();

    verify(appInstallationTokenGenerator).getAppInstallationToken(githubAppInstallation);
    assertThat(value).isEqualTo(token.getValue());
  }

  @Test
  void getValue_shouldCacheToken() {
    ExpiringAppInstallationToken token = mockValidToken();

    AutoRefreshableAppToken autoRefreshableAppToken = new AutoRefreshableAppToken(appInstallationTokenGenerator, githubAppInstallation);
    autoRefreshableAppToken.getValue();

    String value = autoRefreshableAppToken.getValue();

    verify(appInstallationTokenGenerator).getAppInstallationToken(githubAppInstallation);
    assertThat(value).isEqualTo(token.getValue());
  }

  @Test
  void getValue_ifTokenExpired_shouldRenewToken() {
    mockExpiredToken();

    AutoRefreshableAppToken autoRefreshableAppToken = new AutoRefreshableAppToken(appInstallationTokenGenerator, githubAppInstallation);
    autoRefreshableAppToken.getValue();

    autoRefreshableAppToken.getValue();

    verify(appInstallationTokenGenerator, times(2)).getAppInstallationToken(githubAppInstallation);
  }

  @Test
  void getAuthorizationHeaderPrefix_ifTokenNull_shouldCreateAndDelegate() {
    ExpiringAppInstallationToken token = mockValidToken();

    AutoRefreshableAppToken autoRefreshableAppToken = new AutoRefreshableAppToken(appInstallationTokenGenerator, githubAppInstallation);
    String value = autoRefreshableAppToken.getAuthorizationHeaderPrefix();

    verify(appInstallationTokenGenerator).getAppInstallationToken(githubAppInstallation);
    assertThat(value).isEqualTo(token.getAuthorizationHeaderPrefix());
  }

  private ExpiringAppInstallationToken mockValidToken() {
    return mockToken(false);
  }

  private ExpiringAppInstallationToken mockExpiredToken() {
    return mockToken(true);
  }

  private ExpiringAppInstallationToken mockToken(boolean expired) {
    ExpiringAppInstallationToken token = mock();
    lenient().when(token.isExpired()).thenReturn(expired);
    lenient().when(token.getAuthorizationHeaderPrefix()).thenReturn("header");
    lenient().when(token.getValue()).thenReturn("bla");
    when(appInstallationTokenGenerator.getAppInstallationToken(githubAppInstallation)).thenReturn(token);
    return token;
  }
}
