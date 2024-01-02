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
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;

public class UpdateBitbucketActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final Encryption encryption = mock(Encryption.class);

  private WsActionTester ws = new WsActionTester(new UpdateBitbucketAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null),
      mock(MultipleAlmFeature.class))));

  @Test
  public void update() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://bitbucket.enterprise-unicorn.com")
      .setParam("personalAccessToken", "10987654321")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, s -> s.getDecryptedPersonalAccessToken(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://bitbucket.enterprise-unicorn.com", "10987654321"));
  }

  @Test
  public void update_with_new_key() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "Bitbucket Server - Infra Team")
      .setParam("url", "https://bitbucket.enterprise-unicorn.com")
      .setParam("personalAccessToken", "0123456789")
      .execute();
    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, s -> s.getDecryptedPersonalAccessToken(encryption))
      .containsOnly(tuple("Bitbucket Server - Infra Team", "https://bitbucket.enterprise-unicorn.com", "0123456789"));
  }

  @Test
  public void update_without_pat() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://bitbucket.enterprise-unicorn.com")
      .execute();
    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, s -> s.getDecryptedPersonalAccessToken(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://bitbucket.enterprise-unicorn.com", almSettingDto.getDecryptedPersonalAccessToken(encryption)));
  }

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", "unknown")
      .setParam("url", "https://bitbucket.enterprise-unicorn.com")
      .setParam("personalAccessToken", "0123456789")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("DevOps Platform setting with key 'unknown' cannot be found");
  }

  @Test
  public void fail_when_new_key_matches_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertBitbucketAlmSetting();
    AlmSettingDto almSetting2 = db.almSettings().insertBitbucketAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", almSetting1.getKey())
      .setParam("newKey", almSetting2.getKey())
      .setParam("url", "https://bitbucket.enterprise-unicorn.com")
      .setParam("personalAccessToken", "0123456789")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("An DevOps Platform setting with key '%s' already exists", almSetting2.getKey()));
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "Bitbucket Server - Infra Team")
      .setParam("url", "https://bitbucket.enterprise-unicorn.com")
      .setParam("personalAccessToken", "0123456789")
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true), tuple("newKey", false), tuple("url", true), tuple("personalAccessToken", false));
  }

}
