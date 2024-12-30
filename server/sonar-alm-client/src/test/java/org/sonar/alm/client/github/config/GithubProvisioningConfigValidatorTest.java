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
package org.sonar.alm.client.github.config;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubAppInstallation;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.auth.github.GithubBinding.GsonApp;
import org.sonar.auth.github.GithubBinding.Permissions;
import org.sonar.auth.github.GitHubSettings;
import org.sonarqube.ws.client.HttpException;

import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.github.config.ConfigCheckResult.ConfigStatus;
import static org.sonar.alm.client.github.config.ConfigCheckResult.InstallationStatus;

@RunWith(MockitoJUnitRunner.class)
public class GithubProvisioningConfigValidatorTest {

  private static final String SUCCESS_STATUS = "SUCCESS";
  private static final String GITHUB_CALL_FAILED = "Error response from GitHub: GitHub call failed.";
  private static final String APP_FETCHING_FAILED = "Exception while fetching the App.";
  private static final String INVALID_APP_ID_STATUS = "GitHub App ID must be a number.";
  private static final String INCOMPLETE_APP_CONFIG_STATUS = "The GitHub App configuration is not complete.";
  private static final String MISSING_EMAIL_PERMISSION = "Missing GitHub permissions: Account permissions > Email addresses (Read-only)";
  private static final String MISSING_ALL_AUTOPROVISIONNING_PERMISSIONS = "Missing GitHub permissions: Organization permissions > Members (Read-only), "
    + "Organization permissions > Administration (Read-only), "
    + "Repository permissions > Administration (Read-only), Repository permissions > Metadata (Read-only)";
  private static final String MISSING_ALL_PERMISSIONS = "Missing GitHub permissions: Account permissions > Email addresses (Read-only), "
    + "Organization permissions > Members (Read-only), Organization permissions > Administration (Read-only), "
    + "Repository permissions > Administration (Read-only), Repository permissions > Metadata (Read-only)";
  private static final String NO_INSTALLATIONS_STATUS = "The GitHub App is not installed on any organizations or the organization is not white-listed.";
  private static final String SUSPENDED_INSTALLATION = "Installation suspended";

  private static final ConfigStatus SUCCESS_CHECK = new ConfigStatus(SUCCESS_STATUS, null);
  public static final String APP_ID = "1";
  public static final String PRIVATE_KEY = "secret";
  public static final String URL = "url";
  @Mock
  private GithubApplicationClient githubClient;

  @Mock
  private GitHubSettings gitHubSettings;

  @InjectMocks
  private GithubProvisioningConfigValidator configValidator;

  @Test
  public void checkConfig_whenAppIdIsNull_shouldReturnFailedAppCheck() {
    when(gitHubSettings.appId()).thenReturn(null);

    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(INVALID_APP_ID_STATUS));
    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.failed(INVALID_APP_ID_STATUS));
    assertThat(checkResult.installations()).isEmpty();
  }

  @Test
  public void checkConfig_whenAppIdNotValid_shouldReturnFailedAppCheck() {
    when(gitHubSettings.appId()).thenReturn("not a number");

    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(INVALID_APP_ID_STATUS));
    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.failed(INVALID_APP_ID_STATUS));
    assertThat(checkResult.installations()).isEmpty();
  }

  @Test
  public void checkConfig_whenGithubAppConfigurationNotComplete_shouldReturnFailedAppCheck() {
    when(gitHubSettings.appId()).thenReturn(APP_ID);

    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(INCOMPLETE_APP_CONFIG_STATUS));
    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.failed(INCOMPLETE_APP_CONFIG_STATUS));
    assertThat(checkResult.installations()).isEmpty();
  }

  @Test
  public void checkConfig_whenHttpExceptionWhileFetchingTheApp_shouldReturnFailedAppCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);

    HttpException httpException = mock(HttpException.class);
    when(httpException.getMessage()).thenReturn("GitHub call failed.");

    when(githubClient.getApp(appConfigurationCaptor.capture())).thenThrow(httpException);

    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(GITHUB_CALL_FAILED));
    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.failed(GITHUB_CALL_FAILED));
    assertThat(checkResult.installations()).isEmpty();
  }

  @Test
  public void checkConfig_whenIllegalArgumentExceptionWhileFetchingTheApp_shouldReturnFailedAppCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);

    IllegalArgumentException illegalArgumentException = mock(IllegalArgumentException.class);
    when(illegalArgumentException.getMessage()).thenReturn("Exception while fetching the App.");

    when(githubClient.getApp(appConfigurationCaptor.capture())).thenThrow(illegalArgumentException);

    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(APP_FETCHING_FAILED));
    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.failed(APP_FETCHING_FAILED));
    assertThat(checkResult.installations()).isEmpty();
  }

  @Test
  public void checkConfig_whenAppDoesntHaveAnyPermissions_shouldReturnFailedAppJitCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);
    GsonApp githubApp = mockGithubApp(appConfigurationCaptor);

    when(githubApp.getPermissions()).thenReturn(Permissions.builder().build());
    mockOrganizationsWithoutPermissions(appConfigurationCaptor, "org1", "org2");

    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(MISSING_ALL_PERMISSIONS));
    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.failed(MISSING_EMAIL_PERMISSION));
    assertThat(checkResult.installations()).hasSize(2);
    assertThat(checkResult.installations())
      .extracting(InstallationStatus::jit, InstallationStatus::autoProvisioning)
      .containsExactly(
        tuple(ConfigStatus.failed(MISSING_EMAIL_PERMISSION), ConfigStatus.failed(MISSING_ALL_AUTOPROVISIONNING_PERMISSIONS)),
        tuple(ConfigStatus.failed(MISSING_EMAIL_PERMISSION), ConfigStatus.failed(MISSING_ALL_AUTOPROVISIONNING_PERMISSIONS)));
    verifyAppConfiguration(appConfigurationCaptor.getValue());

  }

  @Test
  public void checkConfig_whenAppDoesntHaveOrgMembersPermissions_shouldReturnFailedAppAutoProvisioningCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);

    GsonApp githubApp = mockGithubApp(appConfigurationCaptor);
    when(githubApp.getPermissions()).thenReturn(Permissions.builder().setEmails("read").build());
    mockOrganizations(appConfigurationCaptor, "org1", "org2");

    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.SUCCESS);
    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(MISSING_ALL_AUTOPROVISIONNING_PERMISSIONS));
    assertThat(checkResult.installations()).hasSize(2);
    verifyAppConfiguration(appConfigurationCaptor.getValue());
  }

  @Test
  public void checkConfig_whenNoInstallationsAreReturned_shouldReturnFailedAppAutoProvisioningCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);
    mockGithubAppWithValidConfig(appConfigurationCaptor);

    mockOrganizationsWithoutPermissions(appConfigurationCaptor);
    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.failed(NO_INSTALLATIONS_STATUS));
    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.failed(NO_INSTALLATIONS_STATUS));
    assertThat(checkResult.installations()).isEmpty();

    verifyAppConfiguration(appConfigurationCaptor.getValue());
  }

  @Test
  public void checkConfig_whenInstallationsDoesntHaveOrgMembersPermissions_shouldReturnFailedAppAutoProvisioningCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);
    mockGithubAppWithValidConfig(appConfigurationCaptor);

    mockOrganizationsWithoutPermissions(appConfigurationCaptor, "org1");
    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertSuccessfulAppConfig(checkResult);
    assertThat(checkResult.installations())
      .extracting(InstallationStatus::organization, InstallationStatus::autoProvisioning)
      .containsExactly(tuple("org1", ConfigStatus.failed(MISSING_ALL_AUTOPROVISIONNING_PERMISSIONS)));
    verifyAppConfiguration(appConfigurationCaptor.getValue());

  }

  @Test
  public void checkConfig_whenInstallationSuspended_shouldReturnFailedInstallationAutoProvisioningCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);
    mockGithubAppWithValidConfig(appConfigurationCaptor);

    mockSuspendedOrganizations("org1");
    ConfigCheckResult checkResult = configValidator.checkConfig();

    assertSuccessfulAppConfig(checkResult);
    assertThat(checkResult.installations())
      .extracting(InstallationStatus::organization, InstallationStatus::autoProvisioning)
      .containsExactly(tuple("org1", ConfigStatus.failed(SUSPENDED_INSTALLATION)));
    verify(githubClient).getWhitelistedGithubAppInstallations(appConfigurationCaptor.capture());
    verifyAppConfiguration(appConfigurationCaptor.getValue());
  }

  @Test
  public void checkConfig_whenAllPermissionsAreCorrect_shouldReturnSuccessfulCheck() {
    mockGithubConfiguration();
    ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);
    mockGithubAppWithValidConfig(appConfigurationCaptor);

    mockOrganizations(appConfigurationCaptor, "org1", "org2");
    ConfigCheckResult checkResult = configValidator.checkConfig();
    assertSuccessfulAppConfig(checkResult);

    assertThat(checkResult.installations())
      .extracting(InstallationStatus::organization, InstallationStatus::autoProvisioning)
      .containsExactlyInAnyOrder(
        tuple("org1", SUCCESS_CHECK),
        tuple("org2", ConfigStatus.failed(MISSING_ALL_AUTOPROVISIONNING_PERMISSIONS)));
    verifyAppConfiguration(appConfigurationCaptor.getValue());

  }

  private void mockGithubConfiguration() {
    when(gitHubSettings.appId()).thenReturn(APP_ID);
    when(gitHubSettings.privateKey()).thenReturn(PRIVATE_KEY);
    when(gitHubSettings.apiURLOrDefault()).thenReturn(URL);
  }

  private void verifyAppConfiguration(GithubAppConfiguration appConfiguration) {
    assertThat(appConfiguration.getId()).isEqualTo(parseLong(APP_ID));
    assertThat(appConfiguration.getPrivateKey()).isEqualTo(PRIVATE_KEY);
    assertThat(appConfiguration.getApiEndpoint()).isEqualTo(URL);
  }

  private GsonApp mockGithubApp(ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor) {
    GsonApp githubApp = mock(GsonApp.class);
    when(githubClient.getApp(appConfigurationCaptor.capture())).thenReturn(githubApp);
    return githubApp;
  }

  private GsonApp mockGithubAppWithValidConfig(ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor) {
    GsonApp githubApp = mock(GsonApp.class);
    when(githubClient.getApp(appConfigurationCaptor.capture())).thenReturn(githubApp);
    when(githubApp.getPermissions()).thenReturn(
      Permissions.builder()
        .setMembers("read")
        .setEmails("read")
        .setMetadata("read")
        .setRepoAdministration("read")
        .setOrgAdministration("read")
        .build()
    );

    return githubApp;
  }

  private static void assertSuccessfulAppConfig(ConfigCheckResult checkResult) {
    assertThat(checkResult.application().jit()).isEqualTo(ConfigStatus.SUCCESS);
    assertThat(checkResult.application().autoProvisioning()).isEqualTo(ConfigStatus.SUCCESS);
  }

  private void mockOrganizationsWithoutPermissions(ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor, String... organizations) {

    List<GithubAppInstallation> installations = Arrays.stream(organizations).map(GithubProvisioningConfigValidatorTest::mockInstallation).toList();
    when(githubClient.getWhitelistedGithubAppInstallations(appConfigurationCaptor.capture())).thenReturn(installations);

  }

  private void mockSuspendedOrganizations(String orgName) {
    GithubAppInstallation installation = new GithubAppInstallation(null, orgName, null, true);
    when(githubClient.getWhitelistedGithubAppInstallations(any())).thenReturn(List.of(installation));
  }

  private static GithubAppInstallation mockInstallation(String org) {
    GithubAppInstallation installation = mock(GithubAppInstallation.class);
    when(installation.organizationName()).thenReturn(org);
    when(installation.permissions()).thenReturn(mock(Permissions.class));
    return installation;
  }

  private static GithubAppInstallation mockInstallationWithAllPermissions(String org) {
    GithubAppInstallation installation = mockInstallation(org);
    when(installation.permissions()).thenReturn(
      Permissions.builder()
        .setMembers("read")
        .setEmails("read")
        .setMetadata("read")
        .setRepoAdministration("read")
        .setOrgAdministration("read")
        .build()
    );
    return installation;
  }

  private void mockOrganizations(ArgumentCaptor<GithubAppConfiguration> appConfigurationCaptor, String orgWithMembersPermission, String orgWithoutMembersPermission) {
    List<GithubAppInstallation> installations = List.of(
      mockInstallationWithAllPermissions(orgWithMembersPermission),
      mockInstallation(orgWithoutMembersPermission));
    when(githubClient.getWhitelistedGithubAppInstallations(appConfigurationCaptor.capture())).thenReturn(installations);
  }

}
