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
package it.issue;

import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import java.util.Iterator;
import javax.mail.internet.MimeMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.BulkChangeQuery;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;
import util.selenium.SeleneseTest;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class IssueNotificationsTest extends AbstractIssueTest {

  private final static String PROJECT_KEY = "sample";
  private final static String USER_LOGIN = "tester";

  private static Wiser smtpServer;

  private IssueClient issueClient;

  @ClassRule
  public static UserRule userRule = UserRule.from(ORCHESTRATOR);

  @BeforeClass
  public static void before() throws Exception {
    smtpServer = new Wiser(0);
    smtpServer.start();
    System.out.println("SMTP Server port: " + smtpServer.getServer().getPort());

    // Configure Sonar
    setServerProperty(ORCHESTRATOR, "email.smtp_host.secured", "localhost");
    setServerProperty(ORCHESTRATOR, "email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()));

    // Create test user
    userRule.createUser(USER_LOGIN, "Tester", "tester@example.org", USER_LOGIN);

    // 1. Check that SMTP server was turned on and able to send test email
    // 2. Create user, which will receive notifications for new violations
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("issue-notifications",
        "/issue/IssueNotificationsTest/email_configuration.html",
        "/issue/IssueNotificationsTest/user_notifications_settings.html").build()
    ).runOn(ORCHESTRATOR);

    // We need to wait until all notifications will be delivered
    waitUntilAllNotificationsAreDelivered();

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
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
  }

  @Before
  public void prepare() {
    ORCHESTRATOR.resetData();
    smtpServer.getMessages().clear();
    issueClient = ORCHESTRATOR.getServer().adminWsClient().issueClient();

    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", null);
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, "Sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line-profile");
  }

  @Test
  public void notifications_for_new_issues_and_issue_changes() throws Exception {
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample", "sonar.projectDate", "2011-12-15");

    // change assignee
    Issues issues = issueClient.find(IssueQuery.create().componentRoots(PROJECT_KEY));
    Issue issue = issues.list().get(0);
    issueClient.assign(issue.key(), USER_LOGIN);

    waitUntilAllNotificationsAreDelivered();

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    assertThat(emails.hasNext()).isTrue();
    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("Sample");
    assertThat((String) message.getContent()).contains("17 new issues (new debt: 17min)");
    assertThat((String) message.getContent()).contains("Severity");
    assertThat((String) message.getContent()).contains("One Issue Per Line (xoo): 17");
    assertThat((String) message.getContent()).contains(
      "See it in SonarQube: http://localhost:9000/component_issues?id=sample#createdAt=2011-12-15T00%3A00%3A00%2B");

    assertThat(emails.hasNext()).isTrue();
    message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("sample/Sample.xoo");
    assertThat((String) message.getContent()).contains("Assignee changed to Tester");
    assertThat((String) message.getContent()).contains("See it in SonarQube: http://localhost:9000/issues/search#issues=" + issue.key());

    assertThat(emails.hasNext()).isFalse();
  }

  @Test
  public void notifications_for_personalized_emails() throws Exception {
    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", USER_LOGIN);
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-with-scm", "sonar.scm.provider", "xoo", "sonar.scm.disabled", "false");

    waitUntilAllNotificationsAreDelivered();

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
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample", "sonar.projectDate", "2011-12-15");

    Issues issues = issueClient.find(IssueQuery.create().componentRoots(PROJECT_KEY));
    Issue issue = issues.list().get(0);

    // bulk change without notification by default
    issueClient.bulkChange(BulkChangeQuery.create().issues(issue.key())
      .actions("assign", "set_severity")
      .actionParameter("assign", "assignee", USER_LOGIN)
      .actionParameter("set_severity", "severity", "MINOR"));

    // bulk change with notification
    issueClient.bulkChange(BulkChangeQuery.create().issues(issue.key())
      .actions("set_severity")
      .actionParameter("set_severity", "severity", "BLOCKER")
      .sendNotifications(true));

    waitUntilAllNotificationsAreDelivered();

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    emails.next();
    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("sample/Sample.xoo");
    assertThat((String) message.getContent()).contains("Severity: BLOCKER (was MINOR)");
    assertThat((String) message.getContent()).contains(
      "See it in SonarQube: http://localhost:9000/issues/search#issues=" + issue.key());

    assertThat(emails.hasNext()).isFalse();
  }

  private static void waitUntilAllNotificationsAreDelivered() throws InterruptedException {
    Thread.sleep(10000);
  }

}
