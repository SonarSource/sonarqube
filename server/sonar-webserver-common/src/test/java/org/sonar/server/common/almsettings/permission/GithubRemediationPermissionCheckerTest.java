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
package org.sonar.server.common.almsettings.permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubAppInstallationDetails;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonarqube.ws.client.HttpException;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubRemediationPermissionCheckerTest {

  private static final Map<String, String> ALL_REQUIRED_PERMISSIONS = Map.of(
    "contents", "write",
    "pull_requests", "write",
    "checks", "write",
    "metadata", "read");

  private static final AlmSettingDto GITHUB_SETTING = new AlmSettingDto().setAlm(ALM.GITHUB).setKey("my-github");

  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  @Mock
  private GithubApplicationClientImpl githubApplicationClient;

  private final GithubAppConfiguration configuration = new GithubAppConfiguration(1L, "private-key", "https://api.github.com");

  private GithubRemediationPermissionChecker underTest;

  @BeforeEach
  void setUp() {
    underTest = new GithubRemediationPermissionChecker(githubGlobalSettingsValidator, githubApplicationClient);
    lenient().when(githubGlobalSettingsValidator.validateApiEndpoint(GITHUB_SETTING)).thenReturn(configuration);
  }

  @Test
  void checkConnection_whenAppAndEveryInstallationAreCorrect_isSufficient() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    mockInstallations(
      organizationOwned(1L, "acme", ALL_REQUIRED_PERMISSIONS),
      organizationOwned(2L, "globex", ALL_REQUIRED_PERMISSIONS));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.COMPLETE);
    assertThat(result.totalInstallationCount()).isEqualTo(2);
    assertThat(result.affectedInstallationCount()).isZero();
    assertThat(result.affectedInstallations()).isEmpty();
    assertThat(result.appMissingPermissions()).isEmpty();
  }

  @Test
  void checkConnection_whenTheAppIsCorrectButAnInstallationHasNotApproved_isInsufficientOnThatInstallationOnly() {
    // The case the /app check alone cannot see: the app requests contents:write, the installation still runs on the
    // read its owner approved before.
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    mockInstallations(
      organizationOwned(1L, "acme", ALL_REQUIRED_PERMISSIONS),
      organizationOwned(2L, "globex", permissionsWith("contents", "read")));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.appMissingPermissions()).isEmpty();
    assertThat(result.affectedInstallationCount()).isEqualTo(1);
    assertThat(result.affectedInstallations())
      .extracting(AffectedInstallation::installationOwner, AffectedInstallation::installationId)
      .containsExactly(tuple("globex", "2"));
    assertThat(result.affectedInstallations().get(0).missingPermissions())
      .extracting(PermissionDeficit::permission, PermissionDeficit::required, PermissionDeficit::granted)
      .containsExactly(tuple("contents", "write", "read"));
  }

  @Test
  void checkConnection_whenTheAppItselfIsMissingAPermission_reportsItSeparatelyFromTheInstallations() {
    // An installation owner cannot approve what the app never requested, so the two findings are not interchangeable.
    mockAppPermissions(Map.of("pull_requests", "write", "checks", "write", "metadata", "read"));
    mockInstallations(organizationOwned(1L, "acme", ALL_REQUIRED_PERMISSIONS));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.appMissingPermissions())
      .extracting(PermissionDeficit::permission, PermissionDeficit::required, PermissionDeficit::granted)
      .containsExactly(tuple("contents", "write", null));
    assertThat(result.affectedInstallations()).isEmpty();
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.COMPLETE);
  }

  @Test
  void checkConnection_neverReportsAnInstallationForAPermissionTheAppDoesNotRequest() {
    // Every installation is missing contents:write here, because the app never asked for it. Listing all of them
    // would send administrators to settings pages with nothing they could tick.
    mockAppPermissions(permissionsWithout("contents"));
    mockInstallations(
      organizationOwned(1L, "acme", permissionsWithout("contents")),
      organizationOwned(2L, "globex", permissionsWithout("contents")));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.appMissingPermissions()).extracting(PermissionDeficit::permission).containsExactly("contents");
    assertThat(result.affectedInstallations()).isEmpty();
    assertThat(result.affectedInstallationCount()).isZero();
    assertThat(result.totalInstallationCount()).isEqualTo(2);
  }

  @Test
  void checkConnection_whenTheAppIsMissingOnePermission_stillReportsInstallationsForTheOthers() {
    mockAppPermissions(permissionsWithout("contents"));
    mockInstallations(
      organizationOwned(1L, "acme", permissionsWithout("contents")),
      organizationOwned(2L, "globex", permissionsWith("checks", "read")));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.affectedInstallations()).extracting(AffectedInstallation::installationOwner).containsExactly("globex");
    // The deficit reported against globex is 'checks' only: 'contents' belongs to the app finding.
    assertThat(result.affectedInstallations().get(0).missingPermissions())
      .extracting(PermissionDeficit::permission).containsExactly("checks");
  }

  @Test
  void checkConnection_whenNobodyHasInstalledTheApp_isDistinguishableFromEverybodyHavingApproved() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    mockInstallations();

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.totalInstallationCount()).isZero();
    assertThat(result.affectedInstallationCount()).isZero();
  }

  @Test
  void checkConnection_returnsEveryAffectedInstallation_inAStableOrder() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    mockInstallations(
      organizationOwned(30L, "zeta", permissionsWithout("checks")),
      organizationOwned(20L, "Alpha", permissionsWithout("checks")),
      organizationOwned(10L, "alpha", permissionsWithout("checks")),
      personalAccountOwned(25L, "mid-user", permissionsWithout("checks")),
      organizationOwned(40L, "mid", ALL_REQUIRED_PERMISSIONS));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    // Personal-account installations are listed alongside organization ones, not filtered out.
    assertThat(result.totalInstallationCount()).isEqualTo(5);
    assertThat(result.affectedInstallationCount()).isEqualTo(4);
    assertThat(result.affectedInstallations())
      .extracting(AffectedInstallation::installationOwner, AffectedInstallation::installationId)
      .containsExactly(tuple("alpha", "10"), tuple("Alpha", "20"), tuple("mid-user", "25"), tuple("zeta", "30"));
  }

  @Test
  void checkConnection_whenAnInstallationIsSuspendedButHasApproved_doesNotListIt() {
    // Suspension is not a permission an installation owner can approve on the installation settings page.
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    mockInstallations(new GithubAppInstallationDetails(1L, "acme", "Organization", null, true, ALL_REQUIRED_PERMISSIONS));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(result.affectedInstallations()).isEmpty();
  }

  @Test
  void checkConnection_whenThereIsNoInstallationAtAll_completesWithAnEmptyList() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    mockInstallations();

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.COMPLETE);
    assertThat(result.affectedInstallationCount()).isZero();
  }

  @Test
  void checkConnection_whenTheInstallationScanFails_reportsCheckFailedAndDiscardsPartialFindings() {
    // A short list read from the pages that happened to load would read as "nothing left to approve".
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getAllAppInstallations(configuration)).thenThrow(new IllegalStateException("page 2 came back without a body"));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.FAILED);
    assertThat(result.affectedInstallationCount()).isNull();
    assertThat(result.affectedInstallations()).isEmpty();
  }

  @Test
  void checkConnection_whenTheInstallationScanFails_stillReportsWhatIsKnownAboutTheApp() {
    mockAppPermissions(permissionsWithout("contents"));
    when(githubApplicationClient.getAllAppInstallations(configuration)).thenThrow(new IllegalStateException("boom"));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(result.appMissingPermissions()).extracting(PermissionDeficit::permission).containsExactly("contents");
  }

  @Test
  void checkConnection_whenTheConfigurationCannotBeUsed_reportsCheckFailedWithoutScanning() {
    when(githubGlobalSettingsValidator.validateApiEndpoint(GITHUB_SETTING)).thenThrow(new IllegalArgumentException("Missing appId"));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.FAILED);
    verify(githubApplicationClient, never()).getAllAppInstallations(any());
  }

  @Test
  void checkConnection_whenAuthenticationFails_reportsCheckFailed() {
    when(githubApplicationClient.getAppPermissions(configuration))
      .thenThrow(new IllegalArgumentException("Authentication failed, verify the Client Id, Client Secret and Private Key fields"));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    verify(githubApplicationClient, never()).getAllAppInstallations(any());
  }

  @ParameterizedTest
  @MethodSource("settingsUrls")
  void checkConnection_buildsTheInstallationSettingsUrlOnTheConfiguredHost(String apiEndpoint, @Nullable String reportedHtmlUrl, String accountType,
    String expectedSettingsUrl) {
    GithubAppConfiguration hostConfiguration = new GithubAppConfiguration(1L, "private-key", apiEndpoint);
    when(githubGlobalSettingsValidator.validateApiEndpoint(GITHUB_SETTING)).thenReturn(hostConfiguration);
    when(githubApplicationClient.getAppPermissions(hostConfiguration)).thenReturn(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getAllAppInstallations(hostConfiguration))
      .thenReturn(List.of(new GithubAppInstallationDetails(42L, "acme", accountType, reportedHtmlUrl, false, permissionsWith("checks", "read"))));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.affectedInstallations()).extracting(AffectedInstallation::settingsUrl).containsExactly(expectedSettingsUrl);
  }

  private static List<Arguments> settingsUrls() {
    String orgOnDotCom = "https://github.com/organizations/acme/settings/installations/42";
    String orgOnEnterprise = "https://github.acme-corp.com/organizations/acme/settings/installations/42";
    return List.of(
      // An installation an organization owns always links to the organization approval page, never the personal one.
      Arguments.of("https://api.github.com", null, "Organization", orgOnDotCom),
      Arguments.of("https://github.acme-corp.com/api/v3", null, "Organization", orgOnEnterprise),
      Arguments.of("https://api.acme.ghe.com", null, "Organization", "https://acme.ghe.com/organizations/acme/settings/installations/42"),
      // ... on the host this configuration is bound to, whatever host GitHub reported in html_url. GitHub Enterprise
      // payloads in this repository carry a github.com html_url, so it is not usable as the link.
      Arguments.of("https://github.acme-corp.com/api/v3", orgOnDotCom, "Organization", orgOnEnterprise),
      Arguments.of("https://github.acme-corp.com/api/v3", "https://github.acme-corp.com/settings/installations/42", "Organization", orgOnEnterprise),
      Arguments.of("https://api.github.com", "not a url", "Organization", orgOnDotCom),
      // An installation a personal account owns is approved from that account's own settings page.
      Arguments.of("https://api.github.com", null, "User", "https://github.com/settings/installations/42"),
      Arguments.of("https://github.acme-corp.com/api/v3", null, "User", "https://github.acme-corp.com/settings/installations/42"));
  }

  @Test
  void checkConnection_escapesTheOwnerLoginInTheSettingsUrl() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    mockInstallations(organizationOwned(42L, "acme corp", permissionsWith("checks", "read")));

    DopPermissionCheck result = underTest.checkConnection(GITHUB_SETTING);

    assertThat(result.affectedInstallations()).extracting(AffectedInstallation::settingsUrl)
      .containsExactly("https://github.com/organizations/acme%20corp/settings/installations/42");
  }

  @Test
  void checkProject_usesTheInstallationCoveringThatProjectsRepository() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getRepositoryInstallation(configuration, "acme/widgets"))
      .thenReturn(organizationOwned(1L, "acme", ALL_REQUIRED_PERMISSIONS));

    DopPermissionCheck result = underTest.checkProject(GITHUB_SETTING, "acme/widgets");

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.COMPLETE);
    verify(githubApplicationClient, never()).getAllAppInstallations(any());
  }

  @Test
  void checkProject_isUnaffectedByWhatOtherOrganizationsApproved() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getRepositoryInstallation(configuration, "acme/widgets"))
      .thenReturn(organizationOwned(1L, "acme", ALL_REQUIRED_PERMISSIONS));
    when(githubApplicationClient.getRepositoryInstallation(configuration, "globex/gadgets"))
      .thenReturn(organizationOwned(2L, "globex", permissionsWith("contents", "read")));

    assertThat(underTest.checkProject(GITHUB_SETTING, "acme/widgets").status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(underTest.checkProject(GITHUB_SETTING, "globex/gadgets").status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
  }

  @Test
  void checkProject_onAPersonalAccountInstallation_isJudgedLikeAnyOther() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getRepositoryInstallation(configuration, "torvalds/linux"))
      .thenReturn(new GithubAppInstallationDetails(9L, "torvalds", "User", null, false, ALL_REQUIRED_PERMISSIONS));

    assertThat(underTest.checkProject(GITHUB_SETTING, "torvalds/linux").status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
  }

  @Test
  void checkProject_neverCarriesTheOrganizationList() {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getRepositoryInstallation(configuration, "acme/widgets"))
      .thenReturn(organizationOwned(1L, "acme", permissionsWithout("contents")));

    DopPermissionCheck result = underTest.checkProject(GITHUB_SETTING, "acme/widgets");

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.affectedInstallations()).isEmpty();
    assertThat(result.affectedInstallationCount()).isNull();
  }

  @Test
  void checkProject_whenTheAppIsNotInstalledOnTheRepository_saysSoRatherThanReportingAFailedCheck() {
    // A definite answer: the Remediation Agent cannot act, and the fix is to install the app, not to retry.
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getRepositoryInstallation(configuration, "acme/widgets"))
      .thenThrow(new HttpException("https://api.github.com/repos/acme/widgets/installation", HTTP_NOT_FOUND, ""));

    DopPermissionCheck result = underTest.checkProject(GITHUB_SETTING, "acme/widgets");

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.NOT_INSTALLED);
  }

  @ParameterizedTest
  @MethodSource("verificationFailures")
  void checkProject_whenTheInstallationCannotBeRead_reportsAFailedCheck(RuntimeException failure) {
    mockAppPermissions(ALL_REQUIRED_PERMISSIONS);
    when(githubApplicationClient.getRepositoryInstallation(configuration, "acme/widgets")).thenThrow(failure);

    DopPermissionCheck result = underTest.checkProject(GITHUB_SETTING, "acme/widgets");

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.FAILED);
  }

  private static List<Arguments> verificationFailures() {
    return List.of(
      Arguments.of(new HttpException("https://api.github.com/repos/acme/widgets/installation", HTTP_UNAUTHORIZED, "")),
      Arguments.of(new HttpException("https://api.github.com/repos/acme/widgets/installation", HTTP_FORBIDDEN, "")),
      Arguments.of(new HttpException("https://api.github.com/repos/acme/widgets/installation", 500, "")),
      Arguments.of(new IllegalStateException("connection reset")));
  }

  @Test
  void checkProject_neverReportsTheProjectForAPermissionTheAppDoesNotRequest() {
    mockAppPermissions(permissionsWithout("contents"));
    when(githubApplicationClient.getRepositoryInstallation(configuration, "acme/widgets"))
      .thenReturn(organizationOwned(1L, "acme", permissionsWithout("contents")));

    DopPermissionCheck result = underTest.checkProject(GITHUB_SETTING, "acme/widgets");

    // Insufficient because of the app, and the app finding says so exactly once.
    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.appMissingPermissions()).extracting(PermissionDeficit::permission).containsExactly("contents");
  }

  @Test
  void checkProject_whenTheProjectHasNoBoundRepository_judgesTheAppConfigurationOnly() {
    mockAppPermissions(permissionsWithout("contents"));

    DopPermissionCheck result = underTest.checkProject(GITHUB_SETTING, null);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.installationCheckStatus()).isEqualTo(InstallationCheckStatus.NOT_RUN);
    assertThat(result.appMissingPermissions()).extracting(PermissionDeficit::permission).containsExactly("contents");
    verify(githubApplicationClient, never()).getRepositoryInstallation(any(), any());
  }

  @Test
  void checkProject_whenTheAppIsMissingAPermission_isInsufficientEvenIfTheInstallationApprovedEverythingItCould() {
    mockAppPermissions(permissionsWithout("contents"));
    when(githubApplicationClient.getRepositoryInstallation(configuration, "acme/widgets"))
      .thenReturn(organizationOwned(1L, "acme", ALL_REQUIRED_PERMISSIONS));

    DopPermissionCheck result = underTest.checkProject(GITHUB_SETTING, "acme/widgets");

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.appMissingPermissions()).extracting(PermissionDeficit::permission).containsExactly("contents");
  }

  private void mockAppPermissions(Map<String, String> permissions) {
    when(githubApplicationClient.getAppPermissions(configuration)).thenReturn(permissions);
  }

  private void mockInstallations(GithubAppInstallationDetails... installations) {
    when(githubApplicationClient.getAllAppInstallations(configuration)).thenReturn(List.of(installations));
  }

  private static GithubAppInstallationDetails organizationOwned(long id, String login, Map<String, String> permissions) {
    return new GithubAppInstallationDetails(id, login, "Organization", null, false, permissions);
  }

  private static GithubAppInstallationDetails personalAccountOwned(long id, String login, Map<String, String> permissions) {
    return new GithubAppInstallationDetails(id, login, "User", null, false, permissions);
  }

  private static Map<String, String> permissionsWith(String permission, String level) {
    Map<String, String> permissions = new HashMap<>(ALL_REQUIRED_PERMISSIONS);
    permissions.put(permission, level);
    return permissions;
  }

  private static Map<String, String> permissionsWithout(String permission) {
    Map<String, String> permissions = new HashMap<>(ALL_REQUIRED_PERMISSIONS);
    permissions.remove(permission);
    return permissions;
  }
}
