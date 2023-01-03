/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class SetPatActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
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
  public void fail_when_alm_setting_unknow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("ALM Setting 'notExistingKey' not found");

    ws.newRequest()
      .setParam("almSetting", "notExistingKey")
      .setParam("pat", "12345678987654321")
      .execute();
  }

  @Test
  public void fail_when_alm_setting_not_bitbucket_server_nor_gitlab() {
    UserDto user = db.users().insertUser();
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only Azure DevOps, Bibucket Server and GitLab ALM Settings are supported.");

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("pat", "12345678987654321")
      .execute();
  }

  @Test
  public void fail_when_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest().execute();
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.2");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("almSetting", true), tuple("pat", true));
  }

}
