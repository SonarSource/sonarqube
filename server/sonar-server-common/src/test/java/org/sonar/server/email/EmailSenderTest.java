/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Properties;
import java.util.Set;
import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.apache.commons.mail2.jakarta.MultiPartEmail;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.server.oauth.OAuthMicrosoftRestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_AUTH_METHOD_OAUTH;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT;

public class EmailSenderTest {
  private final EmailSmtpConfiguration emailSmtpConfiguration = mock();
  private final Server server = mock();
  private final OAuthMicrosoftRestClient oAuthMicrosoftRestClient = mock();
  private final EmailSender<BasicEmail> sender = new EmailSender<>(emailSmtpConfiguration, server, oAuthMicrosoftRestClient) {
    @Override
    protected void addReportContent(HtmlEmail email, BasicEmail report) {
      email.setSubject("Email Subject");
    }
  };

  @Test
  public void test_email_fields() throws Exception {
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    when(emailSmtpConfiguration.getSmtpHost()).thenReturn("smtphost");
    when(emailSmtpConfiguration.getSmtpPort()).thenReturn(25);
    when(emailSmtpConfiguration.getSecureConnection()).thenReturn("NONE");
    when(emailSmtpConfiguration.getFrom()).thenReturn("noreply@nowhere");
    when(emailSmtpConfiguration.getFromName()).thenReturn("My SonarQube");
    when(emailSmtpConfiguration.getPrefix()).thenReturn("[SONAR]");
    when(emailSmtpConfiguration.getSmtpUsername()).thenReturn("");
    when(emailSmtpConfiguration.getSmtpPassword()).thenReturn("");

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
    when(emailSmtpConfiguration.getSmtpHost()).thenReturn("smtphost");
    when(emailSmtpConfiguration.getSmtpPort()).thenReturn(465);
    when(emailSmtpConfiguration.getSecureConnection()).thenReturn("NONE");
    when(emailSmtpConfiguration.getFrom()).thenReturn("noreply@nowhere");

    MultiPartEmail email = sender.createEmail(basicEmail);

    assertThat(email.getSubject()).isEqualTo("Email Subject");
  }

  @Test
  public void support_ssl() throws Exception {
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    when(emailSmtpConfiguration.getSecureConnection()).thenReturn("SSLTLS");
    when(emailSmtpConfiguration.getSmtpHost()).thenReturn("smtphost");
    when(emailSmtpConfiguration.getSmtpPort()).thenReturn(466);
    when(emailSmtpConfiguration.getFrom()).thenReturn("noreply@nowhere");
    when(emailSmtpConfiguration.getSmtpUsername()).thenReturn("login");
    when(emailSmtpConfiguration.getSmtpPassword()).thenReturn("pwd");

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
    when(emailSmtpConfiguration.getSecureConnection()).thenReturn("STARTTLS");
    when(emailSmtpConfiguration.getSmtpHost()).thenReturn("smtphost");
    when(emailSmtpConfiguration.getSmtpPort()).thenReturn(587);
    when(emailSmtpConfiguration.getFrom()).thenReturn("noreply@nowhere");
    when(emailSmtpConfiguration.getSmtpUsername()).thenReturn("login");
    when(emailSmtpConfiguration.getSmtpPassword()).thenReturn("pwd");

    MultiPartEmail email = sender.createEmail(basicEmail);

    assertThat(email.isSSLOnConnect()).isFalse();
    assertThat(email.isStartTLSEnabled()).isTrue();
    assertThat(email.isStartTLSRequired()).isTrue();
    assertThat(email.getHostName()).isEqualTo("smtphost");
    assertThat(email.getSmtpPort()).isEqualTo("587");
  }

  @Test
  public void support_oauth() throws Exception {
    BasicEmail basicEmail = new BasicEmail(Set.of("noreply@nowhere"));
    when(emailSmtpConfiguration.getSecureConnection()).thenReturn("STARTTLS");
    when(emailSmtpConfiguration.getSmtpHost()).thenReturn("smtphost");
    when(emailSmtpConfiguration.getSmtpPort()).thenReturn(587);
    when(emailSmtpConfiguration.getFrom()).thenReturn("noreply@nowhere");
    when(emailSmtpConfiguration.getAuthMethod()).thenReturn(EMAIL_CONFIG_SMTP_AUTH_METHOD_OAUTH);
    when(emailSmtpConfiguration.getSmtpUsername()).thenReturn("login");
    when(emailSmtpConfiguration.getSmtpPassword()).thenReturn("pwd");
    when(emailSmtpConfiguration.getOAuthHost()).thenReturn("oauthHost");
    when(emailSmtpConfiguration.getOAuthClientId()).thenReturn("oauthClientId");
    when(emailSmtpConfiguration.getOAuthClientSecret()).thenReturn("oauthClientSecret");
    when(emailSmtpConfiguration.getOAuthTenant()).thenReturn("oauthTenant");
    when(emailSmtpConfiguration.getOAuthScope()).thenReturn(EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT);
    when(oAuthMicrosoftRestClient.getAccessTokenFromClientCredentialsGrantFlow("oauthHost", "oauthClientId", "oauthClientSecret", "oauthTenant",
      EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT)).thenReturn("token");

    MultiPartEmail email = sender.createEmail(basicEmail);

    assertThat(email.isSSLOnConnect()).isFalse();
    assertThat(email.isStartTLSEnabled()).isTrue();
    assertThat(email.isStartTLSRequired()).isTrue();
    assertThat(email.getHostName()).isEqualTo("smtphost");
    assertThat(email.getSmtpPort()).isEqualTo("587");
    Properties emailProperties = email.getMailSession().getProperties();
    assertThat(emailProperties)
      .containsEntry("mail.smtp.auth.mechanisms", "XOAUTH2")
      .containsEntry("mail.smtp.auth.login.disable", "true")
      .containsEntry("mail.smtp.auth.plain.disable", "true");
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
