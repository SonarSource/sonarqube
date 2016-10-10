/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.root;

import com.sonar.orchestrator.Orchestrator;
import it.Category3Suite;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsRoot;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.newWsClient;

public class RootIt {
  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;
  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  @After
  public void tearDown() throws Exception {
    userRule.resetUsers();
  }

  @Test
  public void by_default_admin_is_the_only_root() {
    // must be root to call search WS
    verifyHttpError(() -> newWsClient(orchestrator).rootService().search(), 403);

    assertThat(newAdminWsClient(orchestrator).rootService().search().getRootsList())
      .extracting(WsRoot.Root::getLogin)
      .containsOnly(UserRule.ADMIN_LOGIN);

    userRule.createUser("bar", "foo");
    userRule.setRoot("bar");

    assertThat(newAdminWsClient(orchestrator).rootService().search().getRootsList())
      .extracting(WsRoot.Root::getLogin)
      .containsOnly(UserRule.ADMIN_LOGIN, "bar");
  }

  @Test
  public void last_root_can_not_be_unset_root() throws SQLException {
    try (Connection connection = orchestrator.getDatabase().openConnection();
      PreparedStatement preparedStatement = createSelectActiveRootUsers(connection);
      ResultSet resultSet = preparedStatement.executeQuery()) {
      assertThat(resultSet.next()).as("There should be active root user").isTrue();
      assertThat(resultSet.getString(1)).isEqualTo(UserRule.ADMIN_LOGIN);
      assertThat(resultSet.next()).as("There shouldn't be more than one active root user").isFalse();
    }

    verifyHttpError(() -> newAdminWsClient(orchestrator).rootService().unsetRoot(UserRule.ADMIN_LOGIN), 400);
  }

  private static void verifyHttpError(Runnable runnable, int expectedErrorCode) {
    try {
      runnable.run();
      fail("Ws Call should have failed with http code " + expectedErrorCode);
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(expectedErrorCode);
    }
  }

  @Test
  public void root_can_be_set_and_unset_via_web_services() {
    userRule.createUser("root1", "bar");
    userRule.createUser("root2", "bar");
    WsClient root1WsClient = newUserWsClient(orchestrator, "root1", "bar");
    WsClient root2WsClient = newUserWsClient(orchestrator, "root2", "bar");

    // non root can not set or unset root another user not itself
    verifyHttpError(() -> root1WsClient.rootService().setRoot("root2"), 403);
    verifyHttpError(() -> root1WsClient.rootService().setRoot("root1"), 403);
    verifyHttpError(() -> root1WsClient.rootService().unsetRoot("root1"), 403);
    verifyHttpError(() -> root2WsClient.rootService().unsetRoot("root1"), 403);
    verifyHttpError(() -> root2WsClient.rootService().unsetRoot("root2"), 403);
    // admin (the first root) sets root1 as root
    newAdminWsClient(orchestrator).rootService().setRoot("root1");
    // root1 can set root root2
    root1WsClient.rootService().setRoot("root2");
    // root2 can unset root root1
    root2WsClient.rootService().unsetRoot("root1");
    // root2 can unset root itself as it's not the last root
    root2WsClient.rootService().unsetRoot("root2");
  }

  private static PreparedStatement createSelectActiveRootUsers(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement("select login from users where is_root = ? and active = ?");
    preparedStatement.setBoolean(1, true);
    preparedStatement.setBoolean(2, true);
    return preparedStatement;
  }
}
