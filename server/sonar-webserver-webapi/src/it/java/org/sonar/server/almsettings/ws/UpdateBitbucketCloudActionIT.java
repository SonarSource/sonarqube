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
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateBitbucketCloudActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final Encryption encryption = mock(Encryption.class);

  private final WsActionTester ws = new WsActionTester(new UpdateBitbucketCloudAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), mock(ResourceTypes.class)),
      mock(MultipleAlmFeature.class))));

  @Test
  public void update() {
    when(encryption.isEncrypted(any())).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("workspace", "workspace")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getClientId,
        s -> s.getDecryptedClientSecret(encryption), AlmSettingDto::getAppId)
      .containsOnly(tuple(almSettingDto.getKey(), "id", "secret", "workspace"));
  }

  @Test
  public void update_with_new_key() {
    when(encryption.isEncrypted(any())).thenReturn(false);

    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "Bitbucket Server - Infra Team")
      .setParam("workspace", "workspace")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .execute();
    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getClientId,
        s -> s.getDecryptedClientSecret(encryption), AlmSettingDto::getAppId)
      .containsOnly(tuple("Bitbucket Server - Infra Team", "id", "secret", "workspace"));
  }

  @Test
  public void update_binding_without_changing_the_key() {
    when(encryption.isEncrypted(any())).thenReturn(false);

    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .setParam("newKey", almSetting.getKey())
      .setParam("workspace", "workspace")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getClientId,
        s -> s.getDecryptedClientSecret(encryption), AlmSettingDto::getAppId)
      .containsOnly(tuple(almSetting.getKey(), "id", "secret", "workspace"));
  }

  @Test
  public void update_without_secret() {
    when(encryption.isEncrypted(any())).thenReturn(false);

    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("workspace", "workspace")
      .setParam("clientId", "id")
      .execute();
    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getClientId,
        s -> s.getDecryptedClientSecret(encryption), AlmSettingDto::getAppId)
      .containsOnly(tuple(almSettingDto.getKey(), "id", almSettingDto.getDecryptedPrivateKey(encryption), "workspace"));
  }

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    TestRequest request = ws.newRequest()
      .setParam("key", "unknown")
      .setParam("workspace", "workspace")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform setting with key 'unknown' cannot be found");
  }

  @Test
  public void fail_when_new_key_matches_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertBitbucketAlmSetting();
    AlmSettingDto almSetting2 = db.almSettings().insertBitbucketAlmSetting();
    TestRequest request = ws.newRequest()
      .setParam("key", almSetting1.getKey())
      .setParam("newKey", almSetting2.getKey())
      .setParam("workspace", "workspace")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("An DevOps Platform setting with key '%s' already exists", almSetting2.getKey()));
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    TestRequest request = ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "Bitbucket Server - Infra Team")
      .setParam("workspace", "workspace")
      .setParam("clientId", "id")
      .setParam("clientSecret", "secret");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_workspace_id_format_is_incorrect() {
    String workspace = "workspace/name";
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("key", almSettingDto.getKey())
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
    String workspace = "work-space_123";
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("key", almSettingDto.getKey())
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
      .containsExactlyInAnyOrder(tuple("key", true), tuple("newKey", false), tuple("workspace", true),
        tuple("clientId", true), tuple("clientSecret", false));
  }

}
