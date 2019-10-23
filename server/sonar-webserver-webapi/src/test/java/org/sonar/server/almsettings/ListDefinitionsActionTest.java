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
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmSettings.AlmSettingGithub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.AlmSettings.ListDefinitionsWsResponse;

public class ListDefinitionsActionTest {

  @Rule
  public ExpectedException expectedException = none();
  @Rule
  public UserSessionRule userSession = standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new ListDefinitionsAction(db.getDbClient(), userSession));

  @Test
  public void list_github_settings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto almSetting2 = db.almSettings().insertGitHubAlmSetting();

    ListDefinitionsWsResponse wsResponse = ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);

    assertThat(wsResponse.getGithubList())
      .extracting(AlmSettingGithub::getKey, AlmSettingGithub::getUrl, AlmSettingGithub::getAppId, AlmSettingGithub::getPrivateKey)
      .containsExactlyInAnyOrder(
        tuple(almSetting1.getKey(), almSetting1.getUrl(), almSetting1.getAppId(), almSetting1.getPrivateKey()),
        tuple(almSetting2.getKey(), almSetting2.getUrl(), almSetting2.getAppId(), almSetting2.getPrivateKey()));
  }

  @Test
  public void return_empty_list_when_no_settings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    ListDefinitionsWsResponse wsResponse = ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);

    assertThat(wsResponse.getGithubList()).isEmpty();
  }

  @Test
  public void fail_when_user_is_not_system_administrator() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    db.almSettings().insertGitHubAlmSetting();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);
  }

  @Test
  public void json_example() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    db.almSettings().insertGitHubAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("GitHub Server - Dev Team")
        .setUrl("https://github.enterprise.com")
        .setAppId("12345")
        .setPrivateKey("54684654"));
    db.almSettings().insertAzureAlmSetting(
      a -> a.setKey("Azure Devops Server - Dev Team")
      .setPersonalAccessToken("12345")
    );
    db.almSettings().insertBitbucketAlmSetting(
      a -> a.setKey("Bitbucket Server - Dev Team")
        .setUrl("https://bitbucket.enterprise.com")
        .setPersonalAccessToken("abcdef")
    );

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("list_definitions-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.params()).isEmpty();
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
  }
}
