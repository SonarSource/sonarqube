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
package org.sonarqube.tests.ce;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.projectDir;

public class ReportFailureNotificationTest {
  @ClassRule
  public static final Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  private static Wiser smtpServer;

  @BeforeClass
  public static void before() {
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
  public void prepare() {
    tester.settings().setGlobalSettings(
      "email.smtp_host.secured", smtpServer.getServer().getHostName(),
      "email.smtp_port.secured", valueOf(smtpServer.getServer().getPort()));

    smtpServer.getMessages().clear();
  }

  @After
  public void tearDown() throws Exception {
    tester.elasticsearch().unlockWrites("components");
  }

  @After
  public void restoreElasticSearch() throws Exception {
    tester.elasticsearch().unlockWrites("components");
  }

  @Test
  public void send_notification_on_report_processing_failures_to_global_and_project_subscribers() throws Exception {
    Organizations.Organization organization = tester.organizations().getDefaultOrganization();
    Users.CreateWsResponse.User user1 = tester.users().generateMember(organization, t -> t.setPassword("user1").setEmail("user1@bar.com"));
    Users.CreateWsResponse.User user2 = tester.users().generateMember(organization, t -> t.setPassword("user2").setEmail("user2@bar.com"));
    Users.CreateWsResponse.User user3 = tester.users().generateMember(organization, t -> t.setPassword("user3").setEmail("user3@bar.com"));
    Projects.CreateWsResponse.Project project1 = tester.projects().provision(organization, t -> t.setName("Project1"));
    Projects.CreateWsResponse.Project project2 = tester.projects().provision(organization, t -> t.setName("Project2"));
    Projects.CreateWsResponse.Project project3 = tester.projects().provision(organization, t -> t.setName("Project3"));
    // user 1 is admin of project 1 and will subscribe to global notifications
    tester.wsClient().permissions().addUser(new AddUserRequest()
      .setLogin(user1.getLogin())
      .setPermission("admin")
      .setProjectKey(project1.getKey()));
    // user 2 is admin of project 2 but won't subscribe to global notifications
    tester.wsClient().permissions().addUser(new AddUserRequest()
      .setLogin(user2.getLogin())
      .setPermission("admin")
      .setProjectKey(project2.getKey()));
    // user 3 is no admin of any project and will subscribe to global notifications

    // analyses successful and no-one subscribed => no email
    executeAnalysis(project1);
    executeAnalysis(project2);
    executeAnalysis(project3);
    assertThat(waitForEmails()).isEmpty();

    // analyses failed and no-one subscribed => no email
    tester.elasticsearch().lockWrites("components");
    executeAnalysis(project1);
    executeAnalysis(project2);
    executeAnalysis(project3);
    assertThat(waitForEmails()).isEmpty();

    // Add notifications to users: user1 and user3 globally, user2 for project1, user3 for project2
    subscribeToReportFailures(user1, "user1", null);
    subscribeToReportFailures(user2, "user2", project1);
    subscribeToReportFailures(user3, "user3", project2);
    subscribeToReportFailures(user3, "user3", null);

    // analysis failed and 1 global subscriber with admin permission + 1 project subscriber => 2 emails
    executeAnalysis(project1);
    List<MimeMessage> messages = waitForEmails();
    assertThat(messages.stream().flatMap(toToRecipients()).collect(Collectors.toSet()))
      .containsOnly("user1@bar.com", "user2@bar.com");
    assertSubjectAndContent(project1, messages);
    // analysis failed and no global subscriber with admin permission + 1 project subscriber => 1 emails
    executeAnalysis(project2);
    messages = waitForEmails();
    assertThat(messages.stream().flatMap(toToRecipients()).collect(Collectors.toSet()))
      .containsOnly("user3@bar.com");
    assertSubjectAndContent(project2, messages);
    // analysis fails and no global subscriber with admin permission => 0 email
    executeAnalysis(project3);
    messages = waitForEmails();
    assertThat(messages).isEmpty();

    // global and project subscribed but analysis is successful => no email
    tester.elasticsearch().unlockWrites("components");
    executeAnalysis(project1);
    executeAnalysis(project2);
    executeAnalysis(project3);
    assertThat(waitForEmails()).isEmpty();

    // remove notifications from users: user1 globally, user2 for project1, user3 for project2
    //  => no email anyway
    unsubscribeFromReportFailures(user1, "user1", null);
    unsubscribeFromReportFailures(user2, "user2", project1);
    unsubscribeFromReportFailures(user3, "user3", project2);

    // analyses fail and no subscriber but user3 globally but is not admin of any project => no email
    tester.elasticsearch().lockWrites("components");
    executeAnalysis(project1);
    executeAnalysis(project2);
    executeAnalysis(project3);
    assertThat(waitForEmails()).isEmpty();
  }

  private static void assertSubjectAndContent(Projects.CreateWsResponse.Project project, List<MimeMessage> messages) {
    assertThat(messages.stream().map(toSubject()).collect(Collectors.toSet()))
      .containsOnly("[SONARQUBE] " + project.getName() + ": Background task in failure");
    Set<String> content = messages.stream().map(toContent()).collect(Collectors.toSet());
    assertThat(content).hasSize(1);
    assertThat(content.iterator().next())
      .contains("Project:\t" + project.getName(),
        "Background task:\t",
        "Submission time:\t",
        "Failure time:\t",
        "Error message:\tUnrecoverable indexation failures",
        "More details at: http://localhost:9000/project/background_tasks?id=" + project.getKey());
  }

  private static Function<MimeMessage, Stream<String>> toToRecipients() {
    return t -> {
      try {
        return Arrays.stream(t.getRecipients(Message.RecipientType.TO)).map(Address::toString);
      } catch (MessagingException e) {
        return Stream.of();
      }
    };
  }

  private static Function<MimeMessage, String> toSubject() {
    return t -> {
      try {
        return t.getSubject();
      } catch (MessagingException e) {
        return null;
      }
    };
  }

  private static Function<MimeMessage, String> toContent() {
    return t -> {
      try {
        return (String) t.getContent();
      } catch (IOException | MessagingException e) {
        return null;
      }
    };
  }

  private void subscribeToReportFailures(Users.CreateWsResponse.User user1, String password, @Nullable Projects.CreateWsResponse.Project project) {
    WsClient wsClient = newUserWsClient(orchestrator, user1.getLogin(), password);
    PostRequest request = new PostRequest("api/notifications/add")
      .setParam("type", "CeReportTaskFailure")
      .setParam("channel", "EmailNotificationChannel");
    if (project != null) {
      request.setParam("project", project.getKey());
    }
    wsClient.wsConnector().call(request)
      .failIfNotSuccessful();
  }

  private void unsubscribeFromReportFailures(Users.CreateWsResponse.User user1, String password, @Nullable Projects.CreateWsResponse.Project project) {
    WsClient wsClient = newUserWsClient(orchestrator, user1.getLogin(), password);
    PostRequest request = new PostRequest("api/notifications/remove")
      .setParam("type", "CeReportTaskFailure")
      .setParam("channel", "EmailNotificationChannel");
    if (project != null) {
      request.setParam("project", project.getKey());
    }
    wsClient.wsConnector().call(request)
      .failIfNotSuccessful();
  }

  private void executeAnalysis(Projects.CreateWsResponse.Project project) {
    SonarScanner sonarScanner = SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.projectKey", project.getKey(),
      "sonar.projectName", project.getName());
    orchestrator.executeBuild(sonarScanner);
  }

  private static List<MimeMessage> waitForEmails() throws InterruptedException {
    System.out.println("Waiting for new emails...");
    for (int i = 0; i < 10; i++) {
      Thread.sleep(1_000);
      List<WiserMessage> res = new ArrayList<>(smtpServer.getMessages());
      // shortcut, we expect at most 2 emails at a time
      if (res.size() >= 2) {
        smtpServer.getMessages().clear();
        return toMimeMessages(res);
      }
    }
    List<WiserMessage> res = new ArrayList<>(smtpServer.getMessages());
    smtpServer.getMessages().clear();
    return toMimeMessages(res);
  }

  private static List<MimeMessage> toMimeMessages(List<WiserMessage> res) {
    return res.stream().map(t -> {
      try {
        return t.getMimeMessage();
      } catch (MessagingException e) {
        return null;
      }
    }).filter(t -> t != null)
      .collect(Collectors.toList());
  }
}
