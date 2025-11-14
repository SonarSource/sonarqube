/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmSettings.CountBindingWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.test.JsonAssert.assertJson;

public class CountBindingActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new CountBindingAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null),
      mock(MultipleAlmFeature.class))));

  @Test
  public void count_github_binding() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(almSetting, project1);
    db.almSettings().insertGitHubProjectAlmSetting(almSetting, project2);

    CountBindingWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(CountBindingWsResponse.class);

    assertThat(response.getKey()).isEqualTo(almSetting.getKey());
    assertThat(response.getProjects()).isEqualTo(2);
  }

  @Test
  public void count_azure_binding() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertAzureProjectAlmSetting(almSetting, project1);

    CountBindingWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(CountBindingWsResponse.class);

    assertThat(response.getKey()).isEqualTo(almSetting.getKey());
    assertThat(response.getProjects()).isOne();
  }

  @Test
  public void fail_when_alm_setting_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("almSetting", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("DevOps Platform setting with key 'unknown' cannot be found");
  }

  @Test
  public void fail_when_missing_system_administer_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("almSetting", githubAlmSetting.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void json_example() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("GitHub Server - Dev Team")
        .setAppId("12345")
        .setPrivateKey("54684654"));
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, db.components().insertPrivateProject().getProjectDto());
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, db.components().insertPrivateProject().getProjectDto());
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, db.components().insertPrivateProject().getProjectDto());

    String response = ws.newRequest()
      .setParam("almSetting", githubAlmSetting.getKey())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("count_binding-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isFalse();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("almSetting", true));
  }

}
