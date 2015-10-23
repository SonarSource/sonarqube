/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it;

import com.sonar.orchestrator.Orchestrator;
import it.analysis.BatchTest;
import it.analysis.ExtensionLifecycleTest;
import it.analysis.IssueJsonReportTest;
import it.analysis.IssuesModeTest;
import it.analysis.LinksTest;
import it.analysis.MavenTest;
import it.analysis.MultiLanguageTest;
import it.analysis.ProjectBuilderTest;
import it.analysis.ProjectExclusionsTest;
import it.analysis.ProjectProvisioningTest;
import it.analysis.SettingsEncryptionTest;
import it.analysis.TempFolderTest;
import it.analysisExclusion.FileExclusionsTest;
import it.analysisExclusion.IssueExclusionsTest;
import it.duplication.CrossProjectDuplicationsTest;
import it.duplication.DuplicationsTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  // analysis
  ExtensionLifecycleTest.class,
  LinksTest.class,
  MavenTest.class,
  ProjectBuilderTest.class,
  ProjectExclusionsTest.class,
  TempFolderTest.class,
  MultiLanguageTest.class,
  IssueJsonReportTest.class,
  ProjectProvisioningTest.class,
  BatchTest.class,
  IssuesModeTest.class,
  SettingsEncryptionTest.class
})
public class Category3Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE").addPlugin("java")
    .setContext("/")

    // Used by SettingsEncryptionTest
    .addPlugin(pluginArtifact("settings-encryption-plugin"))

    // Used by IssuesModeTest
    .addPlugin(pluginArtifact("access-secured-props-plugin"))

    // used by TempFolderTest
    .addPlugin(pluginArtifact("batch-plugin"))

    // used by ExtensionLifecycleTest
    .addPlugin(pluginArtifact("extension-lifecycle-plugin"))

    // used by ProjectBuilderTest
    .addPlugin(pluginArtifact("project-builder-plugin"))

    .build();
}
