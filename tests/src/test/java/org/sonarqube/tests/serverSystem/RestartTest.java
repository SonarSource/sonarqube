/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.System;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newWsClient;

/**
 * This class starts a new orchestrator on each test case
 */
public class RestartTest {

  private Orchestrator orchestrator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(900));

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void restart_in_prod_mode_requires_sysadmin_permission_and_restarts() throws Exception {
    // server classloader locks Jar files on Windows
    if (!SystemUtils.IS_OS_WINDOWS) {
      orchestrator = Orchestrator.builderEnv()
        .setOrchestratorProperty("orchestrator.keepWorkspace", "true")
        .build();
      orchestrator.start();

      verifyFailWith403(() -> newWsClient(orchestrator).system().restart());

      createNonSystemAdministrator("john", "doe");
      verifyFailWith403(() -> ItUtils.newUserWsClient(orchestrator, "john", "doe").system().restart());

      createSystemAdministrator("big", "boss");
      ItUtils.newUserWsClient(orchestrator, "big", "boss").system().restart();
      assertThat(newAdminWsClient(orchestrator).system().status().getStatus()).isEqualTo(System.Status.RESTARTING);

      // we just wait five seconds, for a lack of a better approach to waiting for the restart process to start in SQ
      Thread.sleep(5000);

      assertThat(FileUtils.readFileToString(orchestrator.getServer().getWebLogs()))
        .contains("SonarQube restart requested by big");
    }
  }

  private static void verifyFailWith403(Runnable runnable) {
    try {
      runnable.run();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("403");
    }
  }

  private void createSystemAdministrator(String login, String password) {
    WsClient wsClient = newAdminWsClient(orchestrator);
    createNonSystemAdministrator(wsClient, login, password);
    wsClient.permissions().addUser(new AddUserRequest().setLogin(login).setPermission("admin"));
  }

  private void createNonSystemAdministrator(String login, String password) {
    createNonSystemAdministrator(newAdminWsClient(orchestrator), login, password);
  }

  private static void createNonSystemAdministrator(WsClient wsClient, String login, String password) {
    wsClient.wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", login)
        .setParam("password", password));
  }
}
