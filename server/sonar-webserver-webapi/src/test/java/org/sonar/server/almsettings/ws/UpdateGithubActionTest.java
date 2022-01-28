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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeatureProvider;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;

public class UpdateGithubActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final Encryption encryption = mock(Encryption.class);

  private WsActionTester ws = new WsActionTester(new UpdateGithubAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null),
      mock(MultipleAlmFeatureProvider.class))));

  @Test
  public void update() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertGitHubAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://github.enterprise-unicorn.com")
      .setParam("appId", "54321")
      .setParam("privateKey", "10987654321")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://github.enterprise-unicorn.com", "54321", "10987654321", "client_1234", "client_so_secret"));
  }

  @Test
  public void update_url_with_trailing_slash() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertGitHubAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://github.enterprise-unicorn.com/")
      .setParam("appId", "54321")
      .setParam("privateKey", "10987654321")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://github.enterprise-unicorn.com", "54321", "10987654321", "client_1234", "client_so_secret"));
  }

  @Test
  public void update_with_new_key() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertGitHubAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "GitHub Server - Infra Team")
      .setParam("url", "https://github.enterprise-unicorn.com")
      .setParam("appId", "54321")
      .setParam("privateKey", "10987654321")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple("GitHub Server - Infra Team", "https://github.enterprise-unicorn.com", "54321", "10987654321", "client_1234", "client_so_secret"));
  }

  @Test
  public void update_without_private_key_nor_client_secret() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertGitHubAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://github.enterprise-unicorn.com/")
      .setParam("appId", "54321")
      .setParam("clientId", "client_1234")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://github.enterprise-unicorn.com", "54321",
        almSettingDto.getDecryptedPrivateKey(encryption), "client_1234", almSettingDto.getDecryptedClientSecret(encryption)));
  }

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("key", "unknown")
      .setParam("newKey", "GitHub Server - Infra Team")
      .setParam("url", "https://github.enterprise-unicorn.com")
      .setParam("appId", "54321")
      .setParam("privateKey", "10987654321")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret");

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("ALM setting with key 'unknown' cannot be found");
  }

  @Test
  public void fail_when_new_key_matches_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto almSetting2 = db.almSettings().insertGitHubAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("key", almSetting1.getKey())
      .setParam("newKey", almSetting2.getKey())
      .setParam("url", "https://github.enterprise-unicorn.com")
      .setParam("appId", "54321")
      .setParam("privateKey", "10987654321")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret");

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("An ALM setting with key '%s' already exists", almSetting2.getKey()));
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    AlmSettingDto almSettingDto = db.almSettings().insertGitHubAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "GitHub Server - Infra Team")
      .setParam("url", "https://github.enterprise-unicorn.com")
      .setParam("appId", "54321")
      .setParam("privateKey", "10987654321")
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
      .containsExactlyInAnyOrder(tuple("key", true), tuple("newKey", false), tuple("url", true), tuple("appId", true), tuple("privateKey", false), tuple("clientId", true),
        tuple("clientSecret", false));
  }

}
