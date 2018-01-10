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
package org.sonarqube.tests.settings;

import com.sonar.orchestrator.Orchestrator;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nullable;
import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category1Suite;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.settings.ValuesRequest;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class EmailsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  private static Wiser SMTP_SERVER;

  @BeforeClass
  public static void before() {
    SMTP_SERVER = new Wiser(0);
    SMTP_SERVER.start();
    System.out.println("SMTP Server port: " + SMTP_SERVER.getServer().getPort());
  }

  @AfterClass
  public static void stop() {
    if (SMTP_SERVER != null) {
      SMTP_SERVER.stop();
    }
  }

  @Before
  public void prepare() {
    SMTP_SERVER.getMessages().clear();
  }

  @Test
  public void update_email_settings() {
    updateEmailSettings("localhost", "42", "noreply@email.com", "The Devil", "[EMAIL]", "ssl", "john", "123456");

    Settings.ValuesWsResponse response = tester.settings().service().values(new ValuesRequest()
      .setKeys(asList("email.smtp_host.secured", "email.smtp_port.secured", "email.smtp_secure_connection.secured", "email.smtp_username.secured", "email.smtp_password.secured",
        "email.from", "email.fromName", "email.prefix")));

    assertThat(response.getSettingsList()).extracting(Settings.Setting::getKey, Settings.Setting::getValue)
      .containsOnly(
        tuple("email.smtp_host.secured", "localhost"),
        tuple("email.smtp_port.secured", "42"),
        tuple("email.smtp_secure_connection.secured", "ssl"),
        tuple("email.smtp_username.secured", "john"),
        tuple("email.smtp_password.secured", "123456"),
        tuple("email.from", "noreply@email.com"),
        tuple("email.fromName", "The Devil"),
        tuple("email.prefix", "[EMAIL]"));
  }

  @Test
  public void send_test_email() throws Exception {
    updateEmailSettings("localhost", Integer.toString(SMTP_SERVER.getServer().getPort()), null, null, null, null, null, null);

    sendEmail("test@example.org", "Test Message from SonarQube", "This is a test message from SonarQube");

    // We need to wait until all notifications will be delivered
    waitUntilAllNotificationsAreDelivered(1);
    Iterator<WiserMessage> emails = SMTP_SERVER.getMessages().iterator();
    MimeMessage message = emails.next().getMimeMessage();
    assertThat(Arrays.stream(message.getFrom()).map(Address::toString)).containsOnly("SonarQube <noreply@nowhere>");
    assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
    assertThat(message.getSubject()).isEqualTo("[SONARQUBE] Test Message from SonarQube");
    assertThat((String) message.getContent()).contains("This is a test message from SonarQube");
    assertThat(emails.hasNext()).isFalse();
  }

  @Test
  public void send_customized_test_email() throws Exception {
    String from = randomAlphanumeric(4) + "@" + randomAlphabetic(5);
    String fromName = randomAlphanumeric(5);
    String prefix = randomAlphanumeric(6);
    updateEmailSettings("localhost", Integer.toString(SMTP_SERVER.getServer().getPort()), from, fromName, prefix, null, null, null);

    sendEmail("test@example.org", "Test Message from SonarQube", "This is a test message from SonarQube");

    // We need to wait until all notifications will be delivered
    waitUntilAllNotificationsAreDelivered(1);
    Iterator<WiserMessage> emails = SMTP_SERVER.getMessages().iterator();
    MimeMessage message = emails.next().getMimeMessage();
    assertThat(Arrays.stream(message.getFrom()).map(Address::toString)).containsOnly(format("%s <%s>", fromName, from));
    assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
    assertThat(message.getSubject()).isEqualTo(prefix + " Test Message from SonarQube");
    assertThat((String) message.getContent()).contains("This is a test message from SonarQube");
    assertThat(emails.hasNext()).isFalse();
  }

  private static void waitUntilAllNotificationsAreDelivered(int expectedNumberOfEmails) throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      if (SMTP_SERVER.getMessages().size() == expectedNumberOfEmails) {
        return;
      }
      Thread.sleep(1_000);
    }
    fail(format("Received %d emails, expected %d", SMTP_SERVER.getMessages().size(), expectedNumberOfEmails));
  }

  private void updateEmailSettings(@Nullable String host, @Nullable String port, @Nullable String from, @Nullable String fromName, @Nullable String prefix, @Nullable String secure,
    @Nullable String username, @Nullable String password) {
    tester.settings().setGlobalSettings(
      "email.smtp_host.secured", host,
      "email.smtp_port.secured", port,
      "email.smtp_secure_connection.secured", secure,
      "email.smtp_username.secured", username,
      "email.smtp_password.secured", password,
      "email.from", from,
      "email.fromName", fromName,
      "email.prefix", prefix);
  }

  private void sendEmail(String to, String subject, String message) {
    tester.wsClient().wsConnector().call(
      new PostRequest("/api/emails/send")
        .setParam("to", to)
        .setParam("subject", subject)
        .setParam("message", message))
      .failIfNotSuccessful();
  }

}
