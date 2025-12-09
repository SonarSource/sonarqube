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
package org.sonar.alm.client.github.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubAppInstallation;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.auth.github.GithubBinding.Permissions;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GitHubSettings;
import org.sonarqube.ws.client.HttpException;

import static java.lang.Long.parseLong;
import static org.sonar.auth.github.GithubBinding.GsonApp;
import static org.sonar.alm.client.github.config.ConfigCheckResult.ApplicationStatus;
import static org.sonar.alm.client.github.config.ConfigCheckResult.ConfigStatus;
import static org.sonar.alm.client.github.config.ConfigCheckResult.InstallationStatus;

@ServerSide
@ComputeEngineSide
public class GithubProvisioningConfigValidator {

  private static final String ORG_MEMBERS_PERMISSION = "Organization permissions > Members (Read-only)";
  private static final String ORG_ADMIN_PERMISSION = "Organization permissions > Administration (Read-only)";
  private static final String ACCOUNT_EMAILS_PERMISSION = "Account permissions > Email addresses (Read-only)";
  private static final String REPO_ADMIN_PERMISSION = "Repository permissions > Administration (Read-only)";
  private static final String REPO_METADATA_PERMISSION = "Repository permissions > Metadata (Read-only)";
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
    } catch (IllegalArgumentException e) {
      return failedApplicationStatus(
        ConfigStatus.failed(e.getMessage()));
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
      return failedStatus(List.of(ACCOUNT_EMAILS_PERMISSION));
    }
    return ConfigStatus.SUCCESS;
  }

  private static ConfigStatus autoProvisioningAppConfigStatus(Permissions permissions) {
    List<String> missingPermissions = new ArrayList<>();
    if (permissions.getEmails() == null) {
      missingPermissions.add(ACCOUNT_EMAILS_PERMISSION);
    }
    checkCommonPermissions(permissions, missingPermissions);
    if (missingPermissions.isEmpty()) {
      return ConfigStatus.SUCCESS;
    }
    return failedStatus(missingPermissions);
  }

  private static ConfigStatus failedStatus(List<String> missingPermissions) {
    return ConfigStatus.failed("Missing GitHub permissions: " + String.join(", ", missingPermissions));
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
    List<String> missingPermissions = new ArrayList<>();
    checkCommonPermissions(permissions, missingPermissions);
    if (missingPermissions.isEmpty()) {
      return ConfigStatus.SUCCESS;
    }
    return failedStatus(missingPermissions);
  }

  private static void checkCommonPermissions(Permissions permissions, List<String> missingPermissions) {
    if (permissions.getMembers() == null) {
      missingPermissions.add(ORG_MEMBERS_PERMISSION);
    }
    if (permissions.getOrgAdministration() == null) {
      missingPermissions.add(ORG_ADMIN_PERMISSION);
    }
    if (permissions.getRepoAdministration() == null) {
      missingPermissions.add(REPO_ADMIN_PERMISSION);
    }
    if (permissions.getMetadata() == null) {
      missingPermissions.add(REPO_METADATA_PERMISSION);
    }
  }

}
