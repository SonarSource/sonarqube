/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.email;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.commons.mail.EmailException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.plugins.email.api.EmailMessage;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

public class EmailNotificationChannelTest {

  private static int port;

  private SimpleSmtpServer server;

  private EmailConfiguration configuration;
  private EmailNotificationChannel channel;

  @BeforeClass
  public static void selectPort() {
    port = getNextAvailablePort();
  }

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
    server = SimpleSmtpServer.start(port);
    configuration = mock(EmailConfiguration.class);
    channel = new EmailNotificationChannel(configuration, null, null);
  }

  @After
  public void tearDown() {
    if (!server.isStopped()) {
      server.stop();
    }
  }

  @Test
  public void shouldSendTestEmail() throws Exception {
    configure();
    channel.sendTestEmail("user@nowhere", "Test Message from Sonar", "This is a test message from Sonar.");

    assertThat(server.getReceivedEmailSize(), is(1));
    SmtpMessage email = (SmtpMessage) server.getReceivedEmail().next();

    assertThat(email.getHeaderValue("From"), is("Sonar <server@nowhere>"));
    assertThat(email.getHeaderValue("To"), is("<user@nowhere>"));
    assertThat(email.getHeaderValue("Subject"), is("[SONAR] Test Message from Sonar"));
    assertThat(email.getBody(), is("This is a test message from Sonar."));
  }

  @Test(expected = EmailException.class)
  public void shouldThrowAnExceptionWhenUnableToSendTestEmail() throws Exception {
    configure();
    server.stop();

    channel.sendTestEmail("user@nowhere", "Test Message from Sonar", "This is a test message from Sonar.");
  }

  @Test
  public void shouldNotSendEmailWhenHostnameNotConfigured() throws Exception {
    EmailMessage emailMessage = new EmailMessage()
        .setTo("user@nowhere")
        .setSubject("Foo")
        .setMessage("Bar");
    channel.deliver(emailMessage);
    assertThat(server.getReceivedEmailSize(), is(0));
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

    assertThat(server.getReceivedEmailSize(), is(1));
    SmtpMessage email = (SmtpMessage) server.getReceivedEmail().next();

    assertThat(email.getHeaderValue("In-Reply-To"), is("<reviews/view/1@nemo.sonarsource.org>"));
    assertThat(email.getHeaderValue("References"), is("<reviews/view/1@nemo.sonarsource.org>"));

    assertThat(email.getHeaderValue("List-ID"), is("Sonar <sonar.nemo.sonarsource.org>"));
    assertThat(email.getHeaderValue("List-Archive"), is("http://nemo.sonarsource.org"));

    assertThat(email.getHeaderValue("From"), is("Full Username <server@nowhere>"));
    assertThat(email.getHeaderValue("To"), is("<user@nowhere>"));
    assertThat(email.getHeaderValue("Subject"), is("[SONAR] Review #3"));
    assertThat(email.getBody(), is("I'll take care of this violation."));
  }

  @Test
  public void shouldSendNonThreadedEmail() throws Exception {
    configure();
    EmailMessage emailMessage = new EmailMessage()
        .setTo("user@nowhere")
        .setSubject("Foo")
        .setMessage("Bar");
    channel.deliver(emailMessage);

    assertThat(server.getReceivedEmailSize(), is(1));
    SmtpMessage email = (SmtpMessage) server.getReceivedEmail().next();

    assertThat(email.getHeaderValue("In-Reply-To"), nullValue());
    assertThat(email.getHeaderValue("References"), nullValue());

    assertThat(email.getHeaderValue("List-ID"), is("Sonar <sonar.nemo.sonarsource.org>"));
    assertThat(email.getHeaderValue("List-Archive"), is("http://nemo.sonarsource.org"));

    assertThat(email.getHeaderValue("From"), is("Sonar <server@nowhere>"));
    assertThat(email.getHeaderValue("To"), is("<user@nowhere>"));
    assertThat(email.getHeaderValue("Subject"), is("[SONAR] Foo"));
    assertThat(email.getBody(), is("Bar"));
  }

  @Test
  public void shouldNotThrowAnExceptionWhenUnableToSendEmail() throws Exception {
    configure();
    server.stop();

    EmailMessage emailMessage = new EmailMessage()
        .setTo("user@nowhere")
        .setSubject("Foo")
        .setMessage("Bar");
    channel.deliver(emailMessage);
  }

  private void configure() {
    when(configuration.getSmtpHost()).thenReturn("localhost");
    when(configuration.getSmtpPort()).thenReturn(Integer.toString(port));
    when(configuration.getFrom()).thenReturn("server@nowhere");
    when(configuration.getPrefix()).thenReturn("[SONAR]");
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
  }

}
