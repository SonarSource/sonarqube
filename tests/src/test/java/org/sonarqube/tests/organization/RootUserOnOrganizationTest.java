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
package org.sonarqube.tests.organization;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category6Suite;
import java.sql.SQLException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Session;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsRoot;
import org.sonarqube.ws.WsUsers;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.expectBadRequestError;
import static util.ItUtils.expectForbiddenError;

public class RootUserOnOrganizationTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void system_administrator_is_flagged_as_root_when_he_enables_organization_support() {
    assertThat(tester.wsClient().roots().search().getRootsList())
      .extracting(WsRoot.Root::getLogin)
      .containsExactly(UserRule.ADMIN_LOGIN);
  }

  @Test
  public void a_root_can_flag_other_user_as_root() {
    WsUsers.CreateWsResponse.User user = tester.users().generate();
    tester.wsClient().roots().setRoot(user.getLogin());

    assertThat(tester.wsClient().roots().search().getRootsList())
      .extracting(WsRoot.Root::getLogin)
      .containsExactlyInAnyOrder(UserRule.ADMIN_LOGIN, user.getLogin());
  }

  @Test
  public void last_root_can_not_be_unset_root() throws SQLException {
    expectBadRequestError(() -> tester.wsClient().roots().unsetRoot(UserRule.ADMIN_LOGIN));
  }

  @Test
  public void root_can_be_set_and_unset_via_web_services() {
    WsUsers.CreateWsResponse.User user1 = tester.users().generate();
    WsUsers.CreateWsResponse.User user2 = tester.users().generate();
    Session user1Session = tester.as(user1.getLogin());
    Session user2Session = tester.as(user2.getLogin());

    // non root can not set or unset root another user not itself
    expectForbiddenError(() -> user1Session.wsClient().roots().setRoot(user2.getLogin()));
    expectForbiddenError(() -> user1Session.wsClient().roots().setRoot(user1.getLogin()));
    expectForbiddenError(() -> user1Session.wsClient().roots().unsetRoot(user1.getLogin()));
    expectForbiddenError(() -> user2Session.wsClient().roots().unsetRoot(user1.getLogin()));
    expectForbiddenError(() -> user2Session.wsClient().roots().unsetRoot(user2.getLogin()));
    // admin (the first root) sets root1 as root
    tester.wsClient().roots().setRoot(user1.getLogin());
    // root1 can set root root2
    user1Session.wsClient().roots().setRoot(user2.getLogin());
    // root2 can unset root root1
    user2Session.wsClient().roots().unsetRoot(user1.getLogin());
    // root2 can unset root itself as it's not the last root
    user2Session.wsClient().roots().unsetRoot(user2.getLogin());
  }

}
