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

import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonarsource.onboarding.server.ProvisioningPermissionPort;
import org.sonarsource.onboarding.server.db.OnboardingSecretDecryptor;
import org.sonarsource.onboarding.shared.ciconfiguration.port.GitHubReadTokenPort;
import org.sonarsource.onboarding.shared.ciconfiguration.port.ServerUrlPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

/**
 * Registers the optional host beans of the onboarding capability, adapting the platform settings and the user session.
 */
@Configuration
public class OnboardingHostConfiguration {

  /**
   * Reads {@code sonar.core.serverBaseURL} as configured, without the computed fallback applied by the platform URL
   * settings.
   */
  @Bean
  public ServerUrlPort onboardingServerUrlPort(org.sonar.api.config.Configuration config) {
    return scope -> config.get(CoreProperties.SERVER_BASE_URL)
      .map(String::trim)
      .filter(url -> !url.isEmpty());
  }

  @Bean
  public ProvisioningPermissionPort onboardingProvisioningPermissionPort(UserSession userSession) {
    return () -> userSession.hasPermission(PROVISION_PROJECTS);
  }

  @Bean
  public OnboardingSecretDecryptor onboardingSecretDecryptor(Settings settings) {
    return value -> {
      Encryption encryption = settings.getEncryption();
      return value != null && encryption.isEncrypted(value) ? encryption.decrypt(value) : value;
    };
  }

  @Bean
  public GitHubReadTokenPort onboardingGitHubReadTokenPort(DbClient dbClient, GithubGlobalSettingsValidator settingsValidator,
    GithubApplicationClient githubClient) {
    return new GitHubInstallationReadTokenPort(dbClient, settingsValidator, githubClient);
  }
}
