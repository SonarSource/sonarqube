/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.emailnotifications;

import org.apache.commons.mail.EmailException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.mail.internet.MimeMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmailNotificationChannelTest {

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
    channel.sendTestEmail("user@nowhere", "Test Message from Sonar", "This is a test message from Sonar.");

    List<WiserMessage> messages = server.getMessages();
    assertThat(messages.size(), is(1));

    MimeMessage email = messages.get(0).getMimeMessage();
    assertThat(email.getHeader("Content-Type", null), is("text/plain; charset=UTF-8"));
    assertThat(email.getHeader("From", ","), is("Sonar <server@nowhere>"));
    assertThat(email.getHeader("To", null), is("<user@nowhere>"));
    assertThat(email.getHeader("Subject", null), is("[SONAR] Test Message from Sonar"));
    assertThat((String) email.getContent(), startsWith("This is a test message from Sonar."));
  }

  @Test
  public void shouldThrowAnExceptionWhenUnableToSendTestEmail() throws Exception {
    configure();
    server.stop();

    try {
      channel.sendTestEmail("user@nowhere", "Test Message from Sonar", "This is a test message from Sonar.");
      fail();
    } catch (EmailException e) {
      // expected
    }
  }

  @Test
  public void shouldNotSendEmailWhenHostnameNotConfigured() throws Exception {
    EmailMessage emailMessage = new EmailMessage()
      .setTo("user@nowhere")
      .setSubject("Foo")
      .setMessage("Bar");
    channel.deliver(emailMessage);
    assertThat(server.getMessages().size(), is(0));
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
    assertThat(messages.size(), is(1));

    MimeMessage email = messages.get(0).getMimeMessage();

    assertThat(email.getHeader("Content-Type", null), is("text/plain; charset=UTF-8"));

    assertThat(email.getHeader("In-Reply-To", null), is("<reviews/view/1@nemo.sonarsource.org>"));
    assertThat(email.getHeader("References", null), is("<reviews/view/1@nemo.sonarsource.org>"));

    assertThat(email.getHeader("List-ID", null), is("Sonar <sonar.nemo.sonarsource.org>"));
    assertThat(email.getHeader("List-Archive", null), is("http://nemo.sonarsource.org"));

    assertThat(email.getHeader("From", ","), is("\"Full Username (Sonar)\" <server@nowhere>"));
    assertThat(email.getHeader("To", null), is("<user@nowhere>"));
    assertThat(email.getHeader("Subject", null), is("[SONAR] Review #3"));
    assertThat((String) email.getContent(), startsWith("I'll take care of this violation."));
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
    assertThat(messages.size(), is(1));

    MimeMessage email = messages.get(0).getMimeMessage();

    assertThat(email.getHeader("Content-Type", null), is("text/plain; charset=UTF-8"));

    assertThat(email.getHeader("In-Reply-To", null), nullValue());
    assertThat(email.getHeader("References", null), nullValue());

    assertThat(email.getHeader("List-ID", null), is("Sonar <sonar.nemo.sonarsource.org>"));
    assertThat(email.getHeader("List-Archive", null), is("http://nemo.sonarsource.org"));

    assertThat(email.getHeader("From", null), is("Sonar <server@nowhere>"));
    assertThat(email.getHeader("To", null), is("<user@nowhere>"));
    assertThat(email.getHeader("Subject", null), is("[SONAR] Foo"));
    assertThat((String) email.getContent(), startsWith("Bar"));
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
    when(configuration.getSmtpPort()).thenReturn(port);
    when(configuration.getFrom()).thenReturn("server@nowhere");
    when(configuration.getPrefix()).thenReturn("[SONAR]");
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
  }

}
