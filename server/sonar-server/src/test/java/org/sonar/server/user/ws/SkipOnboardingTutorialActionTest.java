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
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SkipOnboardingTutorialActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsActionTester ws = new WsActionTester(new SkipOnboardingTutorialAction(userSession, db.getDbClient()));

  @Test
  public void mark_user_as_onboarded() {
    UserDto user = db.users().insertUser(u -> u
      .setOnboarded(false));
    userSession.logIn(user);

    call();

    UserDto userDto = selectUser(user.getLogin());
    assertThat(userDto.isOnboarded()).isEqualTo(true);
  }

  @Test
  public void does_nothing_if_user_already_onboarded() {
    UserDto user = db.users().insertUser(u -> u
      .setOnboarded(true));
    userSession.logIn(user);

    call();

    UserDto userDto = selectUser(user.getLogin());
    assertThat(userDto.isOnboarded()).isEqualTo(true);
    assertThat(userDto.getUpdatedAt()).isEqualTo(user.getUpdatedAt());
  }

  @Test
  public void fail_for_anonymous() {
    userSession.anonymous();
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call();
  }

  @Test
  public void fail_with_ISE_when_user_login_in_db_does_not_exist() {
    db.users().insertUser(usert -> usert.setLogin("another"));
    userSession.logIn("obiwan.kenobi");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("User login 'obiwan.kenobi' cannot be found");

    call();
  }

  @Test
  public void response_has_no_content() {
    UserDto user = db.users().insertUser(u -> u.setOnboarded(false));
    userSession.logIn(user);

    TestResponse response = call();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.since()).isEqualTo("6.5");
    assertThat(def.params()).isEmpty();
    assertThat(def.changelog()).isEmpty();
  }

  private TestResponse call() {
    return ws.newRequest().setMethod("POST").execute();
  }

  private UserDto selectUser(String userLogin) {
    UserDto userDto = db.getDbClient().userDao().selectByLogin(db.getSession(), userLogin);
    assertThat(userDto).isNotNull();
    return userDto;
  }

}
