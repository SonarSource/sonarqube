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
package it.authorisation;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import it.Category1Suite;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddProjectCreatorToTemplateWsRequest;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.project.UpdateVisibilityRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonarqube.ws.client.project.UpdateVisibilityRequest.Visibility.PRIVATE;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;

/**
 * SONAR-4397
 */
public class ExecuteAnalysisPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private final static String USER_LOGIN = "scanperm";
  private final static String USER_PASSWORD = "thewhite";
  private final static String PROJECT_KEY = "sample";

  private static WsClient adminWsClient;
  private static SonarClient oldAdminWsClient;

  @Before
  public void setUp() {
    orchestrator.resetData();
    oldAdminWsClient = orchestrator.getServer().adminWsClient();
    oldAdminWsClient.userClient().create(UserParameters.create().login(USER_LOGIN).name(USER_LOGIN).password(USER_PASSWORD).passwordConfirmation(USER_PASSWORD));
    orchestrator.getServer().provisionProject(PROJECT_KEY, "Sample");
    adminWsClient = newAdminWsClient(orchestrator);
  }

  @After
  public void tearDown() {
    addGlobalPermission("anyone", "scan");
    oldAdminWsClient.userClient().deactivate(USER_LOGIN);
  }

  @Test
  public void should_fail_if_logged_but_no_scan_permission() throws Exception {
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

    ItUtils.newAdminWsClient(orchestrator).projects().updateVisibility(new UpdateVisibilityRequest(PROJECT_KEY, PRIVATE));
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
  public void no_need_for_browse_permission_to_scan() throws Exception {
    // Do a first analysis, no error
    executeAnonymousAnalysis();

    // make project private
    ItUtils.newAdminWsClient(orchestrator).projects().updateVisibility(new UpdateVisibilityRequest("sample", PRIVATE));

    // still no error
    executeAnonymousAnalysis();
  }

  @Test
  public void execute_analysis_with_scan_permission_only_on_project() throws Exception {
    removeGlobalPermission("anyone", "scan");
    addProjectPermission("anyone", PROJECT_KEY, "scan");

    executeLoggedAnalysis();
  }

  @Test
  public void execute_analysis_with_scan_on_default_template() {
    removeGlobalPermission("anyone", "scan");
    adminWsClient.permissions().addProjectCreatorToTemplate(AddProjectCreatorToTemplateWsRequest.builder()
      .setPermission("scan")
      .setTemplateId("default_template")
      .build());

    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", USER_PASSWORD, "sonar.projectKey", "ANOTHER_PROJECT_KEY");
  }

  private static void addProjectPermission(String groupName, String projectKey, String permission) {
    adminWsClient.permissions().addGroup(new AddGroupWsRequest().setGroupName(groupName).setProjectKey(projectKey).setPermission(permission));
  }

  private static void addGlobalPermission(String groupName, String permission) {
    adminWsClient.permissions().addGroup(new AddGroupWsRequest().setGroupName(groupName).setPermission(permission));
  }

  private static void removeProjectPermission(String groupName, String projectKey, String permission) {
    adminWsClient.permissions().removeGroup(new RemoveGroupWsRequest().setGroupName(groupName).setProjectKey(projectKey).setPermission(permission));
  }

  private static void removeGlobalPermission(String groupName, String permission) {
    adminWsClient.permissions().removeGroup(new RemoveGroupWsRequest().setGroupName(groupName).setPermission(permission));
  }

  private static void executeLoggedAnalysis() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", USER_PASSWORD);
  }

  private static void executeAnonymousAnalysis() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample");
  }
}
