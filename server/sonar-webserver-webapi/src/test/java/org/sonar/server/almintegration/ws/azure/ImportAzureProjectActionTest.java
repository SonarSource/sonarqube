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
package org.sonar.server.almintegration.ws.azure;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureProject;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;

public class ImportAzureProjectActionTest {

  private static final String GENERATED_PROJECT_KEY = "TEST_PROJECT_KEY";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public final I18nRule i18n = new I18nRule();

  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);

  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), i18n, System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestProjectIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver);

  private final Encryption encryption = mock(Encryption.class);
  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private final ImportAzureProjectAction importAzureProjectAction = new ImportAzureProjectAction(db.getDbClient(), userSession,
    azureDevOpsHttpClient, projectDefaultVisibility, componentUpdater, importHelper, projectKeyGenerator);
  private final WsActionTester ws = new WsActionTester(importAzureProjectAction);

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(projectKeyGenerator.generateUniqueProjectKey(any(), any())).thenReturn(GENERATED_PROJECT_KEY);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void import_project() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setPersonalAccessToken(almSetting.getDecryptedPersonalAccessToken(encryption));
      dto.setUserUuid(user.getUuid());
    });
    GsonAzureRepo repo = getGsonAzureRepo();
    when(azureDevOpsHttpClient.getRepo(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption),
      "project-name", "repo-name"))
      .thenReturn(repo);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();

    Optional<ProjectAlmSettingDto> projectAlmSettingDto = db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get());
    assertThat(projectAlmSettingDto.get().getAlmRepo()).isEqualTo("repo-name");
    assertThat(projectAlmSettingDto.get().getAlmSettingUuid()).isEqualTo(almSetting.getUuid());
    assertThat(projectAlmSettingDto.get().getAlmSlug()).isEqualTo("project-name");

    Optional<BranchDto> mainBranch = db.getDbClient()
      .branchDao()
      .selectByProject(db.getSession(), projectDto.get())
      .stream()
      .filter(BranchDto::isMain)
      .findFirst();
    assertThat(mainBranch).isPresent();
    assertThat(mainBranch.get().getKey()).hasToString("repo-default-branch");

    verify(projectKeyGenerator).generateUniqueProjectKey(repo.getProject().getName(), repo.getName());
  }

  @Test
  public void import_project_from_empty_repo() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setPersonalAccessToken(almSetting.getDecryptedPersonalAccessToken(encryption));
      dto.setUserUuid(user.getUuid());
    });
    GsonAzureRepo repo = getEmptyGsonAzureRepo();
    when(azureDevOpsHttpClient.getRepo(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption),
      "project-name", "repo-name"))
      .thenReturn(repo);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    Optional<BranchDto> mainBranch = db.getDbClient()
      .branchDao()
      .selectByProject(db.getSession(), projectDto.get())
      .stream()
      .filter(BranchDto::isMain)
      .findFirst();

    assertThat(mainBranch).isPresent();
    assertThat(mainBranch.get().getKey()).hasToString(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam("almSetting", "azure")
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_project_creator_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(SCAN);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "azure")
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("personal access token for '" + almSetting.getKey() + "' is missing");
  }

  @Test
  public void fail_check_alm_setting_not_found() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmPatDto almPatDto = newAlmPatDto();
    db.getDbClient().almPatDao().insert(db.getSession(), almPatDto, user.getLogin(), null);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "testKey");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting 'testKey' not found");
  }

  @Test
  public void fail_project_already_exists() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setPersonalAccessToken(almSetting.getDecryptedPersonalAccessToken(encryption));
      dto.setUserUuid(user.getUuid());
    });
    GsonAzureRepo repo = getGsonAzureRepo();
    db.components().insertPublicProject(p -> p.setKey(GENERATED_PROJECT_KEY));

    when(azureDevOpsHttpClient.getRepo(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption),
      "project-name", "repo-name")).thenReturn(repo);
    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", GENERATED_PROJECT_KEY, GENERATED_PROJECT_KEY);
  }

  @Test
  public void define() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.6");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", true),
        tuple("projectName", true),
        tuple("repositoryName", true));
  }

  private GsonAzureRepo getGsonAzureRepo() {
    return new GsonAzureRepo("repo-id", "repo-name", "repo-url",
      new GsonAzureProject("project-name", "project-description"),
      "refs/heads/repo-default-branch");
  }

  private GsonAzureRepo getEmptyGsonAzureRepo() {
    return new GsonAzureRepo("repo-id", "repo-name", "repo-url",
      new GsonAzureProject("project-name", "project-description"), null);
  }

}
