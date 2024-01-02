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

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Project;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Repository;
import org.sonar.alm.client.bitbucket.bitbucketcloud.RepositoryList;
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
import org.sonarqube.ws.AlmIntegrations.BBCRepo;
import org.sonarqube.ws.AlmIntegrations.SearchBitbucketcloudReposWsResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class SearchBitbucketCloudReposActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final BitbucketCloudRestClient bitbucketCloudRestClient = mock(BitbucketCloudRestClient.class);

  private final WsActionTester ws = new WsActionTester(
    new SearchBitbucketCloudReposAction(db.getDbClient(), userSession, bitbucketCloudRestClient));

  @Test
  public void list_repos() {
    when(bitbucketCloudRestClient.searchRepos(any(), any(), any(), any(), any())).thenReturn(getRepositoryList());
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setPersonalAccessToken("abc:xyz");
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto projectDto = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertBitbucketCloudProjectAlmSetting(almSetting, projectDto, s -> s.setAlmRepo("repo-slug-2"));

    SearchBitbucketcloudReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("p", "1")
      .setParam("ps", "100")
      .executeProtobuf(SearchBitbucketcloudReposWsResponse.class);

    assertThat(response.getIsLastPage()).isFalse();
    assertThat(response.getPaging().getPageIndex()).isOne();
    assertThat(response.getPaging().getPageSize()).isEqualTo(100);
    assertThat(response.getRepositoriesList())
      .extracting(BBCRepo::getUuid, BBCRepo::getName, BBCRepo::getSlug, BBCRepo::getProjectKey, BBCRepo::getSqProjectKey, BBCRepo::getWorkspace)
      .containsExactlyInAnyOrder(
        tuple("REPO-UUID-ONE", "repoName1", "repo-slug-1", "projectKey1", "", almSetting.getAppId()),
        tuple("REPO-UUID-TWO", "repoName2", "repo-slug-2", "projectKey2", projectDto.getKey(), almSetting.getAppId()));
  }

  @Test
  public void use_projectKey_to_disambiguate_when_multiple_projects_are_binded_on_one_bitbucket_repo() {
    when(bitbucketCloudRestClient.searchRepos(any(), any(), any(), any(), any())).thenReturn(getRepositoryList());
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setKey("B")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setKey("A")).getProjectDto();
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, project1, s -> s.setAlmRepo("repo-slug-2"));
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, project2, s -> s.setAlmRepo("repo-slug-2"));

    SearchBitbucketcloudReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchBitbucketcloudReposWsResponse.class);

    assertThat(response.getRepositoriesList())
      .extracting(BBCRepo::getUuid, BBCRepo::getName, BBCRepo::getSlug, BBCRepo::getProjectKey, BBCRepo::getSqProjectKey)
      .containsExactlyInAnyOrder(
        tuple("REPO-UUID-ONE", "repoName1", "repo-slug-1", "projectKey1", ""),
        tuple("REPO-UUID-TWO", "repoName2", "repo-slug-2", "projectKey2", "A"));
  }

  @Test
  public void return_empty_list_when_no_bbs_repo() {
    RepositoryList repositoryList = new RepositoryList(null, emptyList(), 1, 100);
    when(bitbucketCloudRestClient.searchRepos(any(), any(), any(), any(), any())).thenReturn(repositoryList);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setPersonalAccessToken("abc:xyz");
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto projectDto = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertBitbucketCloudProjectAlmSetting(almSetting, projectDto, s -> s.setAlmRepo("repo-slug-2"));

    SearchBitbucketcloudReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchBitbucketcloudReposWsResponse.class);

    assertThat(response.getIsLastPage()).isTrue();
    assertThat(response.getRepositoriesList()).isEmpty();
  }

  @Test
  public void fail_check_pat_alm_setting_not_found() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmPatDto almPatDto = newAlmPatDto();
    db.getDbClient().almPatDao().insert(db.getSession(), almPatDto, user.getLogin(), null);

    TestRequest request = ws.newRequest().setParam("almSetting", "testKey");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("DevOps Platform Setting 'testKey' not found");
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest().setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest().setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("9.0");
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleFormat()).isEqualTo("json");
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", true),
        tuple("repositoryName", false),
        tuple(PAGE, false),
        tuple(PAGE_SIZE, false));
  }

  @NotNull
  private RepositoryList getRepositoryList() {
    return new RepositoryList(
      "http://next.url",
      asList(getBBCRepo1(), getBBCRepo2()),
      1,
      100);
  }

  private Repository getBBCRepo1() {
    Project project1 = new Project("PROJECT-UUID-ONE", "projectKey1", "projectName1");
    return new Repository("REPO-UUID-ONE", "repo-slug-1", "repoName1", project1, null);
  }

  private Repository getBBCRepo2() {
    Project project2 = new Project("PROJECT-UUID-TWO", "projectKey2", "projectName2");
    return new Repository("REPO-UUID-TWO", "repo-slug-2", "repoName2", project2, null);
  }

}
