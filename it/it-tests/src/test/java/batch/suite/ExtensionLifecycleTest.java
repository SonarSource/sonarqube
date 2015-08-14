/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch.suite;

import util.ItUtils;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class ExtensionLifecycleTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Before
  public void cleanup() {
    orchestrator.resetData();
  }

  @Test
  public void testInstantiationStrategyAndLifecycleOfBatchExtensions() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("batch/extension-lifecycle"))
      .setCleanSonarGoals()
      .setProperty("extension.lifecycle", "true")
      .setProperty("sonar.dynamicAnalysis", "false");

    // Build fails if the extensions provided in the extension-lifecycle-plugin are not correctly
    // managed.
    orchestrator.executeBuild(build);
  }
}
