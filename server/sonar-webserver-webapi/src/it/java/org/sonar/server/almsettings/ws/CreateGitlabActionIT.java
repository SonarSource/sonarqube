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

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
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

public class CreateGitlabActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private static String GITLAB_URL = "gitlab.com/api/v4";

  private final Encryption encryption = mock(Encryption.class);
  private final MultipleAlmFeature multipleAlmFeature = mock(MultipleAlmFeature.class);

  private WsActionTester ws = new WsActionTester(new CreateGitlabAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null), multipleAlmFeature)));

  @Before
  public void before() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
  }

  @Test
  public void create_without_url() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("key", "Gitlab - Dev Team")
      .setParam("personalAccessToken", "98765432100");

    Assertions.assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'url' parameter is missing");
  }

  @Test
  public void create_with_url() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    ws.newRequest()
      .setParam("key", "Gitlab - Dev Team")
      .setParam("url", GITLAB_URL)
      .setParam("personalAccessToken", "98765432100")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, s -> s.getDecryptedPersonalAccessToken(encryption))
      .containsOnly(tuple("Gitlab - Dev Team", GITLAB_URL, "98765432100"));
  }

  @Test
  public void create_with_empty_url() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("key", "Gitlab - Dev Team")
      .setParam("url", "")
      .setParam("personalAccessToken", "98765432100");

    Assertions.assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'url' parameter is missing");
  }

  @Test
  public void fail_when_key_is_already_used() {
    when(multipleAlmFeature.isAvailable()).thenReturn(true);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto gitlabAlmSetting = db.almSettings().insertGitlabAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", gitlabAlmSetting.getKey())
      .setParam("personalAccessToken", "98765432100")
      .setParam("url", GITLAB_URL)
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(String.format("An DevOps Platform setting with key '%s' already exist", gitlabAlmSetting.getKey()));
  }

  @Test
  public void fail_when_no_multiple_instance_allowed() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    db.almSettings().insertGitlabAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", "anotherKey")
      .setParam("personalAccessToken", "98765432100")
      .setParam("url", GITLAB_URL)
      .execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("A GITLAB setting is already defined");
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", "Gitlab - Dev Team")
      .setParam("personalAccessToken", "98765432100")
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.changelog()).hasSize(1);
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true), tuple("personalAccessToken", true), tuple("url", true));
  }
}
