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

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Project;
import org.sonar.alm.client.bitbucketserver.Repository;
import org.sonar.alm.client.bitbucketserver.RepositoryList;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmIntegrations;
import org.sonarqube.ws.AlmIntegrations.SearchBitbucketserverReposWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class SearchBitbucketServerReposActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final ProjectAlmSettingDao projectAlmSettingDao = db.getDbClient().projectAlmSettingDao();
  private final ProjectDao projectDao = db.getDbClient().projectDao();
  private final WsActionTester ws = new WsActionTester(
    new SearchBitbucketServerReposAction(db.getDbClient(), userSession, bitbucketServerRestClient, projectAlmSettingDao, projectDao));

  @Before
  public void before() {
    RepositoryList gsonBBSRepoList = new RepositoryList();
    gsonBBSRepoList.setLastPage(true);
    List<Repository> values = new ArrayList<>();
    values.add(getGsonBBSRepo1());
    values.add(getGsonBBSRepo2());
    gsonBBSRepoList.setValues(values);
    when(bitbucketServerRestClient.getRepos(any(), any(), any(), any())).thenReturn(gsonBBSRepoList);
  }

  @Test
  public void list_repos() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto projectDto = db.components().insertPrivateProject(dto -> dto.setKey("proj_key_1").setName("proj_name_1")).getProjectDto();
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, projectDto, s -> s.setAlmRepo("projectKey2"), s -> s.setAlmSlug("repo-slug-2"));

    AlmIntegrations.SearchBitbucketserverReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchBitbucketserverReposWsResponse.class);

    assertThat(response.getIsLastPage()).isTrue();
    assertThat(response.getRepositoriesList())
      .extracting(AlmIntegrations.BBSRepo::getId, AlmIntegrations.BBSRepo::getName, AlmIntegrations.BBSRepo::getSlug, AlmIntegrations.BBSRepo::hasSqProjectKey,
        AlmIntegrations.BBSRepo::getSqProjectKey, AlmIntegrations.BBSRepo::getProjectKey)
      .containsExactlyInAnyOrder(
        tuple(1L, "repoName1", "repo-slug-1", false, "", "projectKey1"),
        tuple(3L, "repoName2", "repo-slug-2", true, projectDto.getKey(), "projectKey2"));
  }

  @Test
  public void return_empty_list_when_no_bbs_repo() {
    RepositoryList gsonBBSRepoList = new RepositoryList();
    gsonBBSRepoList.setLastPage(true);
    gsonBBSRepoList.setValues(new ArrayList<>());
    when(bitbucketServerRestClient.getRepos(any(), any(), any(), any())).thenReturn(gsonBBSRepoList);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto projectDto = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, projectDto, s -> s.setAlmRepo("projectKey2"), s -> s.setAlmSlug("repo-slug-2"));

    AlmIntegrations.SearchBitbucketserverReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchBitbucketserverReposWsResponse.class);

    assertThat(response.getIsLastPage()).isTrue();
    assertThat(response.getRepositoriesList()).isEmpty();
  }

  @Test
  public void already_imported_detection_does_not_get_confused_by_same_slug_in_different_projects() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto projectDto = db.components().insertPrivateProject(dto -> dto.setName("proj_1").setKey("proj_key_1")).getProjectDto();
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, projectDto, s -> s.setAlmRepo("projectKey2"), s -> s.setAlmSlug("repo-slug-2"));
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, db.components().insertPrivateProject(dto -> dto.setName("proj_2").setKey("proj_key_2")).getProjectDto(), s -> s.setAlmRepo("projectKey2"), s -> s.setAlmSlug("repo-slug-2"));

    AlmIntegrations.SearchBitbucketserverReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchBitbucketserverReposWsResponse.class);

    assertThat(response.getIsLastPage()).isTrue();
    assertThat(response.getRepositoriesList())
      .extracting(AlmIntegrations.BBSRepo::getId, AlmIntegrations.BBSRepo::getName, AlmIntegrations.BBSRepo::getSlug, AlmIntegrations.BBSRepo::getSqProjectKey,
        AlmIntegrations.BBSRepo::getProjectKey)
      .containsExactlyInAnyOrder(
        tuple(1L, "repoName1", "repo-slug-1", "", "projectKey1"),
        tuple(3L, "repoName2", "repo-slug-2", projectDto.getKey(), "projectKey2"));
  }

  @Test
  public void use_projectKey_to_disambiguate_when_multiple_projects_are_binded_on_one_bitbucketserver_repo() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setKey("B")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setKey("A")).getProjectDto();
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, project1, s -> s.setAlmRepo("projectKey2"), s -> s.setAlmSlug("repo-slug-2"));
    db.almSettings().insertBitbucketProjectAlmSetting(almSetting, project2, s -> s.setAlmRepo("projectKey2"), s -> s.setAlmSlug("repo-slug-2"));

    AlmIntegrations.SearchBitbucketserverReposWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(SearchBitbucketserverReposWsResponse.class);

    assertThat(response.getIsLastPage()).isTrue();
    assertThat(response.getRepositoriesList())
      .extracting(AlmIntegrations.BBSRepo::getId, AlmIntegrations.BBSRepo::getName, AlmIntegrations.BBSRepo::getSlug, AlmIntegrations.BBSRepo::getSqProjectKey,
        AlmIntegrations.BBSRepo::getProjectKey)
      .containsExactlyInAnyOrder(
        tuple(1L, "repoName1", "repo-slug-1", "", "projectKey1"),
        tuple(3L, "repoName2", "repo-slug-2", "A", "projectKey2"));
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
      .hasMessage("No personal access token found");
  }

  @Test
  public void fail_check_pat_alm_setting_not_found() {
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
  public void fail_when_not_logged_in() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "anyvalue")
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
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
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.2");
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleFormat()).isEqualTo("json");
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", true),
        tuple("projectName", false),
        tuple("repositoryName", false));
  }

  private Repository getGsonBBSRepo1() {
    Repository gsonBBSRepo1 = new Repository();
    gsonBBSRepo1.setId(1L);
    gsonBBSRepo1.setName("repoName1");
    Project project1 = new Project();
    project1.setName("projectName1");
    project1.setKey("projectKey1");
    project1.setId(2L);
    gsonBBSRepo1.setProject(project1);
    gsonBBSRepo1.setSlug("repo-slug-1");
    return gsonBBSRepo1;
  }

  private Repository getGsonBBSRepo2() {
    Repository gsonBBSRepo = new Repository();
    gsonBBSRepo.setId(3L);
    gsonBBSRepo.setName("repoName2");
    Project project = new Project();
    project.setName("projectName2");
    project.setKey("projectKey2");
    project.setId(4L);
    gsonBBSRepo.setProject(project);
    gsonBBSRepo.setSlug("repo-slug-2");
    return gsonBBSRepo;
  }

}
