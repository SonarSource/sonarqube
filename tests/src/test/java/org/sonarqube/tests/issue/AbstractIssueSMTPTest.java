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

package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import javax.mail.internet.MimeMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.subethamail.wiser.Wiser;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractIssueSMTPTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR);

  static Wiser smtpServer;

  private final static String EMAIL_TEST = "test@test.com";

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
  public final void before() throws Exception {
    // Configure Sonar
    tester.settings().setGlobalSettings("email.smtp_host.secured", "localhost");
    tester.settings().setGlobalSettings("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()));

    clearSmtpMessages();
    checkEmailSettings();
    clearSmtpMessages();
  }

  @After
  public final void after() throws Exception {
    clearSmtpMessages();
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

  static void waitUntilAllNotificationsAreDelivered(int expectedNumberOfEmails) throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      if (smtpServer.getMessages().size() == expectedNumberOfEmails) {
        break;
      }
      Thread.sleep(1_000);
    }
  }

  static void clearSmtpMessages() {
    synchronized (smtpServer.getMessages()) {
      smtpServer.getMessages().clear();
    }
  }

  void addNotificationsTo(WsUsers.CreateWsResponse.User user) {
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
