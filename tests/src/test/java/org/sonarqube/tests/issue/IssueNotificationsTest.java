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
package org.sonarqube.tests.issue;

import com.google.common.collect.ObjectArrays;
import com.sonar.orchestrator.Orchestrator;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issues.AssignRequest;
import org.sonarqube.ws.client.issues.BulkChangeRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static util.ItUtils.runProjectAnalysis;

@RunWith(Parameterized.class)
public class IssueNotificationsTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR);

  private final static String EMAIL_TEST = "test@test.com";
  private final static String PROJECT_KEY = "sample";

  private static Wiser smtpServer;

  private Organization organization;
  private User userWithUserRole;
  private User userWithUserRoleThroughGroups;
  private User userNotInOrganization;

  @Parameters
  public static List<Boolean> data() {
    return Arrays.asList(true, false);
  }

  @Parameter
  public boolean privateProject;

  @BeforeClass
  public static void setUp() {
    smtpServer = new Wiser(0);
    smtpServer.start();
    System.out.println("SMTP Server port: " + smtpServer.getServer().getPort());
  }

  @AfterClass
  public static void stop() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  @Before
  public void before() throws Exception {
    organization = tester.organizations().generate();

    // Configure Sonar
    tester.settings().setGlobalSettings("email.smtp_host.secured", "localhost");
    tester.settings().setGlobalSettings("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()));

    clearSmtpMessages();
    checkEmailSettings();
    clearSmtpMessages();
  }

  @After
  public void after() {
    clearSmtpMessages();
  }

  @Test
  public void notifications_on_new_issues_should_send_emails_to_subscribers() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    createSampleProject(privateProject ? "private" : "public");
    createUsers();
    runAnalysis("shared/xoo-sample",
      "sonar.projectVersion", version,
      "sonar.projectDate", "2015-12-15");

    // If project is private userNotInOrganization will not receive and email (missing UserRole.User permission)
    waitUntilAllNotificationsAreDelivered(privateProject ? 2 : 3);
    assertThat(smtpServer.getMessages()).hasSize(privateProject ? 2 : 3);

    if (privateProject) {
      assertThat(extractRecipients(smtpServer.getMessages()))
        .containsExactlyInAnyOrder(
          format("<%s>", userWithUserRole.getEmail()),
          format("<%s>", userWithUserRoleThroughGroups.getEmail()));
    } else {
      assertThat(extractRecipients(smtpServer.getMessages()))
        .containsExactlyInAnyOrder(
          format("<%s>", userWithUserRole.getEmail()),
          format("<%s>", userWithUserRoleThroughGroups.getEmail()),
          format("<%s>", userNotInOrganization.getEmail()));
    }

    extractBodies(smtpServer.getMessages()).forEach(
      message -> assertThat(message)
        .contains("Project: Sample")
        .contains("Version: " + version)
        .contains("17 new issues (new debt: 17min)")
        .contains("Type")
        .contains("Bug: ").contains("Code Smell: ").contains("Vulnerability: ")
        .contains("One Issue Per Line (xoo): 17")
        .contains("More details at: http://localhost:9000/project/issues?id=sample&createdAt=2015-12-15T00%3A00%3A00%2B"));

    clearSmtpMessages();
  }

  @Test
  public void notifications_for_issue_changes() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    createSampleProject(privateProject ? "private" : "public");
    createUsers();
    runAnalysis("shared/xoo-sample",
      "sonar.projectVersion", version,
      "sonar.projectDate", "2015-12-15");

    // Ignore the messages generated by the analysis
    clearSmtpMessages();

    // Change assignee
    SearchWsResponse issues = tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList(PROJECT_KEY)));
    Issue issue = issues.getIssuesList().get(0);
    tester.wsClient().issues().assign(new AssignRequest().setIssue(issue.getKey()).setAssignee(userWithUserRole.getLogin()));

    // Only the assignee should receive the email
    waitUntilAllNotificationsAreDelivered(1);
    assertThat(smtpServer.getMessages()).hasSize(1);
    assertThat(extractRecipients(smtpServer.getMessages())).containsExactly(format("<%s>", userWithUserRole.getEmail()));

    assertThat(extractBodies(smtpServer.getMessages()).get(0))
      .contains("sample/Sample.xoo")
      .contains("Assignee changed to userWithUserRole")
      .contains("More details at: http://localhost:9000/project/issues?id=sample&issues=" + issue.getKey() + "&open=" + issue.getKey());
  }

  @Test
  public void notifications_for_personalized_emails() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    // 1st analysis without any issue (because no file is analyzed)
    createSampleProject(privateProject ? "private" : "public");
    createUsers();
    tester.settings().setGlobalSettings("sonar.issues.defaultAssigneeLogin", userWithUserRole.getLogin());

    runAnalysis("issue/xoo-with-scm",
      "sonar.projectVersion", version,
      "sonar.scm.provider", "xoo",
      "sonar.scm.disabled", "false",
      "sonar.exclusions", "**/*");

    // No email since all code is ignored
    waitUntilAllNotificationsAreDelivered(1);
    assertThat(smtpServer.getMessages()).isEmpty();

    // run 2nd analysis which will generate issues on the leak period
    runAnalysis("issue/xoo-with-scm",
      "sonar.projectVersion", version,
      "sonar.scm.provider", "xoo",
      "sonar.scm.disabled", "false");

    // We expect to receive a notification for each subscriber with UserRole.user
    // And a personalized email for the assignee userWithUserRole
    waitUntilAllNotificationsAreDelivered(privateProject ? 3 : 4);
    assertThat(smtpServer.getMessages()).hasSize(privateProject ? 3 : 4);

    // the last email sent is the personalized one
    MimeMessage message = smtpServer.getMessages().get(privateProject ? 2 : 3).getMimeMessage();

    assertThat(message.getHeader("To", null)).isEqualTo(format("<%s>", userWithUserRole.getEmail()));
    assertThat(message.getSubject()).contains("You have 13 new issues");
    assertThat((String) message.getContent())
      .contains("Project: Sample")
      .contains("Version: " + version);
  }

  /**
   * SONAR-4606
   */
  @Test
  public void notifications_for_bulk_change_ws() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    createSampleProject(privateProject ? "private" : "public");
    createUsers();
    runAnalysis("shared/xoo-sample",
      "sonar.projectVersion", version,
      "sonar.projectDate", "2015-12-15");

    // If project is private userNotInOrganization will not receive and email (missing UserRole.User permission)
    waitUntilAllNotificationsAreDelivered(privateProject ? 2 : 3);
    assertThat(smtpServer.getMessages()).hasSize(privateProject ? 2 : 3);
    clearSmtpMessages();

    SearchWsResponse issues = tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList(PROJECT_KEY)));
    Issue issue = issues.getIssuesList().get(0);

    // bulk change without notification by default
    tester.wsClient().issues().bulkChange(new BulkChangeRequest()
      .setIssues(singletonList(issue.getKey()))
      .setAssign(singletonList(userWithUserRole.getLogin()))
      .setSetSeverity(singletonList("MINOR")));

    // bulk change with notification
    tester.wsClient().issues().bulkChange(new BulkChangeRequest()
      .setIssues(singletonList(issue.getKey()))
      .setSetSeverity(singletonList("BLOCKER"))
      .setSendNotifications("true"));

    // We are waiting for a single notification for userWithUserRole
    // for a change on MyIssues
    waitUntilAllNotificationsAreDelivered(1);
    assertThat(smtpServer.getMessages()).hasSize(1);

    assertThat(extractRecipients(smtpServer.getMessages()))
      .containsExactly(format("<%s>", userWithUserRole.getEmail()));
    assertThat(extractBodies(smtpServer.getMessages()).get(0))
      .contains("sample/Sample.xoo")
      .contains("Severity: BLOCKER (was MINOR)")
      .contains("More details at: http://localhost:9000/project/issues?id=sample&issues=" + issue.getKey() + "&open=" + issue.getKey());
  }

  private void runAnalysis(String projectRelativePath, String... extraParameters) throws Exception {
    String[] parameters = {
      "sonar.login", "admin",
      "sonar.password", "admin",
      "sonar.organization", organization.getKey()};
    runProjectAnalysis(ORCHESTRATOR, projectRelativePath,
      ObjectArrays.concat(parameters, extraParameters, String.class));

    // Two emails should be sent for subscribers of "New issues"
    waitUntilAllNotificationsAreDelivered(2);
  }

  private void createSampleProject(String visibility) {
    // Create project
    Qualityprofiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    Project project = tester.projects().provision(organization, p -> p.setProject(PROJECT_KEY)
      .setName("Sample")
      .setVisibility(visibility));
    tester.qProfiles()
      .activateRule(profile, "xoo:OneIssuePerLine")
      .assignQProfileToProject(profile, project);
  }

  private void createUsers() {
    // Create a user with User role
    userWithUserRole = tester.users().generateMember(organization,
      u -> u.setLogin("userWithUserRole")
        .setPassword("userWithUserRole")
        .setName("userWithUserRole")
        .setEmail("userWithUserRole@nowhere.com"));
    tester.organizations().addMember(organization, userWithUserRole);
    tester.wsClient().permissions().addUser(
      new AddUserRequest()
        .setLogin(userWithUserRole.getLogin())
        .setProjectKey(PROJECT_KEY)
        .setPermission("user"));
    addNotificationsTo(userWithUserRole);

    // Create a user that have User role through Members group
    userWithUserRoleThroughGroups = tester.users().generate(
      u -> u.setLogin("userWithUserRoleThroughGroups")
        .setPassword("userWithUserRoleThroughGroups")
        .setName("userWithUserRoleThroughGroups")
        .setEmail("userWithUserRoleThroughGroups@nowhere.com"));
    tester.organizations().addMember(organization, userWithUserRoleThroughGroups);
    addNotificationsTo(userWithUserRoleThroughGroups);

    // Create a user that does not belongs to organization
    userNotInOrganization = tester.users().generate(
      u -> u.setLogin("userNotInOrganization")
        .setPassword("userNotInOrganization")
        .setName("userNotInOrganization")
        .setEmail("userNotInOrganization@nowhere.com"));
    addNotificationsTo(userNotInOrganization);
  }

  void checkEmailSettings() throws Exception {
    // Send test email to the test user
    tester.wsClient().wsConnector().call(new PostRequest("api/emails/send")
      .setParam("to", EMAIL_TEST)
      .setParam("message", "This is a test message from SonarQube"))
      .failIfNotSuccessful();

    // We need to wait until all notifications will be delivered
    waitUntilAllNotificationsAreDelivered(1);

    assertThat(smtpServer.getMessages()).hasSize(1);

    MimeMessage message = smtpServer.getMessages().get(0).getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<" + EMAIL_TEST + ">");
    assertThat((String) message.getContent()).contains("This is a test message from SonarQube");
  }

  private static void waitUntilAllNotificationsAreDelivered(int expectedNumberOfEmails) throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      if (smtpServer.getMessages().size() == expectedNumberOfEmails) {
        break;
      }
      Thread.sleep(1_000);
    }
  }

  private static void clearSmtpMessages() {
    synchronized (smtpServer.getMessages()) {
      smtpServer.getMessages().clear();
    }
  }

  private List<String> extractRecipients(List<WiserMessage> messages) {
    return messages.stream()
      .map(m -> {
        try {
          return m.getMimeMessage().getHeader("To", null);
        } catch (MessagingException e) {
          fail(e.getMessage(), e);
          return null;
        }
      }).collect(Collectors.toList());
  }

  private List<String> extractBodies(List<WiserMessage> messages) {
    return messages.stream()
      .map(m -> {
        try {
          return m.getMimeMessage().getContent().toString();
        } catch (MessagingException | IOException e) {
          fail(e.getMessage(), e);
          return null;
        }
      }).collect(Collectors.toList());
  }

  private void addNotificationsTo(Users.CreateWsResponse.User user) {
    // Add notifications to the test user
    WsClient wsClient = tester.as(user.getLogin()).wsClient();
    wsClient.wsConnector().call(new PostRequest("api/notifications/add")
      .setParam("type", "NewIssues")
      .setParam("channel", "EmailNotificationChannel"))
      .failIfNotSuccessful();
    wsClient.wsConnector().call(new PostRequest("api/notifications/add")
      .setParam("type", "ChangesOnMyIssue")
      .setParam("channel", "EmailNotificationChannel"))
      .failIfNotSuccessful();
    wsClient.wsConnector().call(new PostRequest("api/notifications/add")
      .setParam("type", "SQ-MyNewIssues")
      .setParam("channel", "EmailNotificationChannel"))
      .failIfNotSuccessful();
  }
}
