/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import it.analysisExclusion.FileExclusionsTest;
import it.analysisExclusion.IssueExclusionsTest;
import it.component.ComponentsWsTest;
import it.component.ProjectSearchTest;
import it.componentDashboard.DashboardTest;
import it.componentSearch.AllProjectsTest;
import it.dbCleaner.PurgeTest;
import it.duplication.CrossProjectDuplicationsOnRemoveFileTest;
import it.duplication.CrossProjectDuplicationsTest;
import it.duplication.DuplicationsTest;
import it.http.HttpHeadersTest;
import it.projectComparison.ProjectComparisonTest;
import it.projectEvent.EventTest;
import it.qualityProfile.QualityProfilesPageTest;
import it.serverSystem.ServerSystemTest;
import it.ui.UiTest;
import it.uiExtension.UiExtensionsTest;
import it.user.BaseIdentityProviderTest;
import it.user.FavouriteTest;
import it.user.ForceAuthenticationTest;
import it.user.LocalAuthenticationTest;
import it.user.MyAccountPageTest;
import it.user.OAuth2IdentityProviderTest;
import it.ws.WsLocalCallTest;
import it.ws.WsTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  // server system
  ServerSystemTest.class,
  // user
  MyAccountPageTest.class,
  FavouriteTest.class,
  // authentication
  ForceAuthenticationTest.class,
  LocalAuthenticationTest.class,
  BaseIdentityProviderTest.class,
  OAuth2IdentityProviderTest.class,
  // component search
  ProjectSearchTest.class,
  ComponentsWsTest.class,
  // analysis exclusion
  FileExclusionsTest.class,
  IssueExclusionsTest.class,
  // duplication
  CrossProjectDuplicationsTest.class,
  CrossProjectDuplicationsOnRemoveFileTest.class,
  DuplicationsTest.class,
  // db cleaner
  PurgeTest.class,
  // project event
  EventTest.class,
  // component dashboard
  DashboardTest.class,
  // project comparison
  ProjectComparisonTest.class,
  // component search
  AllProjectsTest.class,
  // http
  HttpHeadersTest.class,
  // ui
  UiTest.class,
  // ui extensions
  UiExtensionsTest.class,
  WsLocalCallTest.class,
  WsTest.class,
  // quality profiles
  QualityProfilesPageTest.class
})
public class Category4Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())

    // Used in BaseIdentityProviderTest
    .addPlugin(pluginArtifact("base-auth-plugin"))

    // Used in OAuth2IdentityProviderTest
    .addPlugin(pluginArtifact("oauth2-auth-plugin"))

    // Used in DashboardTest
    .addPlugin(pluginArtifact("dashboard-plugin"))
    .addPlugin(pluginArtifact("required-measures-widgets-plugin"))

    // Used in UiExtensionsTest
    .addPlugin(pluginArtifact("ui-extensions-plugin"))

    // Used by WsLocalCallTest
    .addPlugin(pluginArtifact("ws-plugin"))
    .build();
}
