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
package org.sonarqube.tests.ce;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;

public class WorkerCountTest {

  private Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void workerCount_can_be_controlled_via_plugin() throws IOException {
    String workerCount = "5";
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("fake-governance-plugin"))
      .setServerProperty("fakeGovernance.workerCount", workerCount);
    orchestrator = builder.build();
    orchestrator.start();

    Set<String> line = Files.lines(orchestrator.getServer().getCeLogs().toPath())
      .filter(s -> s.contains("Compute Engine will use "))
      .collect(Collectors.toSet());
    assertThat(line)
      .hasSize(1);
    assertThat(line.iterator().next()).contains(workerCount);
  }
}
