/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch.suite;

import util.ItUtils;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ExtensionLifecycleTest.class, LinksTest.class, MavenTest.class, ProjectBuilderTest.class, ProjectExclusionsTest.class,
  TempFolderTest.class, MultiLanguageTest.class, IssueJsonReportTest.class, ProjectProvisioningTest.class
})
public class BatchTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE").addPlugin("java")
    .setContext("/")

    // used by TempFolderTest
    .addPlugin(ItUtils.pluginArtifact("batch-plugin"))

    // used by ExtensionLifecycleTest
    .addPlugin(ItUtils.pluginArtifact("extension-lifecycle-plugin"))

    // used by ProjectBuilderTest
    .addPlugin(ItUtils.pluginArtifact("project-builder-plugin"))

    // used by SemaphoreTest
    .addPlugin(ItUtils.pluginArtifact("crash-plugin"))

    .build();
}
