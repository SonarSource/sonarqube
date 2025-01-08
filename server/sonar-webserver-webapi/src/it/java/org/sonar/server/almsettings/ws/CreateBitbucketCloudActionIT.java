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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateBitbucketCloudActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final Encryption encryption = mock(Encryption.class);
  private final MultipleAlmFeature multipleAlmFeature = mock(MultipleAlmFeature.class);

  private WsActionTester ws = new WsActionTester(new CreateBitbucketCloudAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null),
      multipleAlmFeature)));

  @Before
  public void before() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
  }

  @Test
  public void create() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    ws.newRequest()
      .setParam("key", "Bitbucket Server - Dev Team")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .setParam("workspace", "workspace1")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption), AlmSettingDto::getAppId)
      .containsOnly(tuple("Bitbucket Server - Dev Team", "id", "secret", "workspace1"));
  }

  @Test
  public void fail_when_key_is_already_used() {
    when(multipleAlmFeature.isAvailable()).thenReturn(true);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto bitbucketAlmSetting = db.almSettings().insertBitbucketAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", bitbucketAlmSetting.getKey())
      .setParam("workspace", "workspace1")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(String.format("An DevOps Platform setting with key '%s' already exist", bitbucketAlmSetting.getKey()));
  }

  @Test
  public void fail_when_no_multiple_instance_allowed() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    db.almSettings().insertBitbucketCloudAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", "otherKey")
      .setParam("workspace", "workspace1")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("A BITBUCKET_CLOUD setting is already defined");
  }

  @Test
  public void fail_when_no_multiple_instance_allowed_and_bitbucket_server_exists() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    db.almSettings().insertBitbucketAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", "otherKey")
      .setParam("workspace", "workspace1")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("A BITBUCKET setting is already defined");
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", "Bitbucket Server - Dev Team")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .setParam("workspace", "workspace1")
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_workspace_id_format_is_incorrect() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    String workspace = "workspace/name";
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("key", "another new key")
      .setParam("workspace", workspace)
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(String.format(
        "Workspace ID '%s' has an incorrect format. Should only contain lowercase letters, numbers, dashes, and underscores.",
        workspace
      ));
  }

  @Test
  public void do_not_fail_when_workspace_id_format_is_correct() {
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    String workspace = "work-space_123";
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("key", "yet another new key")
      .setParam("workspace", workspace)
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret");

    assertThatNoException().isThrownBy(request::execute);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.7");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true), tuple("clientId", true), tuple("clientSecret", true), tuple("workspace", true));
  }
}
