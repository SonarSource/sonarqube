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
package org.sonar.server.almintegration.ws;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class SetPatActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final WsActionTester ws = new WsActionTester(new SetPatAction(db.getDbClient(), userSession));

  @Test
  public void set_new_azuredevops_pat() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("pat", "12345678987654321")
      .execute();

    Optional<AlmPatDto> actualAlmPat = db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), user.getUuid(), almSetting);
    assertThat(actualAlmPat).isPresent();
    assertThat(actualAlmPat.get().getPersonalAccessToken()).isEqualTo("12345678987654321");
    assertThat(actualAlmPat.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(actualAlmPat.get().getAlmSettingUuid()).isEqualTo(almSetting.getUuid());
  }

  @Test
  public void set_new_bitbucketserver_pat() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("pat", "12345678987654321")
      .execute();

    Optional<AlmPatDto> actualAlmPat = db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), user.getUuid(), almSetting);
    assertThat(actualAlmPat).isPresent();
    assertThat(actualAlmPat.get().getPersonalAccessToken()).isEqualTo("12345678987654321");
    assertThat(actualAlmPat.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(actualAlmPat.get().getAlmSettingUuid()).isEqualTo(almSetting.getUuid());
  }

  @Test
  public void set_new_bitbucketcloud_pat() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    String pat = "12345678987654321";
    String username = "test-user";

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("pat", pat)
      .setParam("username", username)
      .execute();

    Optional<AlmPatDto> actualAlmPat = db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), user.getUuid(), almSetting);
    assertThat(actualAlmPat).isPresent();
    assertThat(actualAlmPat.get().getPersonalAccessToken()).isEqualTo(CredentialsEncoderHelper.encodeCredentials(ALM.BITBUCKET_CLOUD, pat, username));
    assertThat(actualAlmPat.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(actualAlmPat.get().getAlmSettingUuid()).isEqualTo(almSetting.getUuid());
  }

  @Test
  public void set_new_gitlab_pat() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("pat", "12345678987654321")
      .execute();

    Optional<AlmPatDto> actualAlmPat = db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), user.getUuid(), almSetting);
    assertThat(actualAlmPat).isPresent();
    assertThat(actualAlmPat.get().getPersonalAccessToken()).isEqualTo("12345678987654321");
    assertThat(actualAlmPat.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(actualAlmPat.get().getAlmSettingUuid()).isEqualTo(almSetting.getUuid());
  }

  @Test
  public void set_existing_pat() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    db.almPats().insert(p -> p.setUserUuid(user.getUuid()), p -> p.setAlmSettingUuid(almSetting.getUuid()));

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("pat", "newtoken")
      .execute();

    Optional<AlmPatDto> actualAlmPat = db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), user.getUuid(), almSetting);
    assertThat(actualAlmPat).isPresent();
    assertThat(actualAlmPat.get().getPersonalAccessToken()).isEqualTo("newtoken");
    assertThat(actualAlmPat.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(actualAlmPat.get().getAlmSettingUuid()).isEqualTo(almSetting.getUuid());
  }

  @Test
  public void fail_when_bitbucketcloud_without_username() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", almSetting.getKey())
        .setParam("pat", "12345678987654321")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Username cannot be null for Bitbucket Cloud");
  }

  @Test
  public void fail_when_alm_setting_unknow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "notExistingKey")
        .setParam("pat", "12345678987654321")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting 'notExistingKey' not found");
  }

  @Test
  public void fail_when_alm_setting_not_bitbucket_server_nor_gitlab() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", almSetting.getKey())
        .setParam("pat", "12345678987654321")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only Azure DevOps, Bitbucket Server, GitLab and Bitbucket Cloud Settings are supported.");
  }

  @Test
  public void fail_when_not_logged_in() {
    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.2");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("almSetting", true), tuple("pat", true), tuple("username", false));
  }

}
