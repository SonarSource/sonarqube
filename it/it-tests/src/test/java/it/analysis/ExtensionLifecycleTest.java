/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import it.Category3Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

public class ExtensionLifecycleTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Before
  public void cleanup() {
    orchestrator.resetData();
  }

  @Test
  public void testInstantiationStrategyAndLifecycleOfBatchExtensions() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("analysis/extension-lifecycle"))
      .setCleanSonarGoals()
      .setProperty("extension.lifecycle", "true")
      .setProperty("sonar.dynamicAnalysis", "false");

    // Build fails if the extensions provided in the extension-lifecycle-plugin are not correctly
    // managed.
    orchestrator.executeBuild(build);
  }
}
