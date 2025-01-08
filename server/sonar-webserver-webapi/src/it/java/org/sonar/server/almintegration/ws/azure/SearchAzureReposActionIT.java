/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureProject;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.alm.client.azure.GsonAzureRepoList;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonarqube.ws.AlmIntegrations.AzureRepo;
import static org.sonarqube.ws.AlmIntegrations.SearchAzureReposWsResponse;

public class SearchAzureReposActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);
  private final Encryption encryption = mock(Encryption.class);
  private final WsActionTester ws = new WsActionTester(new SearchAzureReposAction(db.getDbClient(), userSession, azureDevOpsHttpClient));
  private int projectId = 0;

  @Before
  public void before() {
    mockClient(new GsonAzureRepoList(ImmutableList.of(getGsonAzureRepo("project-1", "repoName-1"),
      getGsonAzureRepo("project-2", "repoName-2"))));
  }

  @Test
  public void define() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.6");
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleFormat()).isEqualTo("json");
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", true),
        tuple("projectName", false),
        tuple("searchQuery", false));
  }

  @Test
  public void search_repos() {
    AlmSettingDto almSetting = insertAlmSetting();

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName)
      .containsExactlyInAnyOrder(
        tuple("repoName-1", "project-1"), tuple("repoName-2", "project-2"));
  }

  @Test
  public void search_repos_alphabetically_sorted() {
    mockClient(new GsonAzureRepoList(ImmutableList.of(getGsonAzureRepo("project-1", "Z-repo"),
      getGsonAzureRepo("project-1", "A-repo-1"), getGsonAzureRepo("project-1", "a-repo"),
      getGsonAzureRepo("project-1", "b-repo"))));

    AlmSettingDto almSetting = insertAlmSetting();

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName)
      .containsExactly(
        tuple("a-repo", "project-1"), tuple("A-repo-1", "project-1"),
        tuple("b-repo", "project-1"), tuple("Z-repo", "project-1"));
  }

  @Test
  public void search_repos_with_project_already_set_up() {
    AlmSettingDto almSetting = insertAlmSetting();

    ProjectDto projectDto2 = insertProject(almSetting, "repoName-2", "project-2");

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesCount()).isEqualTo(2);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName,
        AzureRepo::getSqProjectKey, AzureRepo::getSqProjectName)
      .containsExactlyInAnyOrder(
        tuple("repoName-1", "project-1", "", ""),
        tuple("repoName-2", "project-2", projectDto2.getKey(), projectDto2.getName()));
  }

  @Test
  public void search_repos_with_project_already_set_u_and_collision_is_handled() {
    AlmSettingDto almSetting = insertAlmSetting();

    ProjectDto projectDto2 = insertProject(almSetting, "repoName-2", "project-2");
    insertProject(almSetting, "repoName-2", "project-2");

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesCount()).isEqualTo(2);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName,
        AzureRepo::getSqProjectKey, AzureRepo::getSqProjectName)
      .containsExactlyInAnyOrder(
        tuple("repoName-1", "project-1", "", ""),
        tuple("repoName-2", "project-2", projectDto2.getKey(), projectDto2.getName()));
  }

  @Test
  public void search_repos_with_projects_already_set_up_and_no_collision() {
    mockClient(new GsonAzureRepoList(ImmutableList.of(getGsonAzureRepo("project-1", "repoName-1"),
      getGsonAzureRepo("project", "1-repoName-1"))));
    AlmSettingDto almSetting = insertAlmSetting();

    ProjectDto projectDto1 = insertProject(almSetting, "repoName-1", "project-1");
    ProjectDto projectDto2 = insertProject(almSetting, "1-repoName-1", "project");

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesCount()).isEqualTo(2);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName,
        AzureRepo::getSqProjectKey, AzureRepo::getSqProjectName)
      .containsExactlyInAnyOrder(
        tuple("repoName-1", "project-1", projectDto1.getKey(), projectDto1.getName()),
        tuple("1-repoName-1", "project", projectDto2.getKey(), projectDto2.getName()));
  }

  @Test
  public void search_repos_with_same_name_and_different_project() {
    mockClient(new GsonAzureRepoList(ImmutableList.of(getGsonAzureRepo("project-1", "repoName-1"),
      getGsonAzureRepo("project-2", "repoName-1"))));
    AlmSettingDto almSetting = insertAlmSetting();

    ProjectDto projectDto1 = insertProject(almSetting, "repoName-1", "project-1");
    ProjectDto projectDto2 = insertProject(almSetting, "repoName-1", "project-2");

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesCount()).isEqualTo(2);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName,
        AzureRepo::getSqProjectKey, AzureRepo::getSqProjectName)
      .containsExactlyInAnyOrder(
        tuple("repoName-1", "project-1", projectDto1.getKey(), projectDto1.getName()),
        tuple("repoName-1", "project-2", projectDto2.getKey(), projectDto2.getName()));
  }

  @Test
  public void search_repos_with_project_name() {
    AlmSettingDto almSetting = insertAlmSetting();

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-1")
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName)
      .containsExactlyInAnyOrder(
        tuple("repoName-1", "project-1"), tuple("repoName-2", "project-2"));
  }

  @Test
  public void search_repos_with_project_name_and_empty_criteria() {
    AlmSettingDto almSetting = insertAlmSetting();

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-1")
      .setParam("searchQuery", "")
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName)
      .containsExactlyInAnyOrder(
        tuple("repoName-1", "project-1"), tuple("repoName-2", "project-2"));
  }

  @Test
  public void search_and_filter_repos_with_repo_name() {
    AlmSettingDto almSetting = insertAlmSetting();

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("searchQuery", "repoName-2")
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName)
      .containsExactlyInAnyOrder(tuple("repoName-2", "project-2"));
  }

  @Test
  public void search_and_filter_repos_with_matching_repo_and_project_name() {
    mockClient(new GsonAzureRepoList(ImmutableList.of(getGsonAzureRepo("big-project", "repo-1"),
      getGsonAzureRepo("big-project", "repo-2"),
      getGsonAzureRepo("big-project", "big-repo"),
      getGsonAzureRepo("project", "big-repo"),
      getGsonAzureRepo("project", "small-repo"))));
    AlmSettingDto almSetting = insertAlmSetting();

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("searchQuery", "big")
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesList())
      .extracting(AzureRepo::getName, AzureRepo::getProjectName)
      .containsExactlyInAnyOrder(tuple("repo-1", "big-project"), tuple("repo-2", "big-project"),
        tuple("big-repo", "big-project"), tuple("big-repo", "project"));
  }

  @Test
  public void return_empty_list_when_there_are_no_azure_repos() {
    when(azureDevOpsHttpClient.getRepos(any(), any(), any())).thenReturn(new GsonAzureRepoList(emptyList()));

    AlmSettingDto almSetting = insertAlmSetting();

    SearchAzureReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchAzureReposWsResponse.class);

    assertThat(response.getRepositoriesList()).isEmpty();
  }

  @Test
  public void check_pat_is_missing() {
    insertUser();
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No personal access token found");
  }

  @Test
  public void fail_check_pat_alm_setting_not_found() {
    UserDto user = insertUser();
    AlmPatDto almPatDto = newAlmPatDto();
    db.getDbClient().almPatDao().insert(db.getSession(), almPatDto, user.getLogin(), null);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "testKey");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting 'testKey' not found");
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  private ProjectDto insertProject(AlmSettingDto almSetting, String repoName, String projectName) {
    ProjectDto projectDto1 =
      db.components().insertPrivateProject(dto -> dto.setKey("key_" + projectId).setName("name" + projectId++)).getProjectDto();
    db.almSettings().insertAzureProjectAlmSetting(almSetting, projectDto1, projectAlmSettingDto -> projectAlmSettingDto.setAlmRepo(repoName),
      projectAlmSettingDto -> projectAlmSettingDto.setAlmSlug(projectName));
    return projectDto1;
  }

  private void mockClient(GsonAzureRepoList repoList) {
    when(azureDevOpsHttpClient.getRepos(any(), any(), any())).thenReturn(repoList);
  }

  private AlmSettingDto insertAlmSetting() {
    UserDto user = insertUser();
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken(almSetting.getDecryptedPersonalAccessToken(encryption));
    });
    return almSetting;
  }

  @NotNull
  private UserDto insertUser() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    return user;
  }

  private GsonAzureRepo getGsonAzureRepo(String projectName, String repoName) {
    GsonAzureProject project = new GsonAzureProject(projectName, "the best project ever");
    return new GsonAzureRepo("repo-id", repoName, "url", project, "repo-default-branch");
  }
}
