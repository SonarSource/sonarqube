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

package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category3Suite;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsUserTokens;
import org.sonarqube.ws.client.WsClient;
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

public class AuthenticationTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  private static WsClient adminWsClient;
  private static UserTokensService userTokensWsClient;

  private static final String LOGIN = "george.orwell";

  @BeforeClass
  public static void setUp() {
    adminWsClient = newAdminWsClient(orchestrator);
    userTokensWsClient = adminWsClient.userTokens();

    userRule.createUser(LOGIN, "123456");
    addUserPermission(LOGIN, "admin");
    addUserPermission(LOGIN, "scan");

    userRule.createUser("simple-user", "password");
  }

  @AfterClass
  public static void tearDown() throws Exception {
    resetSettings(orchestrator, null, "sonar.forceAuthentication");
  }

  @Before
  public void deleteData() {
    orchestrator.resetData();
    resetSettings(orchestrator, null, "sonar.forceAuthentication");
  }

  @Test
  public void run_analysis_with_token_authentication() {
    String tokenName = "Analyze Project";
    WsUserTokens.GenerateWsResponse generateWsResponse = userTokensWsClient.generate(new GenerateWsRequest()
      .setLogin(LOGIN)
      .setName(tokenName));
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", generateWsResponse.getToken(),
      "sonar.password", "");

    BuildResult buildResult = orchestrator.executeBuild(sampleProject);

    assertThat(buildResult.isSuccess()).isTrue();
    userTokensWsClient.revoke(new RevokeWsRequest().setLogin(LOGIN).setName(tokenName));
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

  private static void addUserPermission(String login, String permission) {
    adminWsClient.permissions().addUser(new AddUserWsRequest()
      .setLogin(login)
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
