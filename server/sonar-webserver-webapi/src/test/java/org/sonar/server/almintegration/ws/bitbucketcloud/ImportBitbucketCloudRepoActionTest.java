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
package org.sonar.server.almintegration.ws.bitbucketcloud;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.MainBranch;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Project;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Repository;
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

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;

public class ImportBitbucketCloudRepoActionTest {

  private static final String GENERATED_PROJECT_KEY = "TEST_PROJECT_KEY";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public final I18nRule i18n = new I18nRule();

  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final BitbucketCloudRestClient bitbucketCloudRestClient = mock(BitbucketCloudRestClient.class);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), i18n, System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestProjectIndexers(), new SequenceUuidFactory(),
    mock(DefaultBranchNameResolver.class));

  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private final WsActionTester ws = new WsActionTester(new ImportBitbucketCloudRepoAction(db.getDbClient(), userSession,
    bitbucketCloudRestClient, projectDefaultVisibility, componentUpdater, importHelper, projectKeyGenerator));

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(projectKeyGenerator.generateUniqueProjectKey(any(), any())).thenReturn(GENERATED_PROJECT_KEY);
  }

  @Test
  public void import_project() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    Repository repo = getGsonBBCRepo();
    when(bitbucketCloudRestClient.getRepo(any(), any(), any())).thenReturn(repo);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    Optional<ProjectAlmSettingDto> projectAlmSettingDto = db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get());
    assertThat(projectAlmSettingDto).isPresent();
    assertThat(projectAlmSettingDto.get().getAlmRepo()).isEqualTo("repo-slug-1");

    Optional<BranchDto> branchDto = db.getDbClient().branchDao().selectByBranchKey(db.getSession(), projectDto.get().getUuid(), "develop");
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isMain()).isTrue();
    verify(projectKeyGenerator).generateUniqueProjectKey(requireNonNull(almSetting.getAppId()), repo.getSlug());
  }

  @Test
  public void fail_project_already_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    Repository repo = getGsonBBCRepo();
    db.components().insertPublicProject(p -> p.setKey(GENERATED_PROJECT_KEY));

    when(bitbucketCloudRestClient.getRepo(any(), any(), any())).thenReturn(repo);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", GENERATED_PROJECT_KEY, GENERATED_PROJECT_KEY);
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam("almSetting", "sdgfdshfjztutz")
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_project_creator_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(SCAN);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "sdgfdshfjztutz")
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Username and App Password for '" + almSetting.getKey() + "' is missing");
  }

  @Test
  public void fail_check_alm_setting_not_found() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmPatDto almPatDto = newAlmPatDto();
    db.getDbClient().almPatDao().insert(db.getSession(), almPatDto, user.getLogin(), null);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "testKey")
      .setParam("repositorySlug", "repo");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("DevOps Platform Setting 'testKey' not found");
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("9.0");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", true),
        tuple("repositorySlug", true));
  }

  private Repository getGsonBBCRepo() {
    Project project1 = new Project("PROJECT-UUID-ONE", "projectKey1", "projectName1");
    MainBranch mainBranch = new MainBranch("branch", "develop");
    return new Repository("REPO-UUID-ONE", "repo-slug-1", "repoName1", project1, mainBranch);
  }

}
