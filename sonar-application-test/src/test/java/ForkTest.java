/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
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
