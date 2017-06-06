/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SkipOnboardingTutorialActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_have_a_good_definition() {
    WsActionTester ws = new WsActionTester(new SkipOnboardingTutorialAction(userSession, dbTester.getDbClient()));
    WebService.Action def = ws.getDef();
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.since()).isEqualTo("6.5");
    assertThat(def.params()).isEmpty();
  }

  @Test
  public void should_fail_for_anonymous() {
    WsActionTester ws = new WsActionTester(new SkipOnboardingTutorialAction(userSession, dbTester.getDbClient()));
    TestRequest request = ws.newRequest().setMethod("POST");

    thrown.expect(UnauthorizedException.class);
    thrown.expectMessage("Authentication is required");

    request.execute();
  }

  @Test
  public void should_return_silently_if_user_is_logged_in() {
    UserDto user = dbTester.users().insertUser();
    userSession.logIn(user);
    WsActionTester ws = new WsActionTester(new SkipOnboardingTutorialAction(userSession, dbTester.getDbClient()));
    TestRequest request = ws.newRequest().setMethod("POST");

    TestResponse response = request.execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void should_mark_user_as_onboarded() {
    UserDto user = dbTester.users().insertUser();
    userSession.logIn(user);
    WsActionTester ws = new WsActionTester(new SkipOnboardingTutorialAction(userSession, dbTester.getDbClient()));
    TestRequest request = ws.newRequest().setMethod("POST");
    dbTester.users().setOnboarded(user, true);
    assertThat(dbTester.getDbClient().userDao().selectOnboarded(dbTester.getSession(), user)).isTrue();

    TestResponse response = request.execute();

    assertThat(dbTester.getDbClient().userDao().selectOnboarded(dbTester.getSession(), user)).isFalse();
  }
}
