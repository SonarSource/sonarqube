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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.UserTokens;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.usertokens.GenerateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class PermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  public void setUp() {
    // enforce scanners to be authenticated
    tester.settings().setGlobalSetting("sonar.forceAuthentication", "true");
  }

  @Test
  public void scanner_can_authenticate_with_authentication_token() {
    User user = createUserWithProvisioningAndScanPermissions();

    String tokenName = "For test";
    UserTokens.GenerateWsResponse generateWsResponse = tester.wsClient().userTokens().generate(new GenerateRequest()
      .setLogin(user.getLogin())
      .setName(tokenName));
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", generateWsResponse.getToken(),
      "sonar.password", "");

    BuildResult buildResult = orchestrator.executeBuild(sampleProject);

    assertThat(buildResult.isSuccess()).isTrue();
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
    User user = createUserWithProvisioningAndScanPermissions();

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
      "sonar.login", user.getLogin(),
      "sonar.password", user.getLogin());
    assertThat(buildResult.getLastStatus()).isEqualTo(0);
  }

  @Test
  public void run_scanner_with_user_having_only_scan_permission_on_project() {
    User user = tester.users().generate();
    Project project = tester.projects().provision(c -> c.setVisibility("private"));
    Permissions.PermissionTemplate template = tester.permissions().generateTemplate();
    tester.permissions().addUserToTemplate(user, template, "scan");
    tester.permissions().applyTemplate(template, project);

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.projectKey", project.getKey(),
      "sonar.login", user.getLogin(),
      "sonar.password", user.getLogin());

    assertThat(buildResult.isSuccess()).isTrue();
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

  private User createUserWithProvisioningAndScanPermissions() {
    User user = tester.users().generate();
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(user.getLogin()).setPermission("provisioning"));
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(user.getLogin()).setPermission("scan"));
    return user;
  }

}
