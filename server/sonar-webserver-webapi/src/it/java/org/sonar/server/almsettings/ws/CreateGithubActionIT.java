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

import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
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

public class CreateGithubActionIT {

  private static final String APP_ID = "12345";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final Encryption encryption = mock(Encryption.class);
  private final MultipleAlmFeature multipleAlmFeature = mock(MultipleAlmFeature.class);

  private final WsActionTester ws = new WsActionTester(new CreateGithubAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), mock(ComponentTypes.class)),
      multipleAlmFeature)));

  @Before
  public void setUp() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
  }

  @Test
  public void create() {
    buildTestRequest().execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption), s -> s.getDecryptedWebhookSecret(encryption))
      .containsOnly(tuple("GitHub Server - Dev Team", "https://github.enterprise.com", APP_ID, "678910", "client_1234", "client_so_secret", null));
  }

  private TestRequest buildTestRequest() {
    return ws.newRequest()
      .setParam("key", "GitHub Server - Dev Team")
      .setParam("url", "https://github.enterprise.com")
      .setParam("appId", APP_ID)
      .setParam("privateKey", "678910")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret");
  }

  @Test
  public void create_withWebhookSecret() {
    buildTestRequest().setParam("webhookSecret", "webhook_secret").execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption), s -> s.getDecryptedWebhookSecret(encryption))
      .containsOnly(tuple("GitHub Server - Dev Team", "https://github.enterprise.com", "12345", "678910", "client_1234", "client_so_secret", "webhook_secret"));
  }

  @Test
  public void create_withEmptyWebhookSecret_shouldNotPersist() {
    buildTestRequest().setParam("webhookSecret", "").execute();

    assertThat(db.getDbClient().almSettingDao().selectByAlmAndAppId(db.getSession(), ALM.GITHUB, APP_ID))
      .map(almSettingDto -> almSettingDto.getDecryptedWebhookSecret(encryption))
      .isEmpty();
  }

  @Test
  public void remove_trailing_slash() {

    buildTestRequest().setParam("url", "https://github.enterprise.com/").execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple("GitHub Server - Dev Team", "https://github.enterprise.com", APP_ID, "678910", "client_1234", "client_so_secret"));
  }

  @Test
  public void fail_when_key_is_already_used() {
    when(multipleAlmFeature.isAvailable()).thenReturn(true);
    AlmSettingDto gitHubAlmSetting = db.almSettings().insertGitHubAlmSetting();

    TestRequest request = buildTestRequest().setParam("key", gitHubAlmSetting.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(String.format("An DevOps Platform setting with key '%s' already exist", gitHubAlmSetting.getKey()));
  }

  @Test
  public void fail_when_no_multiple_instance_allowed() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    db.almSettings().insertGitHubAlmSetting();

    TestRequest request = buildTestRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("A GITHUB setting is already defined");
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = buildTestRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("key", true),
        tuple("url", true),
        tuple("appId", true),
        tuple("privateKey", true),
        tuple("clientId", true),
        tuple("clientSecret", true),
        tuple("webhookSecret", false));
  }

  @Test
  public void definition_shouldHaveChangeLog() {
    assertThat(ws.getDef().changelog()).extracting(Change::getVersion, Change::getDescription).containsExactly(
      new Tuple("9.7", "Optional parameter 'webhookSecret' was added")
    );
  }
}
