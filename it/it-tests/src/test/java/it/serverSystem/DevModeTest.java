/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * This class start a new orchestrator on each test case
 */
public class DevModeTest {

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
   * SONAR-4843
   */
  @Test
  public void restart_forbidden_if_not_dev_mode() throws Exception {
    // server classloader locks Jar files on Windows
    if (!SystemUtils.IS_OS_WINDOWS) {
      orchestrator = Orchestrator.builderEnv()
        .build();
      orchestrator.start();
      try {
        orchestrator.getServer().adminWsClient().systemClient().restart();
        fail();
      } catch (Exception e) {
        assertThat(e.getMessage()).contains("403");
      }
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

      orchestrator.getServer().adminWsClient().systemClient().restart();
      assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()))
        .contains("Restart server")
        .contains("Server restarted");
    }
  }
}
