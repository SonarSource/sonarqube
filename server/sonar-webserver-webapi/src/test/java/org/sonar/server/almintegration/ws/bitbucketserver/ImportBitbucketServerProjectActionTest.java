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
package org.sonar.server.almintegration.ws.bitbucketserver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Branch;
import org.sonar.alm.client.bitbucketserver.BranchesList;
import org.sonar.alm.client.bitbucketserver.Project;
import org.sonar.alm.client.bitbucketserver.Repository;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
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
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.JVMRandom.nextLong;
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

public class ImportBitbucketServerProjectActionTest {
  private static final String GENERATED_PROJECT_KEY = "TEST_PROJECT_KEY";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public final I18nRule i18n = new I18nRule();

  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), i18n, System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestProjectIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver);

  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private final WsActionTester ws = new WsActionTester(new ImportBitbucketServerProjectAction(db.getDbClient(), userSession,
    bitbucketServerRestClient, projectDefaultVisibility, componentUpdater, importHelper, projectKeyGenerator));

  private static BranchesList defaultBranchesList;

  @BeforeClass
  public static void beforeAll() {
    Branch defaultBranch = new Branch("default", true);
    defaultBranchesList = new BranchesList(Collections.singletonList(defaultBranch));
  }

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
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    Project project = getGsonBBSProject();
    Repository repo = getGsonBBSRepo(project);
    when(bitbucketServerRestClient.getRepo(any(), any(), any(), any())).thenReturn(repo);
    when(bitbucketServerRestClient.getBranches(any(), any(), any(), any())).thenReturn(defaultBranchesList);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();
    verify(projectKeyGenerator).generateUniqueProjectKey(requireNonNull(project.getKey()), repo.getSlug());
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
    Project project = getGsonBBSProject();
    Repository repo = getGsonBBSRepo(project);
    db.components().insertPublicProject(p -> p.setKey(GENERATED_PROJECT_KEY));

    assertThatThrownBy(() -> {
      when(bitbucketServerRestClient.getRepo(any(), any(), any(), any())).thenReturn(repo);
      when(bitbucketServerRestClient.getBranches(any(), any(), any(), any())).thenReturn(defaultBranchesList);

      ws.newRequest()
        .setParam("almSetting", almSetting.getKey())
        .setParam("projectKey", "projectKey")
        .setParam("repositorySlug", "repo-slug")
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", GENERATED_PROJECT_KEY, GENERATED_PROJECT_KEY);
  }

  @Test
  public void fail_when_not_logged_in() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "sdgfdshfjztutz")
        .setParam("projectKey", "projectKey")
        .setParam("repositorySlug", "repo-slug")
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_project_creator_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(SCAN);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "sdgfdshfjztutz")
        .setParam("projectKey", "projectKey")
        .setParam("repositorySlug", "repo-slug")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", almSetting.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("personal access token for '" + almSetting.getKey() + "' is missing");
  }

  @Test
  public void fail_check_alm_setting_not_found() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmPatDto almPatDto = newAlmPatDto();
    db.getDbClient().almPatDao().insert(db.getSession(), almPatDto, user.getLogin(), null);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "testKey")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting 'testKey' not found");
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "anyvalue")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void handle_givenNoDefaultBranchFound_doNotUpdateDefaultBranchName() {
    BranchesList branchesList = new BranchesList();
    Branch branch = new Branch("not_a_master", false);
    branchesList.addBranch(branch);

    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    Project project = getGsonBBSProject();
    Repository repo = getGsonBBSRepo(project);
    when(bitbucketServerRestClient.getRepo(any(), any(), any(), any())).thenReturn(repo);
    when(bitbucketServerRestClient.getBranches(any(), any(), any(), any())).thenReturn(branchesList);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());

    Collection<BranchDto> branchDtos = db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get());
    List<BranchDto> collect = branchDtos.stream().filter(BranchDto::isMain).toList();
    String mainBranchName = collect.iterator().next().getKey();
    assertThat(mainBranchName).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void handle_givenDefaultBranchNamedDefault_updateDefaultBranchNameToDefault() {
    BranchesList branchesList = new BranchesList();
    Branch branch = new Branch("default", true);
    branchesList.addBranch(branch);

    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    Project project = getGsonBBSProject();
    Repository repo = getGsonBBSRepo(project);
    when(bitbucketServerRestClient.getRepo(any(), any(), any(), any())).thenReturn(repo);
    when(bitbucketServerRestClient.getBranches(any(), any(), any(), any())).thenReturn(branchesList);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());

    Collection<BranchDto> branchDtos = db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get());
    List<BranchDto> collect = branchDtos.stream().filter(BranchDto::isMain).toList();
    String mainBranchName = collect.iterator().next().getKey();
    assertThat(mainBranchName).isEqualTo("default");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.2");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", true),
        tuple("repositorySlug", true),
        tuple("projectKey", true));
  }

  private Repository getGsonBBSRepo(Project project) {
    Repository bbsResult = new Repository();
    bbsResult.setProject(project);
    bbsResult.setSlug(randomAlphanumeric(5));
    bbsResult.setName(randomAlphanumeric(5));
    bbsResult.setId(nextLong(100));
    return bbsResult;
  }

  private Project getGsonBBSProject() {
    return new Project()
      .setKey(randomAlphanumeric(5))
      .setId(nextLong(100))
      .setName(randomAlphanumeric(5));
  }

}
