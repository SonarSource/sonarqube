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
package org.sonar.server.almintegration.ws.github;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.i18n.I18n;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
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
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.github.ImportGithubProjectAction.PARAM_ORGANIZATION;
import static org.sonar.server.almintegration.ws.github.ImportGithubProjectAction.PARAM_REPOSITORY_KEY;
import static org.sonar.server.tester.UserSessionRule.standalone;

public class ImportGithubProjectActionTest {

  private static final String PROJECT_KEY_NAME = "PROJECT_NAME";

  @Rule
  public UserSessionRule userSession = standalone();

  private final System2 system2 = mock(System2.class);
  private final GithubApplicationClientImpl appClient = mock(GithubApplicationClientImpl.class);
  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), mock(I18n.class), System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestProjectIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver);

  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final WsActionTester ws = new WsActionTester(new ImportGithubProjectAction(db.getDbClient(), userSession,
    projectDefaultVisibility, appClient, componentUpdater, importHelper, projectKeyGenerator));

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void importProject_ifProjectWithSameNameDoesNotExist_importSucceed() {
    AlmSettingDto githubAlmSetting = setupAlm();
    db.almPats().insert(p -> p.setAlmSettingUuid(githubAlmSetting.getUuid()).setUserUuid(userSession.getUuid()));

    GithubApplicationClient.Repository repository = new GithubApplicationClient.Repository(1L, PROJECT_KEY_NAME, false, "octocat/" + PROJECT_KEY_NAME,
      "https://github.sonarsource.com/api/v3/repos/octocat/" + PROJECT_KEY_NAME, "default-branch");
    when(appClient.getRepository(any(), any(), any(), any())).thenReturn(Optional.of(repository));
    when(projectKeyGenerator.generateUniqueProjectKey(repository.getFullName())).thenReturn(PROJECT_KEY_NAME);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_ORGANIZATION, "octocat")
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(repository.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get()).stream().filter(BranchDto::isMain).findAny();
    assertThat(mainBranch).isPresent();
    assertThat(mainBranch.get().getKey()).isEqualTo("default-branch");
  }

  @Test
  public void importProject_ifProjectWithSameNameAlreadyExists_importSucceed() {
    AlmSettingDto githubAlmSetting = setupAlm();
    db.almPats().insert(p -> p.setAlmSettingUuid(githubAlmSetting.getUuid()).setUserUuid(userSession.getUuid()));
    db.components().insertPublicProject(p -> p.setKey("Hello-World"));

    GithubApplicationClient.Repository repository = new GithubApplicationClient.Repository(1L, "Hello-World", false, "Hello-World",
      "https://github.sonarsource.com/api/v3/repos/octocat/Hello-World", "main");
    when(appClient.getRepository(any(), any(), any(), any())).thenReturn(Optional.of(repository));
    when(projectKeyGenerator.generateUniqueProjectKey(repository.getFullName())).thenReturn(PROJECT_KEY_NAME);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_ORGANIZATION, "octocat")
      .setParam(PARAM_REPOSITORY_KEY, "Hello-World")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(repository.getName());
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, "asdfghjkl")
      .setParam(PARAM_ORGANIZATION, "test")
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_create_project_permission() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_almSetting_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, "unknown")
      .setParam(PARAM_ORGANIZATION, "test")
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting 'unknown' not found");
  }

  @Test
  public void fail_when_personal_access_token_doesnt_exist() {
    AlmSettingDto githubAlmSetting = setupAlm();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_ORGANIZATION, "test")
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No personal access token found");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.4");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple(PARAM_ALM_SETTING, true),
        tuple(PARAM_ORGANIZATION, true),
        tuple(PARAM_REPOSITORY_KEY, true));
  }

  private AlmSettingDto setupAlm() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);

    return db.almSettings().insertGitHubAlmSetting(alm -> alm.setClientId("client_123").setClientSecret("client_secret_123"));
  }
}
