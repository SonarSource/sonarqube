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
import org.sonarqube.tests.analysis.FileExclusionsTest;
import org.sonarqube.tests.analysis.IssueExclusionsTest;
import org.sonarqube.tests.component.ComponentsWsTest;
import org.sonarqube.tests.component.ProjectsWsTest;
import org.sonarqube.tests.dbCleaner.PurgeTest;
import org.sonarqube.tests.duplication.CrossProjectDuplicationsOnRemoveFileTest;
import org.sonarqube.tests.duplication.CrossProjectDuplicationsTest;
import org.sonarqube.tests.duplication.DuplicationsTest;
import org.sonarqube.tests.duplication.NewDuplicationsTest;
import org.sonarqube.tests.organization.RootUserTest;
import org.sonarqube.tests.projectEvent.EventTest;
import org.sonarqube.tests.projectEvent.ProjectActivityPageTest;
import org.sonarqube.tests.qualityProfile.QualityProfilesUiTest;
import org.sonarqube.tests.serverSystem.HttpHeadersTest;
import org.sonarqube.tests.serverSystem.LogsTest;
import org.sonarqube.tests.serverSystem.PingTest;
import org.sonarqube.tests.serverSystem.ServerSystemTest;
import org.sonarqube.tests.ui.SourceViewerTest;
import org.sonarqube.tests.ui.UiTest;
import org.sonarqube.tests.ui.UiExtensionsTest;
import org.sonarqube.tests.user.BaseIdentityProviderTest;
import org.sonarqube.tests.user.FavoritesWsTest;
import org.sonarqube.tests.user.ForceAuthenticationTest;
import org.sonarqube.tests.user.LocalAuthenticationTest;
import org.sonarqube.tests.user.MyAccountPageTest;
import org.sonarqube.tests.user.OAuth2IdentityProviderTest;
import org.sonarqube.tests.ws.WsLocalCallTest;
import org.sonarqube.tests.ws.WsTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  // organization
  RootUserTest.class,
  // server system
  ServerSystemTest.class,
  PingTest.class,
  // user
  MyAccountPageTest.class,
  FavoritesWsTest.class,
  // authentication
  ForceAuthenticationTest.class,
  LocalAuthenticationTest.class,
  BaseIdentityProviderTest.class,
  OAuth2IdentityProviderTest.class,
  // component search
  ProjectsWsTest.class,
  ComponentsWsTest.class,
  // analysis exclusion
  FileExclusionsTest.class,
  IssueExclusionsTest.class,
  // duplication
  CrossProjectDuplicationsTest.class,
  CrossProjectDuplicationsOnRemoveFileTest.class,
  DuplicationsTest.class,
  NewDuplicationsTest.class,
  // db cleaner
  PurgeTest.class,
  // project event
  EventTest.class,
  ProjectActivityPageTest.class,
  // http
  HttpHeadersTest.class,
  // ui
  UiTest.class,
  SourceViewerTest.class,
  // ui extensions
  UiExtensionsTest.class,
  WsLocalCallTest.class,
  WsTest.class,
  // quality profiles
  QualityProfilesUiTest.class,
  LogsTest.class
})
public class Category4Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())

    // Used in BaseIdentityProviderTest
    .addPlugin(pluginArtifact("base-auth-plugin"))

    // Used in OAuth2IdentityProviderTest
    .addPlugin(pluginArtifact("oauth2-auth-plugin"))

    // Used in UiExtensionsTest
    .addPlugin(pluginArtifact("ui-extensions-plugin"))

    // Used by WsLocalCallTest
    .addPlugin(pluginArtifact("ws-plugin"))

    // Used by LogsTest
    .setServerProperty("sonar.web.accessLogs.pattern", LogsTest.ACCESS_LOGS_PATTERN)

    .build();
}
