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
package org.sonar.server.email;

import java.net.MalformedURLException;
import java.util.Set;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmailSenderTest {
  private EmailSettings emailSettings = mock(EmailSettings.class);
  private EmailSender<BasicEmail> sender = new EmailSender<>(emailSettings) {
    @Override protected void addReportContent(HtmlEmail email, BasicEmail report) throws EmailException, MalformedURLException {
      email.setSubject("Email Subject");
    }
  };

  @Test
  public void test_email_fields() throws Exception {
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    when(emailSettings.getSmtpHost()).thenReturn("smtphost");
    when(emailSettings.getSmtpPort()).thenReturn(25);
    when(emailSettings.getFrom()).thenReturn("noreply@nowhere");
    when(emailSettings.getFromName()).thenReturn("My SonarQube");
    when(emailSettings.getPrefix()).thenReturn("[SONAR]");
    when(emailSettings.getSmtpUsername()).thenReturn("");
    when(emailSettings.getSmtpPassword()).thenReturn("");

    MultiPartEmail email = sender.createEmail(basicEmail);

    assertThat(email.getHostName()).isEqualTo("smtphost");
    assertThat(email.getSmtpPort()).isEqualTo("25");
    assertThat(email.getSubject()).isEqualTo("Email Subject");
    assertThat(email.getFromAddress()).hasToString("My SonarQube <noreply@nowhere>");
    assertThat(email.getToAddresses()).isEmpty();
    assertThat(email.getCcAddresses()).isEmpty();

    assertThat(email.isSSLOnConnect()).isFalse();
    assertThat(email.isStartTLSEnabled()).isFalse();
    assertThat(email.isStartTLSRequired()).isFalse();
  }


  @Test
  public void support_empty_body() throws Exception {
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    when(emailSettings.getSmtpHost()).thenReturn("smtphost");
    when(emailSettings.getSmtpPort()).thenReturn(465);
    when(emailSettings.getFrom()).thenReturn("noreply@nowhere");

    MultiPartEmail email = sender.createEmail(basicEmail);

    assertThat(email.getSubject()).isEqualTo("Email Subject");
  }

  @Test
  public void support_ssl() throws Exception {
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    when(emailSettings.getSecureConnection()).thenReturn("SSL");
    when(emailSettings.getSmtpHost()).thenReturn("smtphost");
    when(emailSettings.getSmtpPort()).thenReturn(466);
    when(emailSettings.getFrom()).thenReturn("noreply@nowhere");
    when(emailSettings.getSmtpUsername()).thenReturn("login");
    when(emailSettings.getSmtpPassword()).thenReturn("pwd");

    MultiPartEmail email = sender.createEmail(basicEmail);

    assertThat(email.isSSLOnConnect()).isTrue();
    assertThat(email.isStartTLSEnabled()).isFalse();
    assertThat(email.isStartTLSRequired()).isFalse();
    assertThat(email.getHostName()).isEqualTo("smtphost");
    assertThat(email.getSmtpPort()).isEqualTo("466");
    assertThat(email.getSslSmtpPort()).isEqualTo("466");
  }

  @Test
  public void support_starttls() throws Exception {
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    when(emailSettings.getSecureConnection()).thenReturn("STARTTLS");
    when(emailSettings.getSmtpHost()).thenReturn("smtphost");
    when(emailSettings.getSmtpPort()).thenReturn(587);
    when(emailSettings.getFrom()).thenReturn("noreply@nowhere");
    when(emailSettings.getSmtpUsername()).thenReturn("login");
    when(emailSettings.getSmtpPassword()).thenReturn("pwd");

    MultiPartEmail email = sender.createEmail(basicEmail);

    assertThat(email.isSSLOnConnect()).isFalse();
    assertThat(email.isStartTLSEnabled()).isTrue();
    assertThat(email.isStartTLSRequired()).isTrue();
    assertThat(email.getHostName()).isEqualTo("smtphost");
    assertThat(email.getSmtpPort()).isEqualTo("587");
  }

  @Test
  public void send_email() throws Exception {
    HtmlEmail email = mock(HtmlEmail.class);
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    EmailSender<BasicEmail> senderSpy = spy(sender);
    doReturn(email).when(senderSpy).createEmail(basicEmail);

    senderSpy.send(basicEmail);

    verify(email).send();
  }

}
