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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmSettings;
import org.sonarqube.ws.AlmSettings.AlmSettingAzure;
import org.sonarqube.ws.AlmSettings.AlmSettingGithub;
import org.sonarqube.ws.AlmSettings.AlmSettingGitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.AlmSettings.ListDefinitionsWsResponse;

public class ListDefinitionsActionTest {

  @Rule
  public UserSessionRule userSession = standalone();

  private final System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final WsActionTester ws = new WsActionTester(new ListDefinitionsAction(db.getDbClient(), userSession));

  @Test
  public void list_github_settings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertGitHubAlmSetting(s -> s.setClientId(""));
    AlmSettingDto almSetting2 = db.almSettings().insertGitHubAlmSetting(alm -> alm.setClientId("client_id").setClientSecret("client_secret"));

    ListDefinitionsWsResponse wsResponse = ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);

    assertThat(wsResponse.getGithubList())
      .extracting(AlmSettingGithub::getKey, AlmSettingGithub::getUrl, AlmSettingGithub::getAppId, AlmSettingGithub::getClientId)
      .containsExactlyInAnyOrder(
        tuple(almSetting1.getKey(), almSetting1.getUrl(), almSetting1.getAppId(), ""),
        tuple(almSetting2.getKey(), almSetting2.getUrl(), almSetting2.getAppId(), "client_id"));
  }

  @Test
  public void list_gitlab_settings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertGitlabAlmSetting();
    AlmSettingDto almSetting2 = db.almSettings().insertGitlabAlmSetting(setting -> setting.setUrl(null));

    ListDefinitionsWsResponse wsResponse = ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);

    assertThat(wsResponse.getGitlabList())
      .extracting(AlmSettingGitlab::getKey, AlmSettingGitlab::getUrl)
      .containsExactlyInAnyOrder(
        tuple(almSetting1.getKey(), almSetting1.getUrl()),
        tuple(almSetting2.getKey(), ""));
  }

  @Test
  public void list_azure_settings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertAzureAlmSetting();
    AlmSettingDto almSetting2 = db.almSettings().insertAzureAlmSetting(setting -> setting.setUrl(null));

    ListDefinitionsWsResponse wsResponse = ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);

    assertThat(wsResponse.getAzureList())
      .extracting(AlmSettingAzure::getKey, AlmSettingAzure::getUrl)
      .containsExactlyInAnyOrder(
        tuple(almSetting1.getKey(), almSetting1.getUrl()),
        tuple(almSetting2.getKey(), ""));
  }

  @Test
  public void list_bitbucket_cloud_settings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertBitbucketCloudAlmSetting(alm -> alm.setClientId("1").setClientSecret("2"));
    AlmSettingDto almSetting2 = db.almSettings().insertBitbucketCloudAlmSetting(alm -> alm.setClientId("client_id").setClientSecret("client_secret"));

    ListDefinitionsWsResponse wsResponse = ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);

    assertThat(wsResponse.getBitbucketcloudList())
      .extracting(AlmSettings.AlmSettingBitbucketCloud::getKey, AlmSettings.AlmSettingBitbucketCloud::getClientId)
      .containsExactlyInAnyOrder(
        tuple(almSetting1.getKey(), "1"),
        tuple(almSetting2.getKey(), "client_id"));
  }

  @Test
  public void list_is_ordered_by_create_date() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    when(system2.now()).thenReturn(10_000_000_000L);
    AlmSettingDto almSetting1 = db.almSettings().insertGitHubAlmSetting();
    when(system2.now()).thenReturn(30_000_000_000L);
    AlmSettingDto almSetting2 = db.almSettings().insertGitHubAlmSetting();
    when(system2.now()).thenReturn(20_000_000_000L);
    AlmSettingDto almSetting3 = db.almSettings().insertGitHubAlmSetting();

    ListDefinitionsWsResponse wsResponse = ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class);

    assertThat(wsResponse.getGithubList())
      .extracting(AlmSettingGithub::getKey)
      .containsExactly(almSetting1.getKey(), almSetting3.getKey(), almSetting2.getKey());
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

    assertThatThrownBy(() -> ws.newRequest().executeProtobuf(ListDefinitionsWsResponse.class))
      .isInstanceOf(ForbiddenException.class);
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
        .setPrivateKey("54684654")
        .setClientId("client_id")
        .setClientSecret("client_secret"));
    db.almSettings().insertAzureAlmSetting(
      a -> a.setKey("Azure Devops Server - Dev Team")
        .setPersonalAccessToken("12345")
        .setUrl("https://ado.sonarqube.com/"));
    db.almSettings().insertBitbucketAlmSetting(
      a -> a.setKey("Bitbucket Server - Dev Team")
        .setUrl("https://bitbucket.enterprise.com")
        .setPersonalAccessToken("abcdef"));
    db.almSettings().insertGitlabAlmSetting(
      a -> a.setKey("Gitlab - Dev Team")
        .setPersonalAccessToken("12345"));

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("list_definitions-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.params()).isEmpty();
    assertThat(def.changelog()).hasSize(3);
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
  }
}
