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
import org.sonar.server.common.almsettings.permission.DopPermissionCheck;
import org.sonar.server.common.almsettings.permission.DopPermissionValidationService;
import org.sonar.server.common.almsettings.permission.PermissionCheckStatus;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.dop.response.PermissionCheckResource;
import org.sonar.server.v2.api.dop.response.PermissionChecksRestResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.ProjectPermission.USER;
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
    when(dopPermissionValidationService.checkAll(any())).thenReturn(List.of(
      DopPermissionCheck.insufficient(),
      DopPermissionCheck.checkFailed()));

    userSession.logIn().setSystemAdministrator();
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT)).andExpect(status().isOk()).andReturn();

    PermissionChecksRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class);
    // Bitbucket is filtered out; supported platforms are mapped with their status.
    assertThat(response.permissionChecks())
      .extracting(PermissionCheckResource::key, PermissionCheckResource::type, PermissionCheckResource::status)
      .containsExactly(
        tuple("key_github", "github", PermissionCheckStatus.INSUFFICIENT),
        tuple("key_gitlab", "gitlab", PermissionCheckStatus.CHECK_FAILED));
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
    ProjectAlmSettingDto binding = mock();
    when(binding.getAlmSettingUuid()).thenReturn("alm-uuid");
    when(dbClient.projectDao().selectProjectByKey(dbSession, "my-project")).thenReturn(Optional.of(project));
    when(dbClient.projectAlmSettingDao().selectByProject(dbSession, project)).thenReturn(Optional.of(binding));
    when(dbClient.almSettingDao().selectByUuid(dbSession, "alm-uuid")).thenReturn(Optional.of(gitlabDto));
    when(dopPermissionValidationService.check(gitlabDto)).thenReturn(DopPermissionCheck.checkFailed());

    userSession.logIn().addProjectPermission(USER, project);
    MvcResult mvcResult = mockMvc.perform(get(PERMISSION_CHECKS_ENDPOINT).param("project", "my-project")).andExpect(status().isOk()).andReturn();

    PermissionChecksRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), PermissionChecksRestResponse.class);
    PermissionCheckResource resource = response.permissionChecks().get(0);
    assertThat(resource.key()).isEqualTo("key_gitlab");
    assertThat(resource.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
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
