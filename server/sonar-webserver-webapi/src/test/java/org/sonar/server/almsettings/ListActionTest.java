/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

package org.sonar.server.almsettings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmSettings;
import org.sonarqube.ws.AlmSettings.AlmSetting;
import org.sonarqube.ws.AlmSettings.ListWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.test.JsonAssert.assertJson;

public class ListActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null)));

  @Test
  public void list() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(ADMIN, project);
    AlmSettingDto githubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto githubAlmSetting2 = db.almSettings().insertGitHubAlmSetting();

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(AlmSettings.ListWsResponse.class);

    assertThat(response.getAlmSettingsList())
      .extracting(AlmSetting::getAlm, AlmSetting::getKey, AlmSetting::getUrl)
      .containsExactlyInAnyOrder(
        tuple(AlmSettings.Alm.github, githubAlmSetting1.getKey(), githubAlmSetting1.getUrl()),
        tuple(AlmSettings.Alm.github, githubAlmSetting2.getKey(), githubAlmSetting2.getUrl()));
  }

  @Test
  public void fail_when_project_does_not_exist() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(ADMIN, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("project", "unknown")
      .execute();
  }

  @Test
  public void fail_when_missing_administer_permission_on_project() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
  }

  @Test
  public void json_example() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(ADMIN, project);
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

    assertJson(response).isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isFalse();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("project", true));
  }

}
