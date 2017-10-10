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
package org.sonarqube.tests.startup;

import com.sonar.orchestrator.Orchestrator;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class StartupFailureTest {

  @Test
  public void exception_during_ce_startup_should_stop_app_startup() throws Exception {
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.pluginArtifact("missing-dep-plugin"))
      .build();
    boolean startupFailed = false;
    try {
      orchestrator.start();
    } catch (Exception e) {
      startupFailed = true;
    } finally {
      orchestrator.stop();
    }
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getCeLogs(), StandardCharsets.UTF_8))
      .contains("Unable to find plugin with key foo");
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getAppLogs(), StandardCharsets.UTF_8))
      .doesNotContain("Process[ce] is up")
      .doesNotContain("SonarQube is up");
    assertThat(startupFailed).isTrue();
  }
}
