/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch.suite;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import util.ItUtils;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ExtensionLifecycleTest.class, LinksTest.class, MavenTest.class, ProjectBuilderTest.class, ProjectExclusionsTest.class,
  TempFolderTest.class, MultiLanguageTest.class, IssueJsonReportTest.class, ProjectProvisioningTest.class, BatchTest.class,
  IssuesModeTest.class, SettingsEncryptionTest.class
})
public class BatchTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE").addPlugin("java")
    .setContext("/")

    // Used by SettingsEncryptionTest
    .addPlugin(ItUtils.pluginArtifact("settings-encryption-plugin"))

    // Used by IssuesModeTest
    .addPlugin(ItUtils.pluginArtifact("access-secured-props-plugin"))

    // used by TempFolderTest
    .addPlugin(ItUtils.pluginArtifact("batch-plugin"))

    // used by ExtensionLifecycleTest
    .addPlugin(ItUtils.pluginArtifact("extension-lifecycle-plugin"))

    // used by ProjectBuilderTest
    .addPlugin(ItUtils.pluginArtifact("project-builder-plugin"))

    .build();
}
