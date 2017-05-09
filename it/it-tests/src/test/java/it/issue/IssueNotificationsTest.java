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
package it.issue;

import java.util.Iterator;
import javax.mail.internet.MimeMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issue.BulkChangeRequest;
import org.sonarqube.ws.client.issue.IssuesService;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;
import util.ItUtils;
import util.user.UserRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.resetEmailSettings;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class IssueNotificationsTest extends AbstractIssueTest {

  private final static String PROJECT_KEY = "sample";
  private final static String USER_LOGIN = "tester";
  private final static String USER_PASSWORD = "tester";
  private static final String USER_EMAIL = "tester@example.org";

  private static Wiser smtpServer;

  private IssueClient issueClient;

  private IssuesService issuesService;

  @ClassRule
  public static UserRule userRule = UserRule.from(ORCHESTRATOR);

  @BeforeClass
  public static void before() throws Exception {
    smtpServer = new Wiser(0);
    smtpServer.start();
    System.out.println("SMTP Server port: " + smtpServer.getServer().getPort());

    // Configure Sonar
    resetEmailSettings(ORCHESTRATOR);
    setServerProperty(ORCHESTRATOR, "email.smtp_host.secured", "localhost");
    setServerProperty(ORCHESTRATOR, "email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()));

    // Send test email to the test user
    newAdminWsClient(ORCHESTRATOR).wsConnector().call(new PostRequest("api/emails/send")
      .setParam("to", USER_EMAIL)
      .setParam("message", "This is a test message from SonarQube"))
      .failIfNotSuccessful();

    // We need to wait until all notifications will be delivered
    waitUntilAllNotificationsAreDelivered(1);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<" + USER_EMAIL + ">");
    assertThat((String) message.getContent()).contains("This is a test message from SonarQube");

    assertThat(emails.hasNext()).isFalse();
  }

  @AfterClass
  public static void stop() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
    userRule.deactivateUsers(USER_LOGIN);
    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", null);
    resetEmailSettings(ORCHESTRATOR);
  }

  @Before
  public void prepare() {
    ORCHESTRATOR.resetData();

    // Create test user
    userRule.createUser(USER_LOGIN, "Tester", USER_EMAIL, USER_LOGIN);

    smtpServer.getMessages().clear();
    issueClient = ORCHESTRATOR.getServer().adminWsClient().issueClient();
    issuesService = newAdminWsClient(ORCHESTRATOR).issues();

    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", null);
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, "Sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line-profile");

    // Add notifications to the test user
    WsClient wsClient = newUserWsClient(ORCHESTRATOR, USER_LOGIN, USER_PASSWORD);
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

  @Test
  public void notifications_for_new_issues_and_issue_changes() throws Exception {
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample", "sonar.projectDate", "2015-12-15");

    // change assignee
    Issues issues = issueClient.find(IssueQuery.create().componentRoots(PROJECT_KEY));
    Issue issue = issues.list().get(0);
    issueClient.assign(issue.key(), USER_LOGIN);

    waitUntilAllNotificationsAreDelivered(2);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    assertThat(emails.hasNext()).isTrue();
    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("Sample");
    assertThat((String) message.getContent()).contains("17 new issues (new debt: 17min)");
    assertThat((String) message.getContent()).contains("Severity");
    assertThat((String) message.getContent()).contains("One Issue Per Line (xoo): 17");
    assertThat((String) message.getContent()).contains(
      "See it in SonarQube: http://localhost:9000/project/issues?id=sample&createdAt=2015-12-15T00%3A00%3A00%2B");

    assertThat(emails.hasNext()).isTrue();
    message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("sample/Sample.xoo");
    assertThat((String) message.getContent()).contains("Assignee changed to Tester");
    assertThat((String) message.getContent()).contains(
      "See it in SonarQube: http://localhost:9000/project/issues?id=sample&issues=" + issue.key() + "&open=" + issue.key());

    assertThat(emails.hasNext()).isFalse();
  }

  @Test
  public void notifications_for_personalized_emails() throws Exception {
    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", USER_LOGIN);
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-with-scm", "sonar.scm.provider", "xoo", "sonar.scm.disabled", "false");

    waitUntilAllNotificationsAreDelivered(2);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();
    emails.next();
    // the second email sent is the personalized one
    MimeMessage message = emails.next().getMimeMessage();

    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat(message.getSubject()).contains("You have 13 new issues");
  }

  /**
   * SONAR-4606
   */
  @Test
  public void notifications_for_bulk_change_ws() throws Exception {
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample", "sonar.projectDate", "2015-12-15");

    Issues issues = issueClient.find(IssueQuery.create().componentRoots(PROJECT_KEY));
    Issue issue = issues.list().get(0);

    // bulk change without notification by default
    issuesService.bulkChange(BulkChangeRequest.builder()
      .setIssues(singletonList(issue.key()))
      .setAssign(USER_LOGIN)
      .setSetSeverity("MINOR")
      .build());

    // bulk change with notification
    issuesService.bulkChange(BulkChangeRequest.builder()
      .setIssues(singletonList(issue.key()))
      .setSetSeverity("BLOCKER")
      .setSendNotifications(true)
      .build());

    waitUntilAllNotificationsAreDelivered(2);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    emails.next();
    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("sample/Sample.xoo");
    assertThat((String) message.getContent()).contains("Severity: BLOCKER (was MINOR)");
    assertThat((String) message.getContent()).contains(
      "See it in SonarQube: http://localhost:9000/project/issues?id=sample&issues=" + issue.key() + "&open=" + issue.key());

    assertThat(emails.hasNext()).isFalse();
  }

  private static void waitUntilAllNotificationsAreDelivered(int expectedNumberOfEmails) throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      if (smtpServer.getMessages().size() == expectedNumberOfEmails) {
        break;
      }
      Thread.sleep(1_000);
    }
  }

}
