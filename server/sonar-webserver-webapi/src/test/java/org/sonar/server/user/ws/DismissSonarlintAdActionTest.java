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
package org.sonar.server.user.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_DISMISS_SONARLINT_AD;

public class DismissSonarlintAdActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DismissNoticeAction dismissNoticeAction = new DismissNoticeAction(userSession, db.getDbClient());
  private final WsActionTester underTest = new WsActionTester(new DismissSonarlintAdAction(userSession, dismissNoticeAction));

  @Test
  public void test_definition() {
    WebService.Action definition = underTest.getDef();
    assertThat(definition.key()).isEqualTo(ACTION_DISMISS_SONARLINT_AD);
    assertThat(definition.description()).isEqualTo("Dismiss SonarLint advertisement. Deprecated since 9.6, replaced api/users/dismiss_notice");
    assertThat(definition.since()).isEqualTo("9.2");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).isEmpty();
    assertThat(definition.changelog()).isEmpty();
  }

  @Test
  public void endpoint_throw_exception_if_no_user_login() {
    final TestRequest request = underTest.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void calling_endpoint_should_set_sonarlint_ad_seen_true() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("obiwan.kenobi")
      .setName("Obiwan Kenobi")
      .setEmail(null));
    userSession.logIn(user);
    assertThat(db.properties().findFirstUserProperty(userSession.getUuid(), "user.dismissedNotices.sonarlintAd")).isEmpty();

    underTest.newRequest().execute();
    UserDto updatedUser = db.users().selectUserByLogin(user.getLogin()).get();
    assertThat(db.properties().findFirstUserProperty(userSession.getUuid(), "user.dismissedNotices.sonarlintAd")).isPresent();
  }
}
