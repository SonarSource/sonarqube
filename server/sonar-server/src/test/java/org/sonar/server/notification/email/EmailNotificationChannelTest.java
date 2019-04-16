/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.mail.EmailException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static junit.framework.Assert.fail;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class EmailNotificationChannelTest {

  private static final String SUBJECT_PREFIX = "[SONARQUBE]";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Wiser smtpServer;
  private EmailSettings configuration;
  private EmailNotificationChannel underTest;

  @Before
  public void setUp() {
    smtpServer = new Wiser(0);
    smtpServer.start();

    configuration = mock(EmailSettings.class);
    underTest = new EmailNotificationChannel(configuration, null, null);
  }

  @After
  public void tearDown() {
    smtpServer.stop();
  }

  @Test
  public void isActivated_returns_true_if_smpt_host_is_not_empty() {
    when(configuration.getSmtpHost()).thenReturn(random(5));

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

    List<WiserMessage> messages = smtpServer.getMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages.get(0).getMimeMessage();
    assertThat(email.getHeader("Content-Type", null)).isEqualTo("text/plain; charset=UTF-8");
    assertThat(email.getHeader("From", ",")).isEqualTo("SonarQube from NoWhere <server@nowhere>");
    assertThat(email.getHeader("To", null)).isEqualTo("<user@nowhere>");
    assertThat(email.getHeader("Subject", null)).isEqualTo("[SONARQUBE] Test Message from SonarQube");
    assertThat((String) email.getContent()).startsWith("This is a test message from SonarQube.");
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
    assertThat(smtpServer.getMessages()).isEmpty();
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

    List<WiserMessage> messages = smtpServer.getMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages.get(0).getMimeMessage();

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

    List<WiserMessage> messages = smtpServer.getMessages();
    assertThat(messages).hasSize(1);

    MimeMessage email = messages.get(0).getMimeMessage();

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
    smtpServer.getServer().setEnableTLS(true);
    smtpServer.getServer().setRequireTLS(true);
    configure();
    when(configuration.getSecureConnection()).thenReturn("STARTTLS");

    try {
      underTest.sendTestEmail("user@nowhere", "Test Message from SonarQube", "This is a test message from SonarQube.");
      fail("An SSL exception was expected a a proof that STARTTLS is enabled");
    } catch (EmailException e) {
      // We don't have a SSL certificate so we are expecting a SSL error
      assertThat(e.getCause().getMessage()).isEqualTo("Could not convert socket to TLS");
    }
  }

  @Test
  public void deliverAll_has_no_effect_if_set_is_empty() {
    EmailSettings emailSettings = mock(EmailSettings.class);
    EmailNotificationChannel underTest = new EmailNotificationChannel(emailSettings, null, null);

    int count = underTest.deliverAll(Collections.emptySet());

    assertThat(count).isZero();
    verifyZeroInteractions(emailSettings);
    assertThat(smtpServer.getMessages()).isEmpty();
  }

  @Test
  public void deliverAll_has_no_effect_if_smtp_host_is_null() {
    EmailSettings emailSettings = mock(EmailSettings.class);
    when(emailSettings.getSmtpHost()).thenReturn(null);
    Set<EmailDeliveryRequest> requests = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> new EmailDeliveryRequest("foo" + i + "@moo", mock(Notification.class)))
      .collect(toSet());
    EmailNotificationChannel underTest = new EmailNotificationChannel(emailSettings, null, null);

    int count = underTest.deliverAll(requests);

    assertThat(count).isZero();
    verify(emailSettings).getSmtpHost();
    verifyNoMoreInteractions(emailSettings);
    assertThat(smtpServer.getMessages()).isEmpty();
  }

  @Test
  @UseDataProvider("emptyStrings")
  public void deliverAll_ignores_requests_which_recipient_is_empty(String emptyString) {
    EmailSettings emailSettings = mock(EmailSettings.class);
    when(emailSettings.getSmtpHost()).thenReturn(null);
    Set<EmailDeliveryRequest> requests = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> new EmailDeliveryRequest(emptyString, mock(Notification.class)))
      .collect(toSet());
    EmailNotificationChannel underTest = new EmailNotificationChannel(emailSettings, null, null);

    int count = underTest.deliverAll(requests);

    assertThat(count).isZero();
    verify(emailSettings).getSmtpHost();
    verifyNoMoreInteractions(emailSettings);
    assertThat(smtpServer.getMessages()).isEmpty();
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
    EmailNotificationChannel underTest = new EmailNotificationChannel(configuration, new EmailTemplate[] {template1, template3}, null);

    int count = underTest.deliverAll(requests);

    assertThat(count).isEqualTo(2);
    assertThat(smtpServer.getMessages()).hasSize(2);
    Map<String, MimeMessage> messagesBySubject = smtpServer.getMessages().stream()
      .map(t -> {
        try {
          return t.getMimeMessage();
        } catch (MessagingException e) {
          throw new RuntimeException(e);
        }
      })
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
    EmailNotificationChannel underTest = new EmailNotificationChannel(configuration, new EmailTemplate[] {template11, template12}, null);

    int count = underTest.deliverAll(Collections.singleton(request));

    assertThat(count).isEqualTo(1);
    assertThat(smtpServer.getMessages()).hasSize(1);
    assertThat((String) smtpServer.getMessages().iterator().next().getMimeMessage().getContent())
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
    when(configuration.getSmtpHost()).thenReturn("localhost");
    when(configuration.getSmtpPort()).thenReturn(smtpServer.getServer().getPort());
    when(configuration.getFrom()).thenReturn("server@nowhere");
    when(configuration.getFromName()).thenReturn("SonarQube from NoWhere");
    when(configuration.getPrefix()).thenReturn(SUBJECT_PREFIX);
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
  }

}
