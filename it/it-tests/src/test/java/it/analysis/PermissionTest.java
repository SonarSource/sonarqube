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
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category3Suite;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsUserTokens;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;
import org.sonarqube.ws.client.usertoken.RevokeWsRequest;
import org.sonarqube.ws.client.usertoken.UserTokensService;
import util.ItUtils;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class PermissionTest {

  private static final String SIMPLE_USER = "simple-user";

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);
  private static WsClient adminWsClient;

  private static UserTokensService userTokensWsClient;

  @BeforeClass
  public static void setUp() {
    adminWsClient = newAdminWsClient(orchestrator);
    userTokensWsClient = adminWsClient.userTokens();
    resetData();
  }

  @AfterClass
  public static void restoreAnyonePermissionToScan() throws Exception {
    addGroupPermission("Anyone", "scan");
  }

  @Before
  public void deleteData() {
    resetData();
  }

  private static void resetData() {
    orchestrator.resetData();
    resetSettings(orchestrator, null, "sonar.forceAuthentication");
    userRule.deactivateUsers(SIMPLE_USER);
    removeGroupPermission("Anyone", "scan");
  }

  @Test
  public void run_analysis_with_token_authentication() {
    userRule.createUser(SIMPLE_USER, "password");
    addUserPermission(SIMPLE_USER, "scan", null);
    String tokenName = "Analyze Project";
    WsUserTokens.GenerateWsResponse generateWsResponse = userTokensWsClient.generate(new GenerateWsRequest()
      .setLogin(SIMPLE_USER)
      .setName(tokenName));
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", generateWsResponse.getToken(),
      "sonar.password", "");

    BuildResult buildResult = orchestrator.executeBuild(sampleProject);

    assertThat(buildResult.isSuccess()).isTrue();
    userTokensWsClient.revoke(new RevokeWsRequest().setLogin(SIMPLE_USER).setName(tokenName));
  }

  @Test
  public void run_analysis_with_incorrect_token() {
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", "unknown-token",
      "sonar.password", "");

    BuildResult buildResult = orchestrator.executeBuildQuietly(sampleProject);

    assertThat(buildResult.isSuccess()).isFalse();
  }

  /**
   * SONAR-4211 Test Sonar Runner when server requires authentication
   */
  @Test
  public void should_authenticate_when_needed() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.login", "",
      "sonar.password", "");
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");

    // SONAR-4048
    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.login", "wrong_login",
      "sonar.password", "wrong_password");
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "Not authorized. Please check the properties sonar.login and sonar.password.");

    buildResult = scan("shared/xoo-sample",
      "sonar.login", "admin",
      "sonar.password", "admin");
    assertThat(buildResult.getLastStatus()).isEqualTo(0);
  }

  @Test
  public void run_analysis_with_authenticated_user_having_global_execute_analysis_permission() throws Exception {
    userRule.createUser(SIMPLE_USER, "password");
    orchestrator.getServer().provisionProject("sample", "sample");
    addUserPermission(SIMPLE_USER, "scan", null);

    BuildResult buildResult = scanQuietly("shared/xoo-sample", "sonar.login", SIMPLE_USER, "sonar.password", "password");

    assertThat(buildResult.isSuccess()).isTrue();
  }

  @Test
  public void run_analysis_with_authenticated_user_having_execute_analysis_permission_only_on_project() throws Exception {
    userRule.createUser(SIMPLE_USER, "password");
    orchestrator.getServer().provisionProject("sample", "sample");
    addUserPermission(SIMPLE_USER, "scan", "sample");

    BuildResult buildResult = scanQuietly("shared/xoo-sample", "sonar.login", SIMPLE_USER, "sonar.password", "password");

    assertThat(buildResult.isSuccess()).isTrue();
  }

  @Test
  public void run_analysis_when_execute_analysis_is_set_to_anyone() throws Exception {
    addGroupPermission("Anyone", "scan");

    BuildResult buildResult = scanQuietly("shared/xoo-sample");

    assertThat(buildResult.isSuccess()).isTrue();
  }

  private static void addUserPermission(String login, String permission, @Nullable String projectKey) {
    adminWsClient.permissions().addUser(new AddUserWsRequest()
      .setLogin(login)
      .setPermission(permission)
      .setProjectKey(projectKey));
  }

  private static void addGroupPermission(String groupName, String permission) {
    adminWsClient.permissions().addGroup(new AddGroupWsRequest()
      .setGroupName(groupName)
      .setPermission(permission));
  }

  private static void removeGroupPermission(String groupName, String permission) {
    adminWsClient.permissions().removeGroup(new RemoveGroupWsRequest()
      .setGroupName(groupName)
      .setPermission(permission));
  }

  private BuildResult scan(String projectPath, String... props) {
    SonarScanner scanner = configureScanner(projectPath, props);
    return orchestrator.executeBuild(scanner);
  }

  private BuildResult scanQuietly(String projectPath, String... props) {
    SonarScanner scanner = configureScanner(projectPath, props);
    return orchestrator.executeBuildQuietly(scanner);
  }

  private SonarScanner configureScanner(String projectPath, String... props) {
    return SonarScanner.create(ItUtils.projectDir(projectPath))
      .setProperties(props);
  }
}
