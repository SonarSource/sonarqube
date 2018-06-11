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
package org.sonarqube.tests.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.newOrchestratorBuilder;
import static util.ItUtils.pluginArtifact;

public class PluginWithoutBuiltinQualityProfileTest {
  private static Orchestrator orchestrator;

  @Test
  public void should_fail_if_plugin_defines_language_and_no_builtin_qprofile() throws IOException {
    orchestrator = newOrchestratorBuilder(b -> b.addPlugin(pluginArtifact("foo-plugin-without-qprofile")));

    try {
      orchestrator.start();
      fail("Expected to fail to start");
    } catch (IllegalStateException e) {
      String logs = FileUtils.readFileToString(orchestrator.getServer().getWebLogs(), StandardCharsets.UTF_8);
      assertThat(logs).contains("java.lang.IllegalStateException: The following languages have no built-in quality profiles: foo");
    }
  }

  @After
  public void after() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }
}
