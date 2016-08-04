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
package it.serverSystem;

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
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newWsClient;

/**
 * This class starts a new orchestrator on each test case
 */
public class RestartTest {

  Orchestrator orchestrator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TestRule globalTimeout = new DisableOnDebug(Timeout.seconds(300L));

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void restart_in_prod_mode_requires_admin_privileges_and_restarts_WebServer_and_ES() throws Exception {
    // server classloader locks Jar files on Windows
    if (!SystemUtils.IS_OS_WINDOWS) {
      orchestrator = Orchestrator.builderEnv()
        .setOrchestratorProperty("orchestrator.keepWorkspace", "true")
        .build();
      orchestrator.start();

      try {
        newWsClient(orchestrator).system().restart();
        fail();
      } catch (Exception e) {
        assertThat(e.getMessage()).contains("403");
      }

      newAdminWsClient(orchestrator).system().restart();
      WsResponse wsResponse = newAdminWsClient(orchestrator).wsConnector().call(new GetRequest("/api/system/status")).failIfNotSuccessful();
      assertThat(wsResponse.content()).contains("RESTARTING");

      // we just wait five seconds, for a lack of a better approach to waiting for the restart process to start in SQ
      Thread.sleep(5000);

      assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()))
        .contains("SonarQube restart requested by admin");
    }
  }

  /**
   * SONAR-4843
   */
  @Test
  public void restart_on_dev_mode() throws Exception {
    // server classloader locks Jar files on Windows
    if (!SystemUtils.IS_OS_WINDOWS) {
      orchestrator = Orchestrator.builderEnv()
        .setServerProperty("sonar.web.dev", "true")
        .build();
      orchestrator.start();

      newAdminWsClient(orchestrator).system().restart();
      assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()))
        .contains("Fast restarting WebServer...")
        .contains("WebServer restarted");
    }
  }
}
