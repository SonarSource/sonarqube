/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.alm.client.github.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubBinding.Permissions;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GitHubSettings;
import org.sonarqube.ws.client.HttpException;

import static java.lang.Long.parseLong;
import static org.sonar.alm.client.github.GithubBinding.GsonApp;
import static org.sonar.alm.client.github.config.ConfigCheckResult.ApplicationStatus;
import static org.sonar.alm.client.github.config.ConfigCheckResult.ConfigStatus;
import static org.sonar.alm.client.github.config.ConfigCheckResult.InstallationStatus;

@ServerSide
@ComputeEngineSide
public class GithubProvisioningConfigValidator {

  private static final ConfigStatus APP_NOT_FOUND_STATUS = ConfigStatus.failed("Github App not found");
  private static final String MEMBERS_PERMISSION = "Organization permissions -> Members";

  private static final String EMAILS_PERMISSION = "Account permissions -> Email addresses";

  private static final ConfigStatus INVALID_APP_CONFIG_STATUS = ConfigStatus.failed("The GitHub App configuration is not complete.");
  private static final ConfigStatus INVALID_APP_ID_STATUS = ConfigStatus.failed("GitHub App ID must be a number.");
  private static final ConfigStatus SUSPENDED_INSTALLATION_STATUS = ConfigStatus.failed("Installation suspended");
  private static final ConfigStatus NO_INSTALLATION_FOUND_STATUS = ConfigStatus.failed(
    "The GitHub App is not installed on any organizations or the organization is not white-listed.");
  private static final ConfigCheckResult NO_INSTALLATIONS_RESULT = new ConfigCheckResult(
    new ApplicationStatus(
      NO_INSTALLATION_FOUND_STATUS,
      NO_INSTALLATION_FOUND_STATUS),
    List.of());

  private final GithubApplicationClient githubClient;
  private final GitHubSettings gitHubSettings;

  public GithubProvisioningConfigValidator(GithubApplicationClient githubClient, GitHubSettings gitHubSettings) {
    this.githubClient = githubClient;
    this.gitHubSettings = gitHubSettings;
  }

  public ConfigCheckResult checkConfig() {
    Optional<Long> appId = getAppId();
    if (appId.isEmpty()) {
      return failedApplicationStatus(INVALID_APP_ID_STATUS);
    }
    GithubAppConfiguration githubAppConfiguration = new GithubAppConfiguration(appId.get(), gitHubSettings.privateKey(), gitHubSettings.apiURLOrDefault());
    return checkConfig(githubAppConfiguration);
  }

  private Optional<Long> getAppId() {
    try {
      return Optional.of(parseLong(gitHubSettings.appId()));
    } catch (NumberFormatException numberFormatException) {
      return Optional.empty();
    }
  }

  public ConfigCheckResult checkConfig(GithubAppConfiguration githubAppConfiguration) {
    if (!githubAppConfiguration.isComplete()) {
      return failedApplicationStatus(INVALID_APP_CONFIG_STATUS);
    }

    try {
      GsonApp app = githubClient.getApp(githubAppConfiguration);
      return checkNonEmptyConfig(githubAppConfiguration, app);
    } catch (HttpException e) {
      return failedApplicationStatus(
        ConfigStatus.failed("Error response from GitHub: " + e.getMessage()));
    }
  }

  private static ConfigCheckResult failedApplicationStatus(ConfigStatus configStatus) {
    return new ConfigCheckResult(new ApplicationStatus(configStatus, configStatus), List.of());
  }

  private ConfigCheckResult checkNonEmptyConfig(GithubAppConfiguration githubAppConfiguration, GsonApp app) {
    ApplicationStatus appStatus = checkNonEmptyAppConfig(app);
    List<InstallationStatus> installations = checkInstallations(githubAppConfiguration, appStatus);
    if (installations.isEmpty()) {
      return NO_INSTALLATIONS_RESULT;
    }
    return new ConfigCheckResult(
      appStatus,
      installations);
  }

  private static ApplicationStatus checkNonEmptyAppConfig(GsonApp app) {
    return new ApplicationStatus(
      jitAppConfigStatus(app.getPermissions()),
      autoProvisioningAppConfigStatus(app.getPermissions()));
  }

  private static ConfigStatus jitAppConfigStatus(Permissions permissions) {
    if (permissions.getEmails() == null) {
      return failedStatus(List.of(EMAILS_PERMISSION));
    }
    return ConfigStatus.SUCCESS;
  }

  private static ConfigStatus autoProvisioningAppConfigStatus(Permissions permissions) {
    List<String> missingPermissions = new ArrayList<>();
    if (permissions.getEmails() == null) {
      missingPermissions.add(EMAILS_PERMISSION);
    }
    if (permissions.getMembers() == null) {
      missingPermissions.add(MEMBERS_PERMISSION);
    }
    if (missingPermissions.isEmpty()) {
      return ConfigStatus.SUCCESS;
    }
    return failedStatus(missingPermissions);
  }

  private static ConfigStatus failedStatus(List<String> missingPermissions) {
    return ConfigStatus.failed("Missing permissions: " + String.join(",", missingPermissions));
  }

  private List<InstallationStatus> checkInstallations(GithubAppConfiguration githubAppConfiguration, ApplicationStatus appStatus) {
    return githubClient.getWhitelistedGithubAppInstallations(githubAppConfiguration)
      .stream()
      .map(installation -> statusForInstallation(installation, appStatus))
      .toList();
  }

  private static InstallationStatus statusForInstallation(GithubAppInstallation installation, ApplicationStatus appStatus) {
    if (installation.isSuspended()) {
      return new InstallationStatus(installation.organizationName(), SUSPENDED_INSTALLATION_STATUS, SUSPENDED_INSTALLATION_STATUS);
    }
    return new InstallationStatus(
      installation.organizationName(),
      appStatus.jit(),
      autoProvisioningInstallationConfigStatus(installation.permissions()));
  }

  private static ConfigStatus autoProvisioningInstallationConfigStatus(Permissions permissions) {
    if (permissions.getMembers() == null) {
      return failedStatus(List.of(MEMBERS_PERMISSION));
    }
    return ConfigStatus.SUCCESS;
  }

}
