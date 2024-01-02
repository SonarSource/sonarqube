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
package org.sonar.server.almsettings.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmSettings;
import org.sonarqube.ws.AlmSettings.AlmSetting;
import org.sonarqube.ws.AlmSettings.ListWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.test.JsonAssert.assertJson;

public class ListActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), mock(ResourceTypes.class));
  private final WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), userSession, componentFinder));

  @Test
  public void list() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto githubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto githubAlmSetting2 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto azureAlmSetting = db.almSettings().insertAzureAlmSetting();
    AlmSettingDto azureAlmSettingWithoutUrl = db.almSettings().insertAzureAlmSetting(s -> s.setUrl(null));
    AlmSettingDto gitlabAlmSetting = db.almSettings().insertGitlabAlmSetting();
    AlmSettingDto gitlabAlmSettingWithoutUrl = db.almSettings().insertGitlabAlmSetting(s -> s.setUrl(null));
    AlmSettingDto bitbucketAlmSetting = db.almSettings().insertBitbucketAlmSetting();
    AlmSettingDto bitbucketCloudAlmSetting = db.almSettings().insertBitbucketCloudAlmSetting();

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getAlmSettingsList())
      .extracting(AlmSetting::getAlm, AlmSetting::getKey, AlmSetting::hasUrl, AlmSetting::getUrl)
      .containsExactlyInAnyOrder(
        tuple(AlmSettings.Alm.github, githubAlmSetting1.getKey(), true, githubAlmSetting1.getUrl()),
        tuple(AlmSettings.Alm.github, githubAlmSetting2.getKey(), true, githubAlmSetting2.getUrl()),
        tuple(AlmSettings.Alm.azure, azureAlmSetting.getKey(), true, azureAlmSetting.getUrl()),
        tuple(AlmSettings.Alm.azure, azureAlmSettingWithoutUrl.getKey(), false, ""),
        tuple(AlmSettings.Alm.gitlab, gitlabAlmSetting.getKey(), true, gitlabAlmSetting.getUrl()),
        tuple(AlmSettings.Alm.gitlab, gitlabAlmSettingWithoutUrl.getKey(), false, ""),
        tuple(AlmSettings.Alm.bitbucket, bitbucketAlmSetting.getKey(), true, bitbucketAlmSetting.getUrl()),
        tuple(AlmSettings.Alm.bitbucketcloud, bitbucketCloudAlmSetting.getKey(), true,
          "https://bitbucket.org/" + bitbucketCloudAlmSetting.getAppId() + "/"));
  }

  @Test
  public void list_is_ordered_by_alm_key() {
    UserDto user = db.users().insertUser();
    db.components().insertPrivateProject();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    db.almSettings().insertGitHubAlmSetting(almSetting -> almSetting.setKey("GitHub1"));
    db.almSettings().insertGitHubAlmSetting(almSetting -> almSetting.setKey("GitHub2"));
    db.almSettings().insertAzureAlmSetting(almSetting -> almSetting.setKey("Azure"));
    db.almSettings().insertGitlabAlmSetting(almSetting -> almSetting.setKey("Gitlab"));
    db.almSettings().insertBitbucketAlmSetting(almSetting -> almSetting.setKey("Bitbucket"));

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getAlmSettingsList())
      .extracting(AlmSetting::getKey)
      .containsExactly("Azure", "Bitbucket", "GitHub1", "GitHub2", "Gitlab");
  }

  @Test
  public void fail_when_missing_create_project_permission() {
    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_project_does_not_exist() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProjectDto();
    userSession.logIn(user).addProjectPermission(ADMIN, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    TestRequest request = ws.newRequest().setParam("project", "unknown");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'unknown' not found");
  }

  @Test
  public void fail_when_missing_administer_permission_on_private_project() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProjectDto();
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    TestRequest request = ws.newRequest().setParam("project", project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_when_missing_administer_permission_on_public_project() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPublicProjectDto();
    userSession.logIn(user).addProjectPermission(SCAN, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    TestRequest request = ws.newRequest().setParam("project", project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void json_example_with_create_project() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    initAlmSetting();

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void json_example_with_administer_permission() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProjectDto();
    userSession.logIn(user).addProjectPermission(ADMIN, project);
    initAlmSetting();

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("list-example.json"));
  }

  private void initAlmSetting() {
    db.almSettings().insertGitHubAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("GitHub Server - Dev Team")
        .setUrl("https://github.enterprise.com"));
    db.almSettings().insertAzureAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("Azure Server - Dev Team")
        .setUrl("https://azure.com"));
    db.almSettings().insertBitbucketAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("Bitbucket Server - Dev Team")
        .setUrl("https://bitbucket.enterprise.com"));
    db.almSettings().insertBitbucketCloudAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("Bitbucket Cloud - Dev Team")
        .setAppId("workspace"));
    db.almSettings().insertGitlabAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("GitLab - Dev Team")
        .setUrl("https://www.gitlab.com/api/v4"));
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("project", false));
  }

}
