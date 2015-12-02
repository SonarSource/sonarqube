/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.runProjectAnalysis;

/**
 * SONAR-4397
 */
public class ScanPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private final static String USER_LOGIN = "scanperm";

  private static SonarClient adminClient;

  @Before
  public void setUp() {
    orchestrator.resetData();
    adminClient = orchestrator.getServer().adminWsClient();
    adminClient.userClient().create(UserParameters.create().login(USER_LOGIN).name(USER_LOGIN).password("thewhite").passwordConfirmation("thewhite"));
  }

  @After
  public void teraDown() {
    addPermission("anyone", "scan");
    addPermission("anyone", "dryRunScan");
    adminClient.userClient().deactivate(USER_LOGIN);
  }

  @Test
  public void should_fail_if_no_scan_permission() throws Exception {
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", "thewhite");

    removeGroupPermission("anyone", "scan");
    try {
      runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", "thewhite");
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains(
        "You're only authorized to execute a local (preview) SonarQube analysis without pushing the results to the SonarQube server. Please contact your SonarQube administrator.");
    }

    // Remove Anyone from dryrun permission
    removeGroupPermission("anyone", "dryRunScan");
    try {
      runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", "thewhite");
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains(
        "You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.");
    }
  }

  @Test
  public void no_need_for_browse_permission_to_scan() throws Exception {
    // Do a first analysis, no error
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", "thewhite");

    // Remove browse permission for groups Anyone on the project
    removeGroupPermission("anyone", "sample", "user");

    // still no error
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.login", USER_LOGIN, "sonar.password", "thewhite");
  }

  private static void addPermission(String groupName, String permission) {
    adminClient.post("api/permissions/add_group",
      "groupName", groupName,
      "permission", permission);
  }

  private static void removeGroupPermission(String groupName, String permission) {
    adminClient.post("api/permissions/remove_group",
      "groupName", groupName,
      "permission", permission);
  }

  private static void removeGroupPermission(String groupName, String projectKey, String permission) {
    adminClient.post("api/permissions/remove_group",
      "groupName", groupName,
      "projectKey", projectKey,
      "permission", permission);
  }
}
