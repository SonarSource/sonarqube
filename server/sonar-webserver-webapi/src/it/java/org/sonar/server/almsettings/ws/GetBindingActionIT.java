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
package org.sonar.server.almsettings.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmSettings;
import org.sonarqube.ws.AlmSettings.GetBindingWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.test.JsonAssert.assertJson;

public class GetBindingActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new GetBindingAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null)));

  private UserDto user;
  private ProjectDto project;

  @Before
  public void before() {
    user = db.users().insertUser();
    project = db.components().insertPrivateProject().getProjectDto();
  }

  @Test
  public void get_github_project_binding() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto githubProjectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response.getAlm()).isEqualTo(AlmSettings.Alm.github);
    assertThat(response.getKey()).isEqualTo(githubAlmSetting.getKey());
    assertThat(response.getRepository()).isEqualTo(githubProjectAlmSetting.getAlmRepo());
    assertThat(response.getUrl()).isEqualTo(githubAlmSetting.getUrl());
    assertThat(response.getSummaryCommentEnabled()).isTrue();
  }

  @Test
  public void get_azure_project_binding() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertAzureMonoRepoProjectAlmSetting(almSetting, project);

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response.getAlm()).isEqualTo(AlmSettings.Alm.azure);
    assertThat(response.getKey()).isEqualTo(almSetting.getKey());
    assertThat(response.getUrl()).isEqualTo(almSetting.getUrl());
    assertThat(response.getRepository()).isEqualTo(projectAlmSettingDto.getAlmRepo());
    assertThat(response.getSlug()).isEqualTo(projectAlmSettingDto.getAlmSlug());
    assertThat(response.hasSummaryCommentEnabled()).isFalse();
    assertThat(response.getMonorepo()).isTrue();
  }

  @Test
  public void get_gitlab_project_binding() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almSettings().insertGitlabProjectAlmSetting(almSetting, project);

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response.getAlm()).isEqualTo(AlmSettings.Alm.gitlab);
    assertThat(response.getKey()).isEqualTo(almSetting.getKey());
    assertThat(response.hasRepository()).isFalse();
    assertThat(response.getUrl()).isEqualTo(almSetting.getUrl());
    assertThat(response.hasUrl()).isTrue();
    assertThat(response.hasSummaryCommentEnabled()).isFalse();
  }

  @Test
  public void get_bitbucket_project_binding() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertBitbucketProjectAlmSetting(almSetting, project);

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response.getAlm()).isEqualTo(AlmSettings.Alm.bitbucket);
    assertThat(response.getKey()).isEqualTo(almSetting.getKey());
    assertThat(response.getRepository()).isEqualTo(projectAlmSettingDto.getAlmRepo());
    assertThat(response.getUrl()).isEqualTo(almSetting.getUrl());
    assertThat(response.getSlug()).isEqualTo(projectAlmSettingDto.getAlmSlug());
    assertThat(response.hasSummaryCommentEnabled()).isFalse();
  }

  @Test
  public void fail_when_project_does_not_exist() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_missing_browse_permission_on_project() {
    userSession.logIn(user);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void json_example() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("GitHub Server - Dev Team")
        .setUrl("https://github.enterprise.com")
        .setAppId("12345")
        .setPrivateKey("54684654"));
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project, projectAlmSetting -> projectAlmSetting.setAlmRepo("team/project"));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("get_binding-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("project", true));
  }

}
