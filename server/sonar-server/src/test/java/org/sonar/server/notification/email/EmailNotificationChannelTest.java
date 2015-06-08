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
package org.sonar.server.notification.email;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import javax.mail.internet.MimeMessage;
import org.apache.commons.mail.EmailException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.EmailSettings;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmailNotificationChannelTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private int port;
  private Wiser server;
  private EmailSettings configuration;
  private EmailNotificationChannel channel;

  private static int getNextAvailablePort() {
    try {
      ServerSocket socket = new ServerSocket(0);
      int unusedPort = socket.getLocalPort();
      socket.close();
      return unusedPort;
    } catch (IOException e) {
      throw new RuntimeException("Error getting an available port from system", e);
    }
  }

  @Before
  public void setUp() {
    port = getNextAvailablePort();
    server = new Wiser();
    server.setPort(port);
    server.start();

    configuration = mock(EmailSettings.class);
    channel = new EmailNotificationChannel(configuration, null, null);
  }

  @After
  public void tearDown() {
    server.stop();
  }

  @Test
  public void shouldSendTestEmail() throws Exception {
    configure();
    channel.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a test message from SonarQube.");

    List<WiserMessage> messages = server.getMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages.get(0).getMimeMessage();
    assertThat(email.getHeader("Content-Type", null)).isEqualTo("text/plain; charset=UTF-8");
    assertThat(email.getHeader("From", ",")).isEqualTo("SonarQube <server@nowhere>");
    assertThat(email.getHeader("To", null)).isEqualTo("<user@nowhere>");
    assertThat(email.getHeader("Subject", null)).isEqualTo("[SONARQUBE] Test Message from SonarQube");
    assertThat((String) email.getContent()).startsWith("This is a test message from SonarQube.");
  }

  @Test
  public void shouldThrowAnExceptionWhenUnableToSendTestEmail() {
    configure();
    server.stop();

    try {
      channel.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a test message from SonarQube.");
      fail();
    } catch (EmailException e) {
      // expected
    }
  }

  @Test
  public void shouldNotSendEmailWhenHostnameNotConfigured() {
    EmailMessage emailMessage = new EmailMessage()
      .setTo("user@nowhere")
      .setSubject("Foo")
      .setMessage("Bar");
    channel.deliver(emailMessage);
    assertThat(server.getMessages()).isEmpty();
  }

  @Test
  public void shouldSendThreadedEmail() throws Exception {
    configure();
    EmailMessage emailMessage = new EmailMessage()
      .setMessageId("reviews/view/1")
      .setFrom("Full Username")
      .setTo("user@nowhere")
      .setSubject("Review #3")
      .setMessage("I'll take care of this violation.");
    channel.deliver(emailMessage);

    List<WiserMessage> messages = server.getMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages.get(0).getMimeMessage();

    assertThat(email.getHeader("Content-Type", null)).isEqualTo("text/plain; charset=UTF-8");

    assertThat(email.getHeader("In-Reply-To", null)).isEqualTo("<reviews/view/1@nemo.sonarsource.org>");
    assertThat(email.getHeader("References", null)).isEqualTo("<reviews/view/1@nemo.sonarsource.org>");

    assertThat(email.getHeader("List-ID", null)).isEqualTo("SonarQube <sonar.nemo.sonarsource.org>");
    assertThat(email.getHeader("List-Archive", null)).isEqualTo("http://nemo.sonarsource.org");

    assertThat(email.getHeader("From", ",")).isEqualTo("\"Full Username (SonarQube)\" <server@nowhere>");
    assertThat(email.getHeader("To", null)).isEqualTo("<user@nowhere>");
    assertThat(email.getHeader("Subject", null)).isEqualTo("[SONARQUBE] Review #3");
    assertThat((String) email.getContent()).startsWith("I'll take care of this violation.");
  }

  @Test
  public void shouldSendNonThreadedEmail() throws Exception {
    configure();
    EmailMessage emailMessage = new EmailMessage()
      .setTo("user@nowhere")
      .setSubject("Foo")
      .setMessage("Bar");
    channel.deliver(emailMessage);

    List<WiserMessage> messages = server.getMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages.get(0).getMimeMessage();

    assertThat(email.getHeader("Content-Type", null)).isEqualTo("text/plain; charset=UTF-8");

    assertThat(email.getHeader("In-Reply-To", null)).isNull();
    assertThat(email.getHeader("References", null)).isNull();

    assertThat(email.getHeader("List-ID", null)).isEqualTo("SonarQube <sonar.nemo.sonarsource.org>");
    assertThat(email.getHeader("List-Archive", null)).isEqualTo("http://nemo.sonarsource.org");

    assertThat(email.getHeader("From", null)).isEqualTo("SonarQube <server@nowhere>");
    assertThat(email.getHeader("To", null)).isEqualTo("<user@nowhere>");
    assertThat(email.getHeader("Subject", null)).isEqualTo("[SONARQUBE] Foo");
    assertThat((String) email.getContent()).startsWith("Bar");
  }

  @Test
  public void shouldNotThrowAnExceptionWhenUnableToSendEmail() {
    configure();
    server.stop();

    EmailMessage emailMessage = new EmailMessage()
      .setTo("user@nowhere")
      .setSubject("Foo")
      .setMessage("Bar");
    channel.deliver(emailMessage);
  }

  @Test
  public void shouldSendTestEmailWithSTARTTLS() {
    server.getServer().setEnableTLS(true);
    server.getServer().setRequireTLS(true);
    configure();
    when(configuration.getSecureConnection()).thenReturn("STARTTLS");

    try {
      channel.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a test message from SonarQube.");
      fail("An SSL exception was expected a a proof that STARTTLS is enabled");
    } catch (EmailException e) {
      // We don't have a SSL certificate so we are expecting a SSL error
      assertThat(e.getCause().getMessage()).isEqualTo("Could not convert socket to TLS");
    }
  }

  private void configure() {
    when(configuration.getSmtpHost()).thenReturn("localhost");
    when(configuration.getSmtpPort()).thenReturn(port);
    when(configuration.getFrom()).thenReturn("server@nowhere");
    when(configuration.getPrefix()).thenReturn("[SONARQUBE]");
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
  }

}
