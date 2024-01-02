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
package org.sonar.server.almintegration.ws.gitlab;

import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.api.utils.System2;
import org.sonar.core.i18n.I18n;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.tester.UserSessionRule.standalone;

public class ImportGitLabProjectActionTest {

  private static final String PROJECT_KEY_NAME = "PROJECT_NAME";

  private final System2 system2 = mock(System2.class);

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = DbTester.create(system2);

  DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), mock(I18n.class), System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestProjectIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver);

  private final GitlabHttpClient gitlabHttpClient = mock(GitlabHttpClient.class);
  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private final ImportGitLabProjectAction importGitLabProjectAction = new ImportGitLabProjectAction(
    db.getDbClient(), userSession, projectDefaultVisibility, gitlabHttpClient, componentUpdater, importHelper, projectKeyGenerator);
  private final WsActionTester ws = new WsActionTester(importGitLabProjectAction);

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void import_project() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken("PAT");
    });
    Project project = getGitlabProject();
    when(gitlabHttpClient.getProject(any(), any(), any())).thenReturn(project);
    when(gitlabHttpClient.getBranches(any(), any(), any())).thenReturn(singletonList(new GitLabBranch("master", true)));
    when(projectKeyGenerator.generateUniqueProjectKey(project.getPathWithNamespace())).thenReturn(PROJECT_KEY_NAME);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(gitlabHttpClient).getProject(almSetting.getUrl(), "PAT", 12345L);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(project.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();
  }

  @Test
  public void import_project_with_specific_different_default_branch() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken("PAT");
    });
    Project project = getGitlabProject();
    when(gitlabHttpClient.getProject(any(), any(), any())).thenReturn(project);
    when(gitlabHttpClient.getBranches(any(), any(), any())).thenReturn(singletonList(new GitLabBranch("main", true)));
    when(projectKeyGenerator.generateUniqueProjectKey(project.getPathWithNamespace())).thenReturn(PROJECT_KEY_NAME);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(gitlabHttpClient).getProject(almSetting.getUrl(), "PAT", 12345L);
    verify(gitlabHttpClient).getBranches(almSetting.getUrl(), "PAT", 12345L);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(project.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();

    Assertions.assertThat(db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get()))
      .extracting(BranchDto::getKey, BranchDto::isMain)
      .containsExactlyInAnyOrder(tuple("main", true));
  }

  @Test
  public void import_project_no_gitlab_default_branch() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken("PAT");
    });
    Project project = getGitlabProject();
    when(gitlabHttpClient.getProject(any(), any(), any())).thenReturn(project);
    when(gitlabHttpClient.getBranches(any(), any(), any())).thenReturn(emptyList());
    when(projectKeyGenerator.generateUniqueProjectKey(project.getPathWithNamespace())).thenReturn(PROJECT_KEY_NAME);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(gitlabHttpClient).getProject(almSetting.getUrl(), "PAT", 12345L);
    verify(gitlabHttpClient).getBranches(almSetting.getUrl(), "PAT", 12345L);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(project.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();

    Assertions.assertThat(db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get()))
      .extracting(BranchDto::getKey, BranchDto::isMain)
      .containsExactlyInAnyOrder(tuple(DEFAULT_MAIN_BRANCH_NAME, true));
  }


  private Project getGitlabProject() {
    return new Project(randomAlphanumeric(5), randomAlphanumeric(5));
  }
}
