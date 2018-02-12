/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.tests.analysis.FileExclusionsTest;
import org.sonarqube.tests.analysis.IssueExclusionsTest;
import org.sonarqube.tests.ce.CeTempDirTest;
import org.sonarqube.tests.ce.CeWsTest;
import org.sonarqube.tests.qualityProfile.QualityProfilesUiTest;
import org.sonarqube.tests.rule.RulesPageTest;
import org.sonarqube.tests.serverSystem.HttpHeadersTest;
import org.sonarqube.tests.serverSystem.LogsTest;
import org.sonarqube.tests.serverSystem.PingTest;
import org.sonarqube.tests.serverSystem.ServerSystemTest;
import org.sonarqube.tests.serverSystem.SystemInfoTest;
import org.sonarqube.tests.ui.UiExtensionsTest;
import org.sonarqube.tests.ui.UiTest;
import org.sonarqube.tests.ws.WsLocalCallTest;
import org.sonarqube.tests.ws.WsTest;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

/**
 * @deprecated use dedicated suites in each package (see {@link org.sonarqube.tests.measure.MeasureSuite}
 * for instance)
 */
@Deprecated
@RunWith(Suite.class)
@Suite.SuiteClasses({
  // server system
  ServerSystemTest.class,
  SystemInfoTest.class,
  PingTest.class,
  // analysis exclusion
  FileExclusionsTest.class,
  IssueExclusionsTest.class,
  // http
  HttpHeadersTest.class,
  // ui
  UiTest.class,
  // ui extensions
  UiExtensionsTest.class,
  WsLocalCallTest.class,
  WsTest.class,
  // quality profiles
  QualityProfilesUiTest.class,
  RulesPageTest.class,
  LogsTest.class,
  // ce
  CeWsTest.class,
  CeTempDirTest.class
})
public class Category4Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())

    // Used in UiExtensionsTest
    .addPlugin(pluginArtifact("ui-extensions-plugin"))

    // Used by WsLocalCallTest
    .addPlugin(pluginArtifact("ws-plugin"))

    // Used by LogsTest
    .setServerProperty("sonar.web.accessLogs.pattern", LogsTest.ACCESS_LOGS_PATTERN)

    // reduce memory for Elasticsearch to 128M
    .setServerProperty("sonar.search.javaOpts", "-Xms128m -Xmx128m")

    .build();
}
