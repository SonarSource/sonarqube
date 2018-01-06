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
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.System;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.newAdminWsClient;

/**
 * This class start a new orchestrator on each test case
 */
public class ServerSystemRestartingOrchestrator {

  Orchestrator orchestrator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  /**
   * SONAR-3516
   */
  @Test
  public void check_minimal_sonar_version_at_startup() throws Exception {
    try {
      orchestrator = Orchestrator.builderEnv()
        .addPlugin(FileLocation.of(new File(ServerSystemRestartingOrchestrator.class.getResource("/serverSystem/ServerSystemTest/incompatible-plugin-1.0.jar").toURI())))
        .build();
      orchestrator.start();
      fail();
    } catch (Exception e) {
      assertThat(FileUtils.readFileToString(orchestrator.getServer().getWebLogs())).contains(
        "Plugin incompatible-plugin [incompatibleplugin] requires at least SonarQube 100");
    }
  }

  @Test
  public void support_install_dir_with_whitespaces() throws Exception {
    String dirName = "target/has space";
    FileUtils.deleteDirectory(new File(dirName));
    orchestrator = Orchestrator.builderEnv()
      .setOrchestratorProperty("orchestrator.workspaceDir", dirName)
      .build();
    orchestrator.start();

    assertThat(newAdminWsClient(orchestrator).system().status().getStatus()).isEqualTo(System.Status.UP);
  }

  // SONAR-4748
  @Test
  public void should_create_in_temp_folder() throws Exception {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.pluginArtifact("server-plugin"))
      .setServerProperty("sonar.createTempFiles", "true")
      .build();
    orchestrator.start();

    File tempDir = new File(orchestrator.getServer().getHome(), "temp/tmp");

    String logs = FileUtils.readFileToString(orchestrator.getServer().getWebLogs());
    assertThat(logs).contains("Creating temp directory: " + tempDir.getAbsolutePath() + File.separator + "sonar-it");
    assertThat(logs).contains("Creating temp file: " + tempDir.getAbsolutePath() + File.separator + "sonar-it");

    // Verify temp folder is created
    assertThat(new File(tempDir, "sonar-it")).isDirectory().exists();

    orchestrator.stop();

    // Verify temp folder is deleted after shutdown
    assertThat(new File(tempDir, "sonar-it")).doesNotExist();
  }

}
