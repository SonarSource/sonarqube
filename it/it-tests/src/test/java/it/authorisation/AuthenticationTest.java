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
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarqube.ws.WsUserTokens;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpWsClient;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;
import org.sonarqube.ws.client.usertoken.UserTokensService;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class AuthenticationTest {
  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;
  private static UserTokensService userTokensWsClient;

  private static final String PROJECT_KEY = "sample";
  private static final String LOGIN = "george.orwell";

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/authorisation/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, "Sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    adminWsClient = newAdminWsClient(ORCHESTRATOR);
    userTokensWsClient = adminWsClient.userTokens();
    removeGroupPermission("anyone", "dryRunScan");
    removeGroupPermission("anyone", "scan");

    createUser(LOGIN, "123456");
    addUserPermission(LOGIN, "admin");
    addUserPermission(LOGIN, "scan");
  }

  @AfterClass
  public static void delete_data() {
    deactivateUser(LOGIN);
    addGroupPermission("anyone", "dryRunScan");
    addGroupPermission("anyone", "scan");
  }

  @Test
  public void basic_authentication_based_on_login_and_password() {
    String userId = UUID.randomUUID().toString();
    String login = format("login-%s", userId);
    String name = format("name-%s", userId);
    String password = "!ascii-only:-)@";
    createUser(login, name, password);

    // authenticate
    WsClient wsClient = new HttpWsClient(new HttpConnector.Builder().url(ORCHESTRATOR.getServer().getUrl()).credentials(login, password).build());
    WsResponse response = wsClient.wsConnector().call(new GetRequest("api/authentication/validate"));
    assertThat(response.content()).isEqualTo("{\"valid\":true}");
  }

  @Test
  public void basic_authentication_based_on_token() {
    WsUserTokens.GenerateWsResponse generateWsResponse = userTokensWsClient.generate(new GenerateWsRequest()
      .setLogin(LOGIN)
      .setName("Validate token based authentication"));
    WsClient wsClient = new HttpWsClient(new HttpConnector.Builder()
      .url(ORCHESTRATOR.getServer().getUrl())
      .token(generateWsResponse.getToken()).build());

    WsResponse response = wsClient.wsConnector().call(new GetRequest("api/authentication/validate"));

    assertThat(response.content()).isEqualTo("{\"valid\":true}");
  }

  /**
   * This is currently a limitation of Ruby on Rails stack.
   */
  @Test
  public void basic_authentication_does_not_support_utf8_passwords() {
    String userId = UUID.randomUUID().toString();
    String login = format("login-%s", userId);
    // see http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
    String password = "κόσμε";

    // create user with a UTF-8 password
    createUser(login, format("name-%s", userId), password);

    // authenticate
    WsClient wsClient = new HttpWsClient(new HttpConnector.Builder().url(ORCHESTRATOR.getServer().getUrl()).credentials(login, password).build());
    WsResponse response = wsClient.wsConnector().call(new GetRequest("api/authentication/validate"));
    assertThat(response.content()).isEqualTo("{\"valid\":false}");
  }

  @Test
  @Ignore
  public void web_login_form_should_support_utf8_passwords() {
    // TODO selenium
  }

  @Test
  public void run_analysis_with_token_authentication() {
    WsUserTokens.GenerateWsResponse generateWsResponse = userTokensWsClient.generate(new GenerateWsRequest()
      .setLogin(LOGIN)
      .setName("Analyze Project"));
    SonarRunner sampleProject = SonarRunner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", generateWsResponse.getToken(),
      "sonar.password", "");

    BuildResult buildResult = ORCHESTRATOR.executeBuild(sampleProject);

    assertThat(buildResult.isSuccess()).isTrue();
  }

  @Test
  public void run_analysis_with_incorrect_token() {
    SonarRunner sampleProject = SonarRunner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", "unknown-token",
      "sonar.password", "");

    BuildResult buildResult = ORCHESTRATOR.executeBuildQuietly(sampleProject);

    assertThat(buildResult.isSuccess()).isFalse();
  }

  private static void createUser(String login, String password) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", login)
        .setParam("password", password));
  }

  private static void createUser(String login, String name, String password) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("password", password));
  }

  private static void addUserPermission(String login, String permission) {
    adminWsClient.permissions().addUser(new AddUserWsRequest()
      .setLogin(login)
      .setPermission(permission));
  }

  private static void deactivateUser(String login) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/deactivate")
        .setParam("login", login));
  }

  private static void removeGroupPermission(String groupName, String permission) {
    adminWsClient.permissions().removeGroup(new RemoveGroupWsRequest()
      .setGroupName(groupName)
      .setPermission(permission));
  }

  private static void addGroupPermission(String groupName, String permission) {
    adminWsClient.permissions().addGroup(new AddGroupWsRequest()
      .setGroupName(groupName)
      .setPermission(permission));
  }
}
