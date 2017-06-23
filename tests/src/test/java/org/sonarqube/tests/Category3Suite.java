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
package org.sonarqube.tests;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.analysis.ExtensionLifecycleTest;
import org.sonarqube.tests.analysis.FavoriteTest;
import org.sonarqube.tests.analysis.IssueJsonReportTest;
import org.sonarqube.tests.analysis.IssuesModeTest;
import org.sonarqube.tests.analysis.LinksTest;
import org.sonarqube.tests.analysis.MultiLanguageTest;
import org.sonarqube.tests.analysis.PermissionTest;
import org.sonarqube.tests.analysis.ProjectBuilderTest;
import org.sonarqube.tests.analysis.ReportDumpTest;
import org.sonarqube.tests.analysis.SSLTest;
import org.sonarqube.tests.analysis.ScannerTest;
import org.sonarqube.tests.analysis.SettingsEncryptionTest;
import org.sonarqube.tests.analysis.TempFolderTest;
import org.sonarqube.tests.measure.DecimalScaleMetricTest;
import org.sonarqube.tests.plugins.VersionPluginTest;
import org.sonarqube.tests.webhook.WebhooksTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  // analysis
  PermissionTest.class,
  ExtensionLifecycleTest.class,
  LinksTest.class,
  ProjectBuilderTest.class,
  TempFolderTest.class,
  MultiLanguageTest.class,
  IssueJsonReportTest.class,
  ScannerTest.class,
  IssuesModeTest.class,
  VersionPluginTest.class,
  SettingsEncryptionTest.class,
  ReportDumpTest.class,
  SSLTest.class,
  FavoriteTest.class,
  // measures
  DecimalScaleMetricTest.class,
  WebhooksTest.class
})
public class Category3Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE").addPlugin("java")

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
