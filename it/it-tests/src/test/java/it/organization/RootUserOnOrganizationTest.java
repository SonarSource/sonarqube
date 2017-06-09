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
package it.organization;

import com.sonar.orchestrator.Orchestrator;
import it.Category6Suite;
import java.sql.SQLException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sonarqube.ws.WsRoot;
import org.sonarqube.ws.client.WsClient;
import util.OrganizationRule;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.expectBadRequestError;
import static util.ItUtils.expectForbiddenError;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;

public class RootUserOnOrganizationTest {

  private static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  private static OrganizationRule organizationRule = new OrganizationRule(orchestrator);

  @ClassRule
  public static TestRule chain = RuleChain.outerRule(orchestrator)
    .around(organizationRule);

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  @Test
  public void system_administrator_is_flagged_as_root_when_he_enables_organization_support() {
    assertThat(newAdminWsClient(orchestrator).rootService().search().getRootsList())
      .extracting(WsRoot.Root::getLogin)
      .containsExactly(UserRule.ADMIN_LOGIN);
  }

  @Test
  public void a_root_can_flag_other_user_as_root() {
    userRule.createUser("bar", "foo");
    userRule.setRoot("bar");

    assertThat(newAdminWsClient(orchestrator).rootService().search().getRootsList())
      .extracting(WsRoot.Root::getLogin)
      .containsOnly(UserRule.ADMIN_LOGIN, "bar");
  }

  @Test
  public void last_root_can_not_be_unset_root() throws SQLException {
    expectBadRequestError(() -> newAdminWsClient(orchestrator).rootService().unsetRoot(UserRule.ADMIN_LOGIN));
  }

  @Test
  public void root_can_be_set_and_unset_via_web_services() {
    userRule.createUser("root1", "bar");
    userRule.createUser("root2", "bar");
    WsClient root1WsClient = newUserWsClient(orchestrator, "root1", "bar");
    WsClient root2WsClient = newUserWsClient(orchestrator, "root2", "bar");

    // non root can not set or unset root another user not itself
    expectForbiddenError(() -> root1WsClient.rootService().setRoot("root2"));
    expectForbiddenError(() -> root1WsClient.rootService().setRoot("root1"));
    expectForbiddenError(() -> root1WsClient.rootService().unsetRoot("root1"));
    expectForbiddenError(() -> root2WsClient.rootService().unsetRoot("root1"));
    expectForbiddenError(() -> root2WsClient.rootService().unsetRoot("root2"));
    // admin (the first root) sets root1 as root
    newAdminWsClient(orchestrator).rootService().setRoot("root1");
    // root1 can set root root2
    root1WsClient.rootService().setRoot("root2");
    // root2 can unset root root1
    root2WsClient.rootService().unsetRoot("root1");
    // root2 can unset root itself as it's not the last root
    root2WsClient.rootService().unsetRoot("root2");
  }

}
