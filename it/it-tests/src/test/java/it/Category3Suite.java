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
package it;

import com.sonar.orchestrator.Orchestrator;
import it.analysis.ExtensionLifecycleTest;
import it.analysis.FavoriteTest;
import it.analysis.IssueJsonReportTest;
import it.analysis.IssuesModeTest;
import it.analysis.LinksTest;
import it.analysis.MultiLanguageTest;
import it.analysis.PermissionTest;
import it.analysis.ProjectBuilderTest;
import it.analysis.ReportDumpTest;
import it.analysis.SSLTest;
import it.analysis.ScannerTest;
import it.analysis.SettingsEncryptionTest;
import it.analysis.TempFolderTest;
import it.measure.DecimalScaleMetricTest;
import it.plugins.VersionPluginTest;
import it.webhook.WebhooksTest;
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
