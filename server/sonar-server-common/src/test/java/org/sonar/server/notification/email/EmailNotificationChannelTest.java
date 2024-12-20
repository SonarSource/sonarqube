/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.notification.email;

import com.icegreen.greenmail.junit4.GreenMailRule;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.mail2.core.EmailException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.server.email.EmailSmtpConfiguration;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;
import org.sonar.server.oauth.OAuthMicrosoftRestClient;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static junit.framework.Assert.fail;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class EmailNotificationChannelTest {

  private static final String SUBJECT_PREFIX = "[SONARQUBE]";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public final GreenMailRule smtpServer = new GreenMailRule(new ServerSetup[] {ServerSetupTest.SMTP, ServerSetupTest.SMTPS});

  private EmailSmtpConfiguration configuration;
  private Server server;
  private EmailNotificationChannel underTest;

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);

    configuration = mock(EmailSmtpConfiguration.class);
    server = mock(Server.class);

    underTest = new EmailNotificationChannel(configuration, server, null, null, mock(OAuthMicrosoftRestClient.class));
  }

  @After
  public void tearDown() {
    smtpServer.stop();
  }

  @Test
  public void isActivated_returns_true_if_smpt_host_is_not_empty() {
    when(configuration.getSmtpHost()).thenReturn(secure().next(5));

    assertThat(underTest.isActivated()).isTrue();
  }

  @Test
  public void isActivated_returns_false_if_smpt_host_is_null() {
    when(configuration.getSmtpHost()).thenReturn(null);

    assertThat(underTest.isActivated()).isFalse();
  }

  @Test
  public void isActivated_returns_false_if_smpt_host_is_empty() {
    when(configuration.getSmtpHost()).thenReturn("");

    assertThat(underTest.isActivated()).isFalse();
  }

  @Test
  public void shouldSendTestEmail() throws Exception {
    configure();
    underTest.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a test message from SonarQube.");

    MimeMessage[] messages = smtpServer.getReceivedMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages[0];
    assertThat(email.getHeader("Content-Type", null)).isEqualTo("text/plain; charset=UTF-8");
    assertThat(email.getHeader("From", ",")).isEqualTo("SonarQube from NoWhere <server@nowhere>");
    assertThat(email.getHeader("To", null)).isEqualTo("<user@nowhere>");
    assertThat(email.getHeader("Subject", null)).isEqualTo("[SONARQUBE] Test Message from SonarQube");
    assertThat((String) email.getContent()).startsWith("This is a test message from SonarQube.\r\n\r\nMail sent from: http://nemo.sonarsource.org");
  }

  @Test
  public void sendTestEmailShouldSanitizeLog() throws Exception {
    logTester.setLevel(Level.TRACE);
    configure();
    underTest.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a message \n containing line breaks \r that should be sanitized when logged.");

    assertThat(logTester.logs(Level.TRACE)).isNotEmpty()
      .contains("Sending email: This is a message _ containing line breaks _ that should be sanitized when logged.__Mail sent from: http://nemo.sonarsource.org");

  }

  @Test
  public void shouldThrowAnExceptionWhenUnableToSendTestEmail() {
    configure();
    smtpServer.stop();

    try {
      underTest.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a test message from SonarQube.");
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
      .setPlainTextMessage("Bar");
    boolean delivered = underTest.deliver(emailMessage);
    assertThat(smtpServer.getReceivedMessages()).isEmpty();
    assertThat(delivered).isFalse();
  }

  @Test
  public void shouldSendThreadedEmail() throws Exception {
    configure();
    EmailMessage emailMessage = new EmailMessage()
      .setMessageId("reviews/view/1")
      .setFrom("Full Username")
      .setTo("user@nowhere")
      .setSubject("Review #3")
      .setPlainTextMessage("I'll take care of this violation.");
    boolean delivered = underTest.deliver(emailMessage);

    MimeMessage[] messages = smtpServer.getReceivedMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages[0];

    assertThat(email.getHeader("Content-Type", null)).isEqualTo("text/plain; charset=UTF-8");

    assertThat(email.getHeader("In-Reply-To", null)).isEqualTo("<reviews/view/1@nemo.sonarsource.org>");
    assertThat(email.getHeader("References", null)).isEqualTo("<reviews/view/1@nemo.sonarsource.org>");

    assertThat(email.getHeader("List-ID", null)).isEqualTo("SonarQube <sonar.nemo.sonarsource.org>");
    assertThat(email.getHeader("List-Archive", null)).isEqualTo("http://nemo.sonarsource.org");

    assertThat(email.getHeader("From", ",")).isEqualTo("\"Full Username (SonarQube from NoWhere)\" <server@nowhere>");
    assertThat(email.getHeader("To", null)).isEqualTo("<user@nowhere>");
    assertThat(email.getHeader("Subject", null)).isEqualTo("[SONARQUBE] Review #3");
    assertThat((String) email.getContent()).startsWith("I'll take care of this violation.");
    assertThat(delivered).isTrue();
  }

  @Test
  public void shouldSendNonThreadedEmail() throws Exception {
    configure();
    EmailMessage emailMessage = new EmailMessage()
      .setTo("user@nowhere")
      .setSubject("Foo")
      .setPlainTextMessage("Bar");
    boolean delivered = underTest.deliver(emailMessage);

    MimeMessage[] messages = smtpServer.getReceivedMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages[0];

    assertThat(email.getHeader("Content-Type", null)).isEqualTo("text/plain; charset=UTF-8");

    assertThat(email.getHeader("In-Reply-To", null)).isNull();
    assertThat(email.getHeader("References", null)).isNull();

    assertThat(email.getHeader("List-ID", null)).isEqualTo("SonarQube <sonar.nemo.sonarsource.org>");
    assertThat(email.getHeader("List-Archive", null)).isEqualTo("http://nemo.sonarsource.org");

    assertThat(email.getHeader("From", null)).isEqualTo("SonarQube from NoWhere <server@nowhere>");
    assertThat(email.getHeader("To", null)).isEqualTo("<user@nowhere>");
    assertThat(email.getHeader("Subject", null)).isEqualTo("[SONARQUBE] Foo");
    assertThat((String) email.getContent()).startsWith("Bar");
    assertThat(delivered).isTrue();
  }

  @Test
  public void shouldNotThrowAnExceptionWhenUnableToSendEmail() {
    configure();
    smtpServer.stop();

    EmailMessage emailMessage = new EmailMessage()
      .setTo("user@nowhere")
      .setSubject("Foo")
      .setPlainTextMessage("Bar");
    boolean delivered = underTest.deliver(emailMessage);

    assertThat(delivered).isFalse();
  }

  @Test
  public void shouldSendTestEmailWithSTARTTLS() {
    configure(true);

    try {
      underTest.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a test message from SonarQube.");
      fail("An SSL exception was expected a a proof that STARTTLS is enabled");
    } catch (EmailException e) {
      // We don't have a SSL certificate so we are expecting a SSL error
      assertThat(e.getCause().getMessage()).contains("Exception reading response");
    }
  }

  @Test
  public void deliverAll_has_no_effect_if_set_is_empty() {
    EmailSmtpConfiguration emailSettings = mock(EmailSmtpConfiguration.class);
    EmailNotificationChannel emailNotificationChannel = new EmailNotificationChannel(emailSettings, server, null, null, mock(OAuthMicrosoftRestClient.class));

    int count = emailNotificationChannel.deliverAll(Collections.emptySet());

    assertThat(count).isZero();
    verifyNoInteractions(emailSettings);
    assertThat(smtpServer.getReceivedMessages()).isEmpty();
  }

  @Test
  public void deliverAll_has_no_effect_if_smtp_host_is_null() {
    EmailSmtpConfiguration emailSettings = mock(EmailSmtpConfiguration.class);
    when(emailSettings.getSmtpHost()).thenReturn(null);
    Set<EmailDeliveryRequest> requests = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> new EmailDeliveryRequest("foo" + i + "@moo", mock(Notification.class)))
      .collect(toSet());
    EmailNotificationChannel emailNotificationChannel = new EmailNotificationChannel(emailSettings, server, null, null, mock(OAuthMicrosoftRestClient.class));

    int count = emailNotificationChannel.deliverAll(requests);

    assertThat(count).isZero();
    verify(emailSettings).getSmtpHost();
    verifyNoMoreInteractions(emailSettings);
    assertThat(smtpServer.getReceivedMessages()).isEmpty();
  }

  @Test
  @UseDataProvider("emptyStrings")
  public void deliverAll_ignores_requests_which_recipient_is_empty(String emptyString) {
    EmailSmtpConfiguration emailSettings = mock(EmailSmtpConfiguration.class);
    when(emailSettings.getSmtpHost()).thenReturn(null);
    Set<EmailDeliveryRequest> requests = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> new EmailDeliveryRequest(emptyString, mock(Notification.class)))
      .collect(toSet());
    EmailNotificationChannel emailNotificationChannel = new EmailNotificationChannel(emailSettings, server, null, null, mock(OAuthMicrosoftRestClient.class));

    int count = emailNotificationChannel.deliverAll(requests);

    assertThat(count).isZero();
    verify(emailSettings).getSmtpHost();
    verifyNoMoreInteractions(emailSettings);
    assertThat(smtpServer.getReceivedMessages()).isEmpty();
  }

  @Test
  public void deliverAll_returns_count_of_request_for_which_at_least_one_formatter_accept_it() throws MessagingException, IOException {
    String recipientEmail = "foo@donut";
    configure();
    Notification notification1 = mock(Notification.class);
    Notification notification2 = mock(Notification.class);
    Notification notification3 = mock(Notification.class);
    EmailTemplate template1 = mock(EmailTemplate.class);
    EmailTemplate template3 = mock(EmailTemplate.class);
    EmailMessage emailMessage1 = new EmailMessage().setTo(recipientEmail).setSubject("sub11").setPlainTextMessage("msg11");
    EmailMessage emailMessage3 = new EmailMessage().setTo(recipientEmail).setSubject("sub3").setPlainTextMessage("msg3");
    when(template1.format(notification1)).thenReturn(emailMessage1);
    when(template3.format(notification3)).thenReturn(emailMessage3);
    Set<EmailDeliveryRequest> requests = Stream.of(notification1, notification2, notification3)
      .map(t -> new EmailDeliveryRequest(recipientEmail, t))
      .collect(toSet());
    EmailNotificationChannel emailNotificationChannel = new EmailNotificationChannel(configuration, server, new EmailTemplate[] {template1, template3}, null,
      mock(OAuthMicrosoftRestClient.class));

    int count = emailNotificationChannel.deliverAll(requests);

    assertThat(count).isEqualTo(2);
    assertThat(smtpServer.getReceivedMessages()).hasSize(2);
    Map<String, MimeMessage> messagesBySubject = Stream.of(smtpServer.getReceivedMessages())
      .collect(toMap(t -> {
        try {
          return t.getSubject();
        } catch (MessagingException e) {
          throw new RuntimeException(e);
        }
      }, t -> t));

    assertThat((String) messagesBySubject.get(SUBJECT_PREFIX + " " + emailMessage1.getSubject()).getContent())
      .contains(emailMessage1.getMessage());
    assertThat((String) messagesBySubject.get(SUBJECT_PREFIX + " " + emailMessage3.getSubject()).getContent())
      .contains(emailMessage3.getMessage());
  }

  @Test
  public void deliverAll_ignores_multiple_templates_by_notification_and_takes_the_first_one_only() throws MessagingException, IOException {
    String recipientEmail = "foo@donut";
    configure();
    Notification notification1 = mock(Notification.class);
    EmailTemplate template11 = mock(EmailTemplate.class);
    EmailTemplate template12 = mock(EmailTemplate.class);
    EmailMessage emailMessage11 = new EmailMessage().setTo(recipientEmail).setSubject("sub11").setPlainTextMessage("msg11");
    EmailMessage emailMessage12 = new EmailMessage().setTo(recipientEmail).setSubject("sub12").setPlainTextMessage("msg12");
    when(template11.format(notification1)).thenReturn(emailMessage11);
    when(template12.format(notification1)).thenReturn(emailMessage12);
    EmailDeliveryRequest request = new EmailDeliveryRequest(recipientEmail, notification1);
    EmailNotificationChannel emailNotificationChannel = new EmailNotificationChannel(configuration, server, new EmailTemplate[] {template11, template12}, null,
      mock(OAuthMicrosoftRestClient.class));

    int count = emailNotificationChannel.deliverAll(Collections.singleton(request));

    assertThat(count).isOne();
    assertThat(smtpServer.getReceivedMessages()).hasSize(1);
    assertThat((String) smtpServer.getReceivedMessages()[0].getContent())
      .contains(emailMessage11.getMessage());
  }

  @DataProvider
  public static Object[][] emptyStrings() {
    return new Object[][] {
      {""},
      {"  "},
      {" \n "}
    };
  }

  private void configure() {
    configure(false);
  }

  private void configure(boolean isSecure) {
    SmtpServer localSmtpServer = isSecure ? smtpServer.getSmtps() : smtpServer.getSmtp();
    when(configuration.getSmtpHost()).thenReturn(localSmtpServer.getBindTo());
    when(configuration.getSmtpPort()).thenReturn(localSmtpServer.getPort());
    when(configuration.getSecureConnection()).thenReturn(isSecure ? "STARTTLS" : "NONE");
    when(configuration.getFrom()).thenReturn("server@nowhere");
    when(configuration.getFromName()).thenReturn("SonarQube from NoWhere");
    when(configuration.getPrefix()).thenReturn(SUBJECT_PREFIX);
    when(server.getPublicRootUrl()).thenReturn("http://nemo.sonarsource.org");
  }

}
