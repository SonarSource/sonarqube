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

import java.util.Arrays;
import java.util.LinkedList;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.alm.client.gitlab.ProjectList;
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
import org.sonarqube.ws.AlmIntegrations;
import org.sonarqube.ws.AlmIntegrations.GitlabRepository;
import org.sonarqube.ws.AlmIntegrations.SearchGitlabReposWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class SearchGitlabReposActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final GitlabHttpClient gitlabHttpClient = mock(GitlabHttpClient.class);
  private final WsActionTester ws = new WsActionTester(new SearchGitlabReposAction(db.getDbClient(), userSession,
    gitlabHttpClient));

  @Test
  public void list_gitlab_repos() {
    Project gitlabProject1 = new Project(1L, "repoName1", "repoNamePath1", "repo-slug-1", "repo-path-slug-1", "url-1");
    Project gitlabProject2 = new Project(2L, "repoName2", "path1 / repoName2", "repo-slug-2", "path-1/repo-slug-2", "url-2");
    Project gitlabProject3 = new Project(3L, "repoName3", "repoName3 / repoName3", "repo-slug-3", "repo-slug-3/repo-slug-3", "url-3");
    Project gitlabProject4 = new Project(4L, "repoName4", "repoName4 / repoName4 / repoName4", "repo-slug-4", "repo-slug-4/repo-slug-4/repo-slug-4", "url-4");
    when(gitlabHttpClient.searchProjects(any(), any(), any(), anyInt(), anyInt()))
      .thenReturn(
        new ProjectList(Arrays.asList(gitlabProject1, gitlabProject2, gitlabProject3, gitlabProject4), 1, 10, 4));

    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken("some-pat");
    });
    ProjectDto projectDto = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, projectDto);

    AlmIntegrations.SearchGitlabReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchGitlabReposWsResponse.class);

    assertThat(response.getRepositoriesCount()).isEqualTo(4);

    assertThat(response.getRepositoriesList())
      .extracting(GitlabRepository::getId,
        GitlabRepository::getName,
        GitlabRepository::getPathName,
        GitlabRepository::getSlug,
        GitlabRepository::getPathSlug,
        GitlabRepository::getUrl,
        GitlabRepository::hasSqProjectKey,
        GitlabRepository::hasSqProjectName)
      .containsExactlyInAnyOrder(
        tuple(1L, "repoName1", "repoNamePath1", "repo-slug-1", "repo-path-slug-1", "url-1", false, false),
        tuple(2L, "repoName2", "path1", "repo-slug-2", "path-1", "url-2", false, false),
        tuple(3L, "repoName3", "repoName3", "repo-slug-3", "repo-slug-3", "url-3", false, false),
        tuple(4L, "repoName4", "repoName4 / repoName4", "repo-slug-4", "repo-slug-4/repo-slug-4", "url-4", false, false));
  }

  @Test
  public void list_gitlab_repos_some_projects_already_set_up() {
    Project gitlabProject1 = new Project(1L, "repoName1", "repoNamePath1", "repo-slug-1", "repo-path-slug-1", "url-1");
    Project gitlabProject2 = new Project(2L, "repoName2", "path1 / repoName2", "repo-slug-2", "path-1/repo-slug-2", "url-2");
    Project gitlabProject3 = new Project(3L, "repoName3", "repoName3 / repoName3", "repo-slug-3", "repo-slug-3/repo-slug-3", "url-3");
    Project gitlabProject4 = new Project(4L, "repoName4", "repoName4 / repoName4 / repoName4", "repo-slug-4", "repo-slug-4/repo-slug-4/repo-slug-4", "url-4");
    when(gitlabHttpClient.searchProjects(any(), any(), any(), anyInt(), anyInt()))
      .thenReturn(
        new ProjectList(Arrays.asList(gitlabProject1, gitlabProject2, gitlabProject3, gitlabProject4), 1, 10, 4));

    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken("some-pat");
    });
    ProjectDto projectDto1 = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, projectDto1);

    ProjectDto projectDto2 = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, projectDto2, projectAlmSettingDto -> projectAlmSettingDto.setAlmRepo("2"));

    ProjectDto projectDto3 = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, projectDto3, projectAlmSettingDto -> projectAlmSettingDto.setAlmRepo("3"));

    ProjectDto projectDto4 = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, projectDto4, projectAlmSettingDto -> projectAlmSettingDto.setAlmRepo("3"));

    AlmIntegrations.SearchGitlabReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchGitlabReposWsResponse.class);

    assertThat(response.getRepositoriesCount()).isEqualTo(4);

    assertThat(response.getRepositoriesList())
      .extracting(GitlabRepository::getId,
        GitlabRepository::getName,
        GitlabRepository::getPathName,
        GitlabRepository::getSlug,
        GitlabRepository::getPathSlug,
        GitlabRepository::getUrl,
        GitlabRepository::getSqProjectKey,
        GitlabRepository::getSqProjectName)
      .containsExactlyInAnyOrder(
        tuple(1L, "repoName1", "repoNamePath1", "repo-slug-1", "repo-path-slug-1", "url-1", "", ""),
        tuple(2L, "repoName2", "path1", "repo-slug-2", "path-1", "url-2", projectDto2.getKey(), projectDto2.getName()),
        tuple(3L, "repoName3", "repoName3", "repo-slug-3", "repo-slug-3", "url-3", projectDto3.getKey(), projectDto3.getName()),
        tuple(4L, "repoName4", "repoName4 / repoName4", "repo-slug-4", "repo-slug-4/repo-slug-4", "url-4", "", ""));
  }

  @Test
  public void return_empty_list_when_no_gitlab_projects() {
    when(gitlabHttpClient.searchProjects(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ProjectList(new LinkedList<>(), 1, 10, 0));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto projectDto = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, projectDto);

    AlmIntegrations.SearchGitlabReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchGitlabReposWsResponse.class);

    assertThat(response.getRepositoriesList()).isEmpty();
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No personal access token found");
  }

  @Test
  public void fail_when_alm_setting_not_found() {
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
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
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

  @Test
  public void verify_response_example() {
    Project gitlabProject1 = new Project(1L, "Gitlab repo name 1", "Group / Gitlab repo name 1", "gitlab-repo-name-1", "group/gitlab-repo-name-1",
      "https://example.gitlab.com/group/gitlab-repo-name-1");
    Project gitlabProject2 = new Project(2L, "Gitlab repo name 2", "Group / Gitlab repo name 2", "gitlab-repo-name-2", "group/gitlab-repo-name-2",
      "https://example.gitlab.com/group/gitlab-repo-name-2");
    Project gitlabProject3 = new Project(3L, "Gitlab repo name 3", "Group / Gitlab repo name 3", "gitlab-repo-name-3", "group/gitlab-repo-name-3",
      "https://example.gitlab.com/group/gitlab-repo-name-3");
    when(gitlabHttpClient.searchProjects(any(), any(), any(), anyInt(), anyInt()))
      .thenReturn(
        new ProjectList(Arrays.asList(gitlabProject1, gitlabProject2, gitlabProject3), 1, 3, 10));

    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken("some-pat");
    });
    ProjectDto projectDto = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, projectDto);

    WebService.Action def = ws.getDef();
    String responseExample = def.responseExampleAsString();

    assertThat(responseExample).isNotBlank();

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .execute().assertJson(
        responseExample);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.5");
    assertThat(def.isPost()).isFalse();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", true),
        tuple("p", false),
        tuple("ps", false),
        tuple("projectName", false));
  }

}
