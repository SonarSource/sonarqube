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
package org.sonar.auth.github;

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppInstallationTokenGeneratorTest {
  private static final String ORG_NAME = "ORG_NAME";
  private static final String INSTALLATION_ID = "1234";

  @Mock
  private GithubAppConfiguration githubAppConfiguration;
  @Mock
  private GithubApplicationClient githubApp;
  @Mock
  private GithubAppInstallation githubAppInstallation;

  @InjectMocks
  private AppInstallationTokenGenerator appInstallationTokenGenerator;

  @Test
  public void getAppInstallationToken_whenTokenGeneratedByGithubApp_returnsIt() {
    ExpiringAppInstallationToken appInstallationToken = mock();
    mockTokenCreation(appInstallationToken);

    AppInstallationToken appInstallationTokenFromGenerator = appInstallationTokenGenerator.getAppInstallationToken(githubAppInstallation);

    assertThat(appInstallationTokenFromGenerator).isSameAs(appInstallationToken);
  }

  @Test
  public void getAppInstallationToken_whenTokenNotGeneratedByGithubApp_throws() {
    mockTokenCreation(null);

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> appInstallationTokenGenerator.getAppInstallationToken(githubAppInstallation))
      .withMessage("Error while generating token for GitHub app installed in organization ORG_NAME");
  }

  private void mockTokenCreation(@Nullable ExpiringAppInstallationToken appInstallationToken) {
    when(githubAppInstallation.organizationName()).thenReturn(ORG_NAME);
    when(githubAppInstallation.installationId()).thenReturn(INSTALLATION_ID);
    when(githubApp.createAppInstallationToken(githubAppConfiguration, Long.parseLong(INSTALLATION_ID))).thenReturn(Optional.ofNullable(appInstallationToken));
  }

}
