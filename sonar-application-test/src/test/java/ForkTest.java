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
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ForkTest {

  private Orchestrator orchestrator;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void start_and_stop() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    builder.addPlugin(MavenLocation.create("com.sonarsource.xoo", "sonar-xoo-plugin", "1.0-SNAPSHOT"));
    orchestrator = builder.build();
    orchestrator.start();

    // verify web service that requests elasticsearch
    String json = orchestrator.getServer().wsClient().get("/api/rules/search", Collections.<String, Object>emptyMap());
    assertThat(json).startsWith("{").endsWith("}");

    // project analysis
    orchestrator.executeBuild(SonarRunner.create(new File("src/test/projects/xoo-sample")));

    orchestrator.stop();
    try {
      orchestrator.getServer().wsClient().get("/api/rules/search", Collections.<String, Object>emptyMap());
      fail("Server is not stopped");
    } catch (Exception e) {
      // ok
    }
  }

}
