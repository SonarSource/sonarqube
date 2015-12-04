/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it;

import com.sonar.orchestrator.Orchestrator;
import it.analysis.*;
import it.measure.DecimalScaleMetricTest;
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
  SettingsEncryptionTest.class,
  ReportDumpTest.class,
  // measures
  DecimalScaleMetricTest.class
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

    // used by TempFolderTest and DecimalScaleMetricTest
    .addPlugin(pluginArtifact("batch-plugin"))

    // used by ExtensionLifecycleTest
    .addPlugin(pluginArtifact("extension-lifecycle-plugin"))

    // used by ProjectBuilderTest
    .addPlugin(pluginArtifact("project-builder-plugin"))

    .build();
}
