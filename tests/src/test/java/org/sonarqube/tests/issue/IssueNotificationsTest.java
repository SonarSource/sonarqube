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

import com.sonar.orchestrator.Orchestrator;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.issues.AssignRequest;
import org.sonarqube.ws.client.issues.BulkChangeRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.notifications.AddRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static util.ItUtils.formatDate;
import static util.ItUtils.runProjectAnalysis;

public class IssueNotificationsTest {

  private static final String PAST_ANALYSIS_DATE = formatDate(addDays(new Date(), -30));

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = IssueSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR).disableOrganizations();

  private static Wiser smtpServer;

  @BeforeClass
  public static void setUp() {
    smtpServer = new Wiser(0);
    smtpServer.start();
  }

  @AfterClass
  public static void stop() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  @Before
  public void before() {
    tester.settings().setGlobalSettings("email.smtp_host.secured", "localhost");
    tester.settings().setGlobalSettings("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()));
    clearSmtpMessages();
  }

  @Test
  public void notification_for_NewIssues() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    Project project = tester.projects().provision();
    createSampleQProfile(project);
    User userReceivingNotification = tester.users().generate();
    tester.as(userReceivingNotification.getLogin()).wsClient().notifications().add(new AddRequest().setType("NewIssues").setChannel("EmailNotificationChannel"));
    User anotherUser = tester.users().generate();

    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.projectKey", project.getKey(),
      "sonar.projectDate", PAST_ANALYSIS_DATE,
      "sonar.projectVersion", version);
    checkNotificationSent(1);

    assertThat(extractRecipients(smtpServer.getMessages()))
      .containsExactlyInAnyOrder(
        format("<%s>", userReceivingNotification.getEmail()));
    extractBodies(smtpServer.getMessages()).forEach(
      message -> assertThat(message)
        .contains("Project: Sample")
        .contains("Version: " + version)
        .contains("17 new issues (new debt: 17min)")
        .contains("Type")
        .contains("Bug: ").contains("Code Smell: ").contains("Vulnerability: ")
        .contains("One Issue Per Line (xoo): 17")
        .contains("More details at: http://localhost:9000/project/issues?id=" + project.getKey() + "&createdAt=" + PAST_ANALYSIS_DATE + "T00%3A00%3A00%2B"));
  }

  @Test
  public void notification_for_ChangesOnMyIssue() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    Project project = tester.projects().provision();
    createSampleQProfile(project);
    User user1 = tester.users().generate();
    tester.as(user1.getLogin()).wsClient().notifications().add(new AddRequest().setType("ChangesOnMyIssue").setChannel("EmailNotificationChannel"));
    User user2 = tester.users().generate();
    tester.as(user2.getLogin()).wsClient().notifications().add(new AddRequest().setType("ChangesOnMyIssue").setChannel("EmailNotificationChannel"));
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.projectKey", project.getKey(),
      "sonar.projectDate", PAST_ANALYSIS_DATE,
      "sonar.projectVersion", version);
    // Ignore the messages generated by the analysis
    clearSmtpMessages();

    // Change assignee
    Issues.SearchWsResponse issues = tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList(project.getKey())));
    Issues.Issue issue = issues.getIssuesList().get(0);
    tester.wsClient().issues().assign(new AssignRequest().setIssue(issue.getKey()).setAssignee(user1.getLogin()));
    checkNotificationSent(1);

    // Only the assignee should receive the email
    assertThat(extractRecipients(smtpServer.getMessages())).containsExactly(format("<%s>", user1.getEmail()));
    assertThat(extractBodies(smtpServer.getMessages()).get(0))
      .contains("sample/Sample.xoo")
      .contains(format("Assignee changed to %s", user1.getName()))
      .contains("More details at: http://localhost:9000/project/issues?id=" + project.getKey() + "&issues=" + issue.getKey() + "&open=" + issue.getKey());
  }

  @Test
  public void notification_for_MyNewIssue() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    Project project = tester.projects().provision();
    createSampleQProfile(project);
    // User1 is the default assignee
    User user1 = tester.users().generate();
    tester.as(user1.getLogin()).wsClient().notifications().add(new AddRequest().setType("SQ-MyNewIssues").setChannel("EmailNotificationChannel"));
    tester.settings().setGlobalSettings("sonar.issues.defaultAssigneeLogin", user1.getLogin());
    // User2 should not receive any email
    User user2 = tester.users().generate();
    tester.as(user2.getLogin()).wsClient().notifications().add(new AddRequest().setType("SQ-MyNewIssues").setChannel("EmailNotificationChannel"));

    // 1st analysis without any issue (because no file is analyzed)
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-with-scm",
      "sonar.projectKey", project.getKey(),
      "sonar.projectVersion", version,
      "sonar.scm.provider", "xoo",
      "sonar.scm.disabled", "false",
      "sonar.exclusions", "**/*");
    // No email since all code is ignored
    checkNoNotificationSent();

    // run 2nd analysis which will generate issues on the leak period
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-with-scm",
      "sonar.projectKey", project.getKey(),
      "sonar.projectVersion", version,
      "sonar.scm.provider", "xoo",
      "sonar.scm.disabled", "false");
    checkNotificationSent(1);
    MimeMessage message = smtpServer.getMessages().get(0).getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo(format("<%s>", user1.getEmail()));
    assertThat(message.getSubject()).contains("You have 13 new issues");
    assertThat((String) message.getContent())
      .contains("Project: Sample")
      .contains("Version: " + version);
  }

  /**
   * SONAR-4606
   */
  @Test
  public void notification_for_bulk_change_ws() throws Exception {
    String version = RandomStringUtils.randomAlphanumeric(10);
    Project project = tester.projects().provision();
    createSampleQProfile(project);
    User user1 = tester.users().generate();
    tester.as(user1.getLogin()).wsClient().notifications().add(new AddRequest().setType("ChangesOnMyIssue").setChannel("EmailNotificationChannel"));
    User user2 = tester.users().generate();
    tester.as(user2.getLogin()).wsClient().notifications().add(new AddRequest().setType("ChangesOnMyIssue").setChannel("EmailNotificationChannel"));
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.projectKey", project.getKey(),
      "sonar.projectVersion", version,
      "sonar.projectDate", PAST_ANALYSIS_DATE);
    checkNoNotificationSent();
    clearSmtpMessages();
    Issues.SearchWsResponse issues = tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList(project.getKey())));
    Issues.Issue issue = issues.getIssuesList().get(0);

    // bulk change without notification by default
    tester.wsClient().issues().bulkChange(new BulkChangeRequest()
      .setIssues(singletonList(issue.getKey()))
      .setAssign(singletonList(user1.getLogin()))
      .setSetSeverity(singletonList("MINOR")));
    checkNoNotificationSent();

    // bulk change with notification
    tester.wsClient().issues().bulkChange(new BulkChangeRequest()
      .setIssues(singletonList(issue.getKey()))
      .setSetSeverity(singletonList("BLOCKER"))
      .setSendNotifications("true"));
    checkNotificationSent(1);
    assertThat(extractRecipients(smtpServer.getMessages()))
      .containsExactly(format("<%s>", user1.getEmail()));
    assertThat(extractBodies(smtpServer.getMessages()).get(0))
      .contains("sample/Sample.xoo")
      .contains("Severity: BLOCKER (was MINOR)")
      .contains("More details at: http://localhost:9000/project/issues?id=" + project.getKey() + "&issues=" + issue.getKey() + "&open=" + issue.getKey());
  }

  @Test
  public void notification_on_private_project() throws Exception {
    // Create a private project using an empty permission template, in order for noone to be able to access it by default
    Project project = tester.projects().provision(p -> p.setVisibility("private"));
    Permissions.PermissionTemplate permissionTemplate = tester.permissions().generateTemplate();
    tester.permissions().applyTemplate(permissionTemplate, project);
    createSampleQProfile(project);
    // User having browse permission on the project
    User userWithBrowsePermission = tester.users().generate();
    tester.as(userWithBrowsePermission.getLogin()).wsClient().notifications().add(new AddRequest().setType("NewIssues").setChannel("EmailNotificationChannel"));
    tester.wsClient().permissions().addUser(
      new AddUserRequest()
        .setLogin(userWithBrowsePermission.getLogin())
        .setProjectKey(project.getKey())
        .setPermission("user"));
    // User not having browse permission on the project
    User userWithoutBrowsePermission = tester.users().generate();
    tester.as(userWithoutBrowsePermission.getLogin()).wsClient().notifications().add(new AddRequest().setType("NewIssues").setChannel("EmailNotificationChannel"));

    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.projectKey", project.getKey(),
      "sonar.projectDate", PAST_ANALYSIS_DATE);
    checkNotificationSent(1);

    assertThat(extractRecipients(smtpServer.getMessages()))
      .containsExactlyInAnyOrder(
        format("<%s>", userWithBrowsePermission.getEmail()))
      .doesNotContain(userWithoutBrowsePermission.getEmail());
  }

  private void createSampleQProfile(Project project) {
    // Create project
    Qualityprofiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile();
    tester.qProfiles()
      .activateRule(profile, "xoo:OneIssuePerLine")
      .assignQProfileToProject(profile, project);
  }

  private static void checkNotificationSent(int expectedNumberOfEmails) throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      if (smtpServer.getMessages().size() == expectedNumberOfEmails) {
        break;
      }
      Thread.sleep(1_000);
    }
    assertThat(smtpServer.getMessages()).hasSize(expectedNumberOfEmails);
  }

  private static void checkNoNotificationSent() throws InterruptedException {
    Thread.sleep(1_000);
    assertThat(smtpServer.getMessages()).isEmpty();
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
}
