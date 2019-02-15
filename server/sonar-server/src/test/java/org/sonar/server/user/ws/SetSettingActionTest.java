/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.user.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPropertyDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SetSettingActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new SetSettingAction(db.getDbClient(), userSession));

  @Test
  public void set_new_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    ws.newRequest()
      .setParam("key", "notifications.optOut")
      .setParam("value", "true")
      .execute();

    assertThat(db.getDbClient().userPropertiesDao().selectByUser(db.getSession(), user))
      .extracting(UserPropertyDto::getKey, UserPropertyDto::getValue)
      .containsExactlyInAnyOrder(tuple("notifications.optOut", "true"));
  }

  @Test
  public void update_existing_setting() {
    UserDto user = db.users().insertUser();
    db.users().insertUserSetting(user, userSetting -> userSetting
      .setKey("notifications.optOut")
      .setValue("false"));
    userSession.logIn(user);

    ws.newRequest()
      .setParam("key", "notifications.optOut")
      .setParam("value", "true")
      .execute();

    assertThat(db.getDbClient().userPropertiesDao().selectByUser(db.getSession(), user))
      .extracting(UserPropertyDto::getKey, UserPropertyDto::getValue)
      .containsExactlyInAnyOrder(tuple("notifications.optOut", "true"));
  }

  @Test
  public void keep_existing_setting_when_setting_new_one() {
    UserDto user = db.users().insertUser();
    db.users().insertUserSetting(user, userSetting -> userSetting
      .setKey("notifications.readDate")
      .setValue("1234"));
    userSession.logIn(user);

    ws.newRequest()
      .setParam("key", "notifications.optOut")
      .setParam("value", "true")
      .execute();

    assertThat(db.getDbClient().userPropertiesDao().selectByUser(db.getSession(), user))
      .extracting(UserPropertyDto::getKey, UserPropertyDto::getValue)
      .containsExactlyInAnyOrder(
        tuple("notifications.readDate", "1234"),
        tuple("notifications.optOut", "true"));
  }

  @Test
  public void fail_when_not_authenticated() {
    expectedException.expect(UnauthorizedException.class);

    ws.newRequest()
      .setParam("key", "notifications.optOut")
      .setParam("value", "true")
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("set_setting");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.since()).isEqualTo("7.6");

    assertThat(definition.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired, WebService.Param::maximumLength)
      .containsOnly(
        tuple("key", true, 100),
        tuple("value", true, 4000));

    assertThat(definition.param("key").possibleValues()).containsExactlyInAnyOrder(
      "notifications.optOut",
      "notifications.readDate");
  }

}
