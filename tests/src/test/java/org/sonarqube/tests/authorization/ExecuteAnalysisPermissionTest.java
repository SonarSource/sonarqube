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
package org.sonarqube.tests.authorization;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.permissions.AddGroupRequest;
import org.sonarqube.ws.client.permissions.AddProjectCreatorToTemplateRequest;
import org.sonarqube.ws.client.permissions.RemoveGroupRequest;
import org.sonarqube.ws.client.projects.UpdateVisibilityRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.runProjectAnalysis;

/**
 * SONAR-4397
 */
public class ExecuteAnalysisPermissionTest {

  private final static String USER_LOGIN = "scanperm";
  private final static String USER_PASSWORD = "thewhite";
  private final static String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

  @Before
  public void setUp() {
    tester.users().generate(u -> u.setLogin(USER_LOGIN).setPassword(USER_PASSWORD));
    orchestrator.getServer().provisionProject(PROJECT_KEY, "Sample");
  }

  @After
  public void tearDown() {
    addGlobalPermission("anyone", "scan");
  }

  @Test
  public void should_fail_if_logged_but_no_scan_permission() {
    executeLoggedAnalysis();

    removeGlobalPermission("anyone", "scan");
    try {
      // Execute logged analysis, but without the "Execute Analysis" permission
      executeLoggedAnalysis();
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains(
        "You're only authorized to execute a local (preview) SonarQube analysis without pushing the results to the SonarQube server. Please contact your SonarQube administrator.");
    }

    tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject(PROJECT_KEY).setVisibility("private"));
    try {
      // Execute anonymous analysis
      executeAnonymousAnalysis();
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains(
        "You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.");
    }
  }

  @Test
  public void no_need_for_browse_permission_to_scan() {
    // Do a first analysis, no error
    executeAnonymousAnalysis();

    // make project private
    tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject("sample").setVisibility("private"));

    // still no error
    executeAnonymousAnalysis();
  }

  @Test
  public void execute_analysis_with_scan_permission_only_on_project() {
    removeGlobalPermission("anyone", "scan");
    addProjectPermission("anyone", PROJECT_KEY, "scan");

    executeLoggedAnalysis();
  }

  @Test
  public void execute_analysis_with_scan_on_default_template() {
    removeGlobalPermission("anyone", "scan");
    tester.wsClient().permissions().addProjectCreatorToTemplate(new AddProjectCreatorToTemplateRequest()
      .setPermission("scan")
      .setTemplateId("default_template"));

    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", USER_PASSWORD, "sonar.projectKey", "ANOTHER_PROJECT_KEY");
  }

  private void addProjectPermission(String groupName, String projectKey, String permission) {
    tester.wsClient().permissions().addGroup(new AddGroupRequest().setGroupName(groupName).setProjectKey(projectKey).setPermission(permission));
  }

  private void addGlobalPermission(String groupName, String permission) {
    tester.wsClient().permissions().addGroup(new AddGroupRequest().setGroupName(groupName).setPermission(permission));
  }

  private void removeGlobalPermission(String groupName, String permission) {
    tester.wsClient().permissions().removeGroup(new RemoveGroupRequest().setGroupName(groupName).setPermission(permission));
  }

  private static void executeLoggedAnalysis() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", USER_PASSWORD);
  }

  private static void executeAnonymousAnalysis() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample");
  }
}
