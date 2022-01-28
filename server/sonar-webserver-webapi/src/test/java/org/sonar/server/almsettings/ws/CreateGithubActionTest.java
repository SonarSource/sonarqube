/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeatureProvider;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateGithubActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final Encryption encryption = mock(Encryption.class);
  private final MultipleAlmFeatureProvider multipleAlmFeatureProvider = mock(MultipleAlmFeatureProvider.class);

  private WsActionTester ws = new WsActionTester(new CreateGithubAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null),
      multipleAlmFeatureProvider)));

  @Before
  public void before() {
    when(multipleAlmFeatureProvider.enabled()).thenReturn(false);
  }

  @Test
  public void create() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    ws.newRequest()
      .setParam("key", "GitHub Server - Dev Team")
      .setParam("url", "https://github.enterprise.com")
      .setParam("appId", "12345")
      .setParam("privateKey", "678910")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple("GitHub Server - Dev Team", "https://github.enterprise.com", "12345", "678910", "client_1234", "client_so_secret"));
  }

  @Test
  public void remove_trailing_slash() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    ws.newRequest()
      .setParam("key", "GitHub Server - Dev Team")
      .setParam("url", "https://github.enterprise.com/")
      .setParam("appId", "12345")
      .setParam("privateKey", "678910")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple("GitHub Server - Dev Team", "https://github.enterprise.com", "12345", "678910", "client_1234", "client_so_secret"));
  }

  @Test
  public void fail_when_key_is_already_used() {
    when(multipleAlmFeatureProvider.enabled()).thenReturn(true);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto gitHubAlmSetting = db.almSettings().insertGitHubAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("key", gitHubAlmSetting.getKey())
      .setParam("url", "https://github.enterprise.com")
      .setParam("appId", "12345")
      .setParam("privateKey", "678910")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret");

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(String.format("An ALM setting with key '%s' already exist", gitHubAlmSetting.getKey()));
  }

  @Test
  public void fail_when_no_multiple_instance_allowed() {
    when(multipleAlmFeatureProvider.enabled()).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    db.almSettings().insertGitHubAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("key", "key")
      .setParam("url", "https://github.enterprise.com")
      .setParam("appId", "12345")
      .setParam("privateKey", "678910")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret");

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("A GITHUB setting is already defined");
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest()
      .setParam("key", "GitHub Server - Dev Team")
      .setParam("url", "https://github.enterprise.com")
      .setParam("appId", "12345")
      .setParam("privateKey", "678910")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret");

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true), tuple("url", true), tuple("appId", true), tuple("privateKey", true), tuple("clientId", true), tuple("clientSecret", true));
  }
}
