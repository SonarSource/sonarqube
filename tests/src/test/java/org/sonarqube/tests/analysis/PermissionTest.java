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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category3Suite;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.UserTokens;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.usertokens.GenerateRequest;
import org.sonarqube.ws.client.usertokens.RevokeRequest;
import org.sonarqube.ws.client.usertokens.UserTokensService;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class PermissionTest {

  private static final String A_LOGIN = "a_login";
  private static final String A_PASSWORD = "a_password";

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private WsClient adminWsClient;
  private UserTokensService userTokensWsClient;

  @Before
  public void setUp() {
    orchestrator.resetData();

    // enforce scanners to be authenticated
    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");

    adminWsClient = newAdminWsClient(orchestrator);
    userTokensWsClient = adminWsClient.userTokens();
  }

  @After
  public void tearDown() {
    resetSettings(orchestrator, null, "sonar.forceAuthentication");
    userRule.resetUsers();
  }

  @Test
  public void scanner_can_authenticate_with_authentication_token() {
    createUserWithProvisioningAndScanPermissions();

    String tokenName = "For test";
    UserTokens.GenerateWsResponse generateWsResponse = userTokensWsClient.generate(new GenerateRequest()
      .setLogin(A_LOGIN)
      .setName(tokenName));
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", generateWsResponse.getToken(),
      "sonar.password", "");

    BuildResult buildResult = orchestrator.executeBuild(sampleProject);

    assertThat(buildResult.isSuccess()).isTrue();
    userTokensWsClient.revoke(new RevokeRequest().setLogin(A_LOGIN).setName(tokenName));
  }

  @Test
  public void scanner_fails_if_authentication_token_is_not_valid() {
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
  public void scanner_can_authenticate_with_login_password() {
    createUserWithProvisioningAndScanPermissions();

    orchestrator.getServer().provisionProject("sample", "xoo-sample");

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
      "sonar.login", A_LOGIN,
      "sonar.password", A_PASSWORD);
    assertThat(buildResult.getLastStatus()).isEqualTo(0);
  }

  @Test
  public void run_scanner_with_user_having_scan_permission_only_on_project() {
    userRule.createUser(A_LOGIN, A_PASSWORD);
    orchestrator.getServer().provisionProject("sample", "sample");
    addUserPermission(A_LOGIN, "scan", "sample");

    BuildResult buildResult = scanQuietly("shared/xoo-sample", "sonar.login", A_LOGIN, "sonar.password", A_PASSWORD);

    assertThat(buildResult.isSuccess()).isTrue();
  }

  private void addUserPermission(String login, String permission, @Nullable String projectKey) {
    adminWsClient.permissions().addUser(new AddUserRequest()
      .setLogin(login)
      .setPermission(permission)
      .setProjectKey(projectKey));
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
    return SonarScanner.create(projectDir(projectPath))
      .setProperties(props);
  }

  private void createUserWithProvisioningAndScanPermissions() {
    userRule.createUser(A_LOGIN, A_PASSWORD);
    addUserPermission(A_LOGIN, "provisioning", null);
    addUserPermission(A_LOGIN, "scan", null);
  }

}
