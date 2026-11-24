/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.onboarding.host;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonarsource.onboarding.server.ProvisioningPermissionPort;
import org.sonarsource.onboarding.server.db.OnboardingSecretDecryptor;
import org.sonarsource.onboarding.shared.OnboardingScope;
import org.sonarsource.onboarding.shared.ciconfiguration.port.GitHubReadTokenPort;
import org.sonarsource.onboarding.shared.ciconfiguration.port.ServerUrlPort;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

class OnboardingHostConfigurationTest {

  private static final OnboardingScope SCOPE = OnboardingScope.of(null);

  private final OnboardingHostConfiguration underTest = new OnboardingHostConfiguration();

  @Test
  void configuredBaseUrl_whenBaseUrlSet_shouldReturnTrimmedValue() {
    MapSettings settings = new MapSettings().setProperty("sonar.core.serverBaseURL", "  https://sonar.example.com  ");
    ServerUrlPort port = underTest.onboardingServerUrlPort(settings.asConfig());

    assertThat(port.configuredBaseUrl(SCOPE)).contains("https://sonar.example.com");
  }

  @Test
  void configuredBaseUrl_whenBaseUrlUnset_shouldBeEmptyWithoutComputedFallback() {
    ServerUrlPort port = underTest.onboardingServerUrlPort(new MapSettings().asConfig());

    assertThat(port.configuredBaseUrl(SCOPE)).isEmpty();
  }

  @Test
  void configuredBaseUrl_whenBaseUrlBlank_shouldBeEmpty() {
    MapSettings settings = new MapSettings().setProperty("sonar.core.serverBaseURL", "   ");
    ServerUrlPort port = underTest.onboardingServerUrlPort(settings.asConfig());

    assertThat(port.configuredBaseUrl(SCOPE)).isEmpty();
  }

  @Test
  void currentUserCanProvisionProjects_whenUserHasPermission_shouldBeTrue() {
    UserSession userSession = mock(UserSession.class);
    when(userSession.hasPermission(PROVISION_PROJECTS)).thenReturn(true);
    ProvisioningPermissionPort port = underTest.onboardingProvisioningPermissionPort(userSession);

    assertThat(port.currentUserCanProvisionProjects()).isTrue();
  }

  @Test
  void currentUserCanProvisionProjects_whenUserLacksPermission_shouldBeFalse() {
    UserSession userSession = mock(UserSession.class);
    when(userSession.hasPermission(PROVISION_PROJECTS)).thenReturn(false);
    ProvisioningPermissionPort port = underTest.onboardingProvisioningPermissionPort(userSession);

    assertThat(port.currentUserCanProvisionProjects()).isFalse();
  }

  @Test
  void decrypt_whenValueIsEncrypted_shouldReturnClearText() {
    String encrypted = "{b64}" + Base64.getEncoder().encodeToString("secret-token".getBytes(UTF_8));
    OnboardingSecretDecryptor decryptor = underTest.onboardingSecretDecryptor(new MapSettings());

    assertThat(decryptor.decrypt(encrypted)).isEqualTo("secret-token");
  }

  @Test
  void decrypt_whenValueIsNotEncrypted_shouldReturnValueUnchanged() {
    OnboardingSecretDecryptor decryptor = underTest.onboardingSecretDecryptor(new MapSettings());

    assertThat(decryptor.decrypt("plain-token")).isEqualTo("plain-token");
  }

  @Test
  void decrypt_whenValueIsNull_shouldReturnNull() {
    OnboardingSecretDecryptor decryptor = underTest.onboardingSecretDecryptor(new MapSettings());

    assertThat(decryptor.decrypt(null)).isNull();
  }

  @Test
  void onboardingGitHubReadTokenPort_shouldReturnAPortThatAnswersEmptyForAnUnknownSetting() {
    DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
    GitHubReadTokenPort port = underTest.onboardingGitHubReadTokenPort(dbClient, mock(GithubGlobalSettingsValidator.class), mock(GithubApplicationClient.class));

    assertThat(port.readToken("unknown", "owner/repo")).isEmpty();
  }
}
