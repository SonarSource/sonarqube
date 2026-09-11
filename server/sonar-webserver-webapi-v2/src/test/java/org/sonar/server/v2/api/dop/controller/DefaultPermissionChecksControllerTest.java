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
package org.sonar.server.v2.api.dop.controller;

import com.google.gson.Gson;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.almsettings.permission.AffectedInstallation;
import org.sonar.server.common.almsettings.permission.DopPermissionCheck;
import org.sonar.server.common.almsettings.permission.DopPermissionValidationService;
import org.sonar.server.common.almsettings.permission.InstallationCheckStatus;
import org.sonar.server.common.almsettings.permission.PermissionCheckStatus;
import org.sonar.server.common.almsettings.permission.PermissionDeficit;
import org.sonar.server.common.almsettings.permission.TimestampedPermissionCheck;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.dop.response.AffectedInstallationResource;
import org.sonar.server.v2.api.dop.response.PermissionCheckResource;
import org.sonar.server.v2.api.dop.response.PermissionDeficitResource;
import org.sonar.server.v2.api.dop.response.PermissionChecksRestResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.user.ServiceIdentity.AGENTIC_SHARED;
import static org.sonar.server.user.ServiceIdentity.REMEDIATION_TO_SQS;
import static org.sonar.server.v2.WebApiEndpoints.PERMISSION_CHECKS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultPermissionChecksControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  private final DbSession dbSession = mock();
  private final DopPermissionValidationService dopPermissionValidationService = mock();

  private final MockMvc mockMvc = ControllerTester.getMockMvc(
    new DefaultPermissionChecksController(userSession, dbClient, dopPermissionValidationService));

  private static final Gson gson = new Gson();

  @BeforeEach
  void setup() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  void checkPermissions_whenUserIsNotSystemAdministrator_returnsForbidden() throws Exception {
    userSession.logIn();

    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT)).andExpect(status().isForbidden());
  }

  @Test
  void checkPermissions_asSystemAdmin_mapsSupportedConfigs_filtersBitbucket() throws Exception {
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    AlmSettingDto gitlabDto = almSetting(ALM.GITLAB);
    AlmSettingDto bitbucketDto = almSetting(ALM.BITBUCKET);
    when(dbClient.almSettingDao().selectAll(dbSession)).thenReturn(List.of(githubDto, gitlabDto, bitbucketDto));
    when(dopPermissionValidationService.checkAllCached(any())).thenReturn(List.of(
      new TimestampedPermissionCheck(DopPermissionCheck.insufficient(), 1_000L),
      new TimestampedPermissionCheck(DopPermissionCheck.checkFailed(), 2_000L)));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT)).andExpect(status().isOk()).andReturn();

    PermissionChecksRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class);
    // Bitbucket is filtered out; supported platforms are mapped with their status and cache timestamp.
    assertThat(response.permissionChecks())
      .extracting(PermissionCheckResource::key, PermissionCheckResource::type, PermissionCheckResource::status, PermissionCheckResource::checkedAt)
      .containsExactly(
        tuple("key_github", "github", PermissionCheckStatus.INSUFFICIENT, 1_000L),
        tuple("key_gitlab", "gitlab", PermissionCheckStatus.CHECK_FAILED, 2_000L));
  }

  @Test
  void checkPermissions_asAgenticSharedService_checksAllConfigurationsWithoutSystemAdminPermission() throws Exception {
    UserSession serviceSession = mock(UserSession.class);
    when(serviceSession.getServiceIdentity()).thenReturn(Optional.of(AGENTIC_SHARED));
    when(dbClient.almSettingDao().selectAll(dbSession)).thenReturn(List.of());
    when(dopPermissionValidationService.checkAllCached(List.of())).thenReturn(List.of());
    MockMvc serviceMockMvc = ControllerTester.getMockMvc(
      new DefaultPermissionChecksController(serviceSession, dbClient, dopPermissionValidationService));

    serviceMockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT)).andExpect(status().isOk());

    verify(serviceSession, never()).checkIsSystemAdministrator();
  }

  @Test
  void checkPermissions_asAnotherService_stillChecksSystemAdminPermission() throws Exception {
    UserSession serviceSession = mock(UserSession.class);
    when(serviceSession.getServiceIdentity()).thenReturn(Optional.of(REMEDIATION_TO_SQS));
    when(dbClient.almSettingDao().selectAll(dbSession)).thenReturn(List.of());
    when(dopPermissionValidationService.checkAllCached(List.of())).thenReturn(List.of());
    MockMvc serviceMockMvc = ControllerTester.getMockMvc(
      new DefaultPermissionChecksController(serviceSession, dbClient, dopPermissionValidationService));

    serviceMockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT)).andExpect(status().isOk());

    verify(serviceSession).checkIsSystemAdministrator();
  }

  @Test
  void checkPermissions_withProject_whenUserLacksBrowsePermission_returnsForbidden() throws Exception {
    ProjectDto project = mock();
    when(dbClient.projectDao().selectProjectByKey(dbSession, "my-project")).thenReturn(Optional.of(project));
    userSession.logIn();

    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project")).andExpect(status().isForbidden());
  }

  @Test
  void checkPermissions_withProject_returnsCheckForBoundConfig() throws Exception {
    ProjectDto project = privateProject();
    AlmSettingDto gitlabDto = almSetting(ALM.GITLAB);
    mockProjectBinding(project, gitlabDto, "42");
    when(dopPermissionValidationService.checkForProject(gitlabDto, "42")).thenReturn(new TimestampedPermissionCheck(DopPermissionCheck.checkFailed(), 1_000L));

    userSession.logIn().addProjectPermission(USER, project);
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project")).andExpect(status().isOk()).andReturn();

    PermissionChecksRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class);
    PermissionCheckResource resource = response.permissionChecks().get(0);
    assertThat(resource.key()).isEqualTo("key_gitlab");
    assertThat(resource.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(resource.checkedAt()).isEqualTo(1_000L);
  }

  @Test
  void checkPermissions_withGithubProject_checksTheInstallationCoveringItsOwnRepository() throws Exception {
    // Two projects sharing one configuration can be covered by installations that approved different permissions, so
    // the repository the project is bound to has to reach the check, not just its configuration.
    ProjectDto project = privateProject();
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    mockProjectBinding(project, githubDto, "acme/widgets");
    when(dopPermissionValidationService.checkForProject(githubDto, "acme/widgets"))
      .thenReturn(new TimestampedPermissionCheck(DopPermissionCheck.githubProject(PermissionCheckStatus.INSUFFICIENT, List.of(), InstallationCheckStatus.COMPLETE),
        1_000L));

    userSession.logIn().addProjectPermission(USER, project);
    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project")).andExpect(status().isOk());

    verify(dopPermissionValidationService).checkForProject(githubDto, "acme/widgets");
    verify(dopPermissionValidationService, never()).checkCached(any());
  }

  @Test
  void checkPermissions_withGithubProject_returnsNoInstallationList() throws Exception {
    ProjectDto project = privateProject();
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    mockProjectBinding(project, githubDto, "acme/widgets");
    DopPermissionCheck check = DopPermissionCheck.githubProject(PermissionCheckStatus.INSUFFICIENT,
      List.of(new PermissionDeficit("contents", "write", null)), InstallationCheckStatus.COMPLETE);
    when(dopPermissionValidationService.checkForProject(githubDto, "acme/widgets")).thenReturn(new TimestampedPermissionCheck(check, 1_000L));

    userSession.logIn().addProjectPermission(USER, project);
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project")).andExpect(status().isOk()).andReturn();

    String body = mvcResult.getResponse().getContentAsString();
    assertThat(body).doesNotContain("affectedInstallations", "affectedInstallationCount", "totalInstallationCount");
    PermissionCheckResource resource = gson.fromJson(body, PermissionChecksRestResponse.class).permissionChecks().get(0);
    assertThat(resource.appMissingPermissions()).extracting(PermissionDeficitResource::permission).containsExactly("contents");
    assertThat(resource.installationCheckStatus()).isEqualTo(InstallationCheckStatus.COMPLETE);
  }

  @Test
  void checkPermissions_withConfiguration_returnsThatConfigurationOnly() throws Exception {
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    when(dbClient.almSettingDao().selectByKey(dbSession, "key_github")).thenReturn(Optional.of(githubDto));
    when(dopPermissionValidationService.checkCached(githubDto)).thenReturn(new TimestampedPermissionCheck(DopPermissionCheck.sufficient(), 1_000L));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_github")).andExpect(status().isOk()).andReturn();

    PermissionChecksRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class);
    assertThat(response.permissionChecks()).extracting(PermissionCheckResource::key).containsExactly("key_github");
    verify(dopPermissionValidationService, never()).checkRefreshed(any());
  }

  @Test
  void checkPermissions_withGithubConfiguration_reportsAppAndInstallationFindings() throws Exception {
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    DopPermissionCheck check = DopPermissionCheck.githubComplete(
      List.of(new PermissionDeficit("checks", "write", "read")),
      3,
      List.of(new AffectedInstallation("12345", "acme", "https://github.com/organizations/acme/settings/installations/12345",
        List.of(new PermissionDeficit("contents", "write", null)))));
    when(dbClient.almSettingDao().selectByKey(dbSession, "key_github")).thenReturn(Optional.of(githubDto));
    when(dopPermissionValidationService.checkCached(githubDto)).thenReturn(new TimestampedPermissionCheck(check, 1_000L));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_github")).andExpect(status().isOk()).andReturn();

    PermissionCheckResource resource = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class).permissionChecks().get(0);
    assertThat(resource.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(resource.installationCheckStatus()).isEqualTo(InstallationCheckStatus.COMPLETE);
    assertThat(resource.totalInstallationCount()).isEqualTo(3);
    assertThat(resource.affectedInstallationCount()).isEqualTo(1);
    assertThat(resource.appMissingPermissions())
      .extracting(PermissionDeficitResource::permission, PermissionDeficitResource::required, PermissionDeficitResource::granted)
      .containsExactly(tuple("checks", "write", "read"));
    assertThat(resource.affectedInstallations())
      .extracting(AffectedInstallationResource::installationId, AffectedInstallationResource::installationOwner, AffectedInstallationResource::settingsUrl)
      .containsExactly(tuple("12345", "acme", "https://github.com/organizations/acme/settings/installations/12345"));
    assertThat(resource.affectedInstallations().get(0).missingPermissions())
      .extracting(PermissionDeficitResource::permission, PermissionDeficitResource::granted)
      .containsExactly(tuple("contents", null));
  }

  @Test
  void checkPermissions_reportsTheTotalInstallationCount_soAnUninstalledAppIsDistinguishable() throws Exception {
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    when(dbClient.almSettingDao().selectByKey(dbSession, "key_github")).thenReturn(Optional.of(githubDto));
    when(dopPermissionValidationService.checkCached(githubDto))
      .thenReturn(new TimestampedPermissionCheck(DopPermissionCheck.githubComplete(List.of(), 0, List.of()), 1_000L));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_github")).andExpect(status().isOk()).andReturn();

    PermissionCheckResource resource = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class).permissionChecks().get(0);
    assertThat(resource.totalInstallationCount()).isZero();
    assertThat(resource.affectedInstallationCount()).isZero();
  }

  @Test
  void checkPermissions_withGithubProject_whenTheAppIsNotInstalledOnTheRepository_saysNotInstalled() throws Exception {
    ProjectDto project = privateProject();
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    mockProjectBinding(project, githubDto, "acme/widgets");
    when(dopPermissionValidationService.checkForProject(githubDto, "acme/widgets")).thenReturn(new TimestampedPermissionCheck(
      DopPermissionCheck.githubProject(PermissionCheckStatus.INSUFFICIENT, List.of(), InstallationCheckStatus.NOT_INSTALLED), 1_000L));

    userSession.logIn().addProjectPermission(USER, project);
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project")).andExpect(status().isOk()).andReturn();

    PermissionCheckResource resource = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class).permissionChecks().get(0);
    assertThat(resource.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(resource.installationCheckStatus()).isEqualTo(InstallationCheckStatus.NOT_INSTALLED);
  }

  @Test
  void checkPermissions_whenTheInstallationScanFailed_omitsTheCountAndListRatherThanReportingZero() throws Exception {
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    when(dbClient.almSettingDao().selectByKey(dbSession, "key_github")).thenReturn(Optional.of(githubDto));
    when(dopPermissionValidationService.checkCached(githubDto))
      .thenReturn(new TimestampedPermissionCheck(DopPermissionCheck.githubInstallationCheckFailed(List.of()), 1_000L));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_github")).andExpect(status().isOk()).andReturn();

    String body = mvcResult.getResponse().getContentAsString();
    assertThat(body)
      .contains("\"status\":\"CHECK_FAILED\"", "\"installationCheckStatus\":\"FAILED\"")
      .doesNotContain("totalInstallationCount", "affectedInstallationCount", "affectedInstallations");
  }

  @Test
  void checkPermissions_forGitlab_omitsTheGithubOnlyFields() throws Exception {
    AlmSettingDto gitlabDto = almSetting(ALM.GITLAB);
    when(dbClient.almSettingDao().selectAll(dbSession)).thenReturn(List.of(gitlabDto));
    when(dopPermissionValidationService.checkAllCached(any()))
      .thenReturn(List.of(new TimestampedPermissionCheck(DopPermissionCheck.unsupportedTokenType(), 1_000L)));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT)).andExpect(status().isOk()).andReturn();

    String body = mvcResult.getResponse().getContentAsString();
    assertThat(body)
      .contains("\"status\":\"UNSUPPORTED_TOKEN_TYPE\"")
      .doesNotContain("appMissingPermissions", "installationCheckStatus");
  }

  @Test
  void checkPermissions_withUnknownConfiguration_returnsNotFound() throws Exception {
    when(dbClient.almSettingDao().selectByKey(dbSession, "nope")).thenReturn(Optional.empty());

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "nope")).andExpect(status().isNotFound());
  }

  @Test
  void checkPermissions_withUnsupportedConfiguration_returnsNotFound() throws Exception {
    when(dbClient.almSettingDao().selectByKey(dbSession, "key_bitbucket")).thenReturn(Optional.of(almSetting(ALM.BITBUCKET)));

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_bitbucket")).andExpect(status().isNotFound());
  }

  @Test
  void checkPermissions_withConfiguration_whenUserIsNotSystemAdministrator_returnsForbidden() throws Exception {
    userSession.logIn();

    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_github")).andExpect(status().isForbidden());
  }

  @Test
  void checkPermissions_withRefresh_runsALiveCheckForThatConfiguration() throws Exception {
    AlmSettingDto githubDto = almSetting(ALM.GITHUB);
    when(dbClient.almSettingDao().selectByKey(dbSession, "key_github")).thenReturn(Optional.of(githubDto));
    when(dopPermissionValidationService.checkRefreshed(githubDto)).thenReturn(new TimestampedPermissionCheck(DopPermissionCheck.sufficient(), 2_000L));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_github").param("refresh", "true"))
      .andExpect(status().isOk()).andReturn();

    PermissionCheckResource resource = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class).permissionChecks().get(0);
    assertThat(resource.checkedAt()).isEqualTo(2_000L);
    verify(dopPermissionValidationService).checkRefreshed(githubDto);
    verify(dopPermissionValidationService, never()).checkCached(any());
  }

  @Test
  void checkPermissions_withRefresh_asTrustedService_isStillForbidden() throws Exception {
    // Reading a cached status is one thing; making the instance call GitHub on demand is an administrator action.
    UserSession serviceSession = mock(UserSession.class);
    when(serviceSession.getServiceIdentity()).thenReturn(Optional.of(AGENTIC_SHARED));
    doThrow(new ForbiddenException("Insufficient privileges")).when(serviceSession).checkIsSystemAdministrator();
    MockMvc serviceMockMvc = ControllerTester.getMockMvc(
      new DefaultPermissionChecksController(serviceSession, dbClient, dopPermissionValidationService));

    serviceMockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("configuration", "key_github").param("refresh", "true"))
      .andExpect(status().isForbidden());
  }

  @Test
  void checkPermissions_withRefreshButNoConfiguration_returnsBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("refresh", "true")).andExpect(status().isBadRequest());
  }

  @Test
  void checkPermissions_withBothProjectAndConfiguration_returnsBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project").param("configuration", "key_github"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void checkPermissions_withProjectAndRefresh_returnsBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project").param("refresh", "true"))
      .andExpect(status().isBadRequest());
  }

  private void mockProjectBinding(ProjectDto project, AlmSettingDto almSetting, String almRepo) {
    ProjectAlmSettingDto binding = mock();
    when(binding.getAlmSettingUuid()).thenReturn("alm-uuid");
    when(binding.getAlmRepo()).thenReturn(almRepo);
    when(dbClient.projectDao().selectProjectByKey(dbSession, "my-project")).thenReturn(Optional.of(project));
    when(dbClient.projectAlmSettingDao().selectByProject(dbSession, project)).thenReturn(Optional.of(binding));
    when(dbClient.almSettingDao().selectByUuid(dbSession, "alm-uuid")).thenReturn(Optional.of(almSetting));
  }

  @Test
  void checkPermissions_withProject_whenNoBoundConfiguration_returnsEmptyList() throws Exception {
    ProjectDto project = privateProject();
    when(dbClient.projectDao().selectProjectByKey(dbSession, "my-project")).thenReturn(Optional.of(project));
    when(dbClient.projectAlmSettingDao().selectByProject(dbSession, project)).thenReturn(Optional.empty());

    userSession.logIn().addProjectPermission(USER, project);
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project")).andExpect(status().isOk()).andReturn();

    PermissionChecksRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class);
    assertThat(response.permissionChecks()).isEmpty();
  }

  private static AlmSettingDto almSetting(ALM alm) {
    return new AlmSettingDto().setAlm(alm).setKey("key_" + alm.getId()).setUrl("http://" + alm.getId());
  }

  private static ProjectDto privateProject() {
    ProjectDto project = mock();
    when(project.getUuid()).thenReturn("project-uuid");
    when(project.getAuthUuid()).thenReturn("project-uuid");
    when(project.isPrivate()).thenReturn(true);
    return project;
  }

}
