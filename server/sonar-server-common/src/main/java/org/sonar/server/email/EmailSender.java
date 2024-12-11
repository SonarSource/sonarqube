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
import java.time.Duration;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.Email;
import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.apache.commons.mail2.jakarta.MultiPartEmail;
import org.sonar.api.platform.Server;
import org.sonar.server.oauth.OAuthMicrosoftRestClient;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_AUTH_METHOD_OAUTH;

public abstract class EmailSender<T extends BasicEmail> {

  private static final Duration SOCKET_TIMEOUT = Duration.of(30, SECONDS);

  protected final EmailSmtpConfiguration emailSmtpConfiguration;
  protected final Server server;
  private final OAuthMicrosoftRestClient oAuthMicrosoftRestClient;

  protected EmailSender(EmailSmtpConfiguration emailSmtpConfiguration, Server server, OAuthMicrosoftRestClient oAuthMicrosoftRestClient) {
    this.emailSmtpConfiguration = emailSmtpConfiguration;
    this.server = server;
    this.oAuthMicrosoftRestClient = oAuthMicrosoftRestClient;
  }

  public void send(T report) {
    // Trick to correctly initialize javax.mail library
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

    try {
      Email email = createEmail(report);
      email.send();
    } catch (MalformedURLException | EmailException e) {
      throw new IllegalStateException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(classloader);
    }
  }

  public HtmlEmail createEmail(T report) throws MalformedURLException, EmailException {
    HtmlEmail email = new HtmlEmail();

    setEmailSettings(email);
    addReportContent(email, report);

    return email;
  }

  public boolean areEmailSettingsSet() {
    return isNotBlank(emailSmtpConfiguration.getSmtpHost());
  }

  protected abstract void addReportContent(HtmlEmail email, T report) throws EmailException, MalformedURLException;

  private void setEmailSettings(MultiPartEmail email) throws EmailException {
    configureSecureConnection(email);
    email.setHostName(emailSmtpConfiguration.getSmtpHost());
    email.setSocketConnectionTimeout(SOCKET_TIMEOUT);
    email.setSocketTimeout(SOCKET_TIMEOUT);
    email.setCharset(UTF_8.name());
    email.setFrom(emailSmtpConfiguration.getFrom(), emailSmtpConfiguration.getFromName());

    if (EMAIL_CONFIG_SMTP_AUTH_METHOD_OAUTH.equals(emailSmtpConfiguration.getAuthMethod())) {
      setOauthAuthentication(email);
    } else if (StringUtils.isNotBlank(emailSmtpConfiguration.getSmtpUsername()) || StringUtils.isNotBlank(emailSmtpConfiguration.getSmtpPassword())) {
      setBasicAuthentication(email);
    }
  }

  private void setOauthAuthentication(Email email) throws EmailException {
    String token = oAuthMicrosoftRestClient.getAccessTokenFromClientCredentialsGrantFlow(emailSmtpConfiguration.getOAuthHost(), emailSmtpConfiguration.getOAuthClientId(),
      emailSmtpConfiguration.getOAuthClientSecret(), emailSmtpConfiguration.getOAuthTenant(), emailSmtpConfiguration.getOAuthScope());
    email.setAuthentication(emailSmtpConfiguration.getSmtpUsername(), token);
    Properties props = email.getMailSession().getProperties();
    props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
    props.put("mail.smtp.auth.login.disable", "true");
    props.put("mail.smtp.auth.plain.disable", "true");
  }

  private void setBasicAuthentication(Email email) {
    if (StringUtils.isNotBlank(emailSmtpConfiguration.getSmtpUsername()) || StringUtils.isNotBlank(emailSmtpConfiguration.getSmtpPassword())) {
      email.setAuthentication(emailSmtpConfiguration.getSmtpUsername(), emailSmtpConfiguration.getSmtpPassword());
    }
  }

  private void configureSecureConnection(MultiPartEmail email) {
    if (StringUtils.equalsIgnoreCase(emailSmtpConfiguration.getSecureConnection(), "SSLTLS")) {
      email.setSSLOnConnect(true);
      email.setSSLCheckServerIdentity(true);
      email.setSslSmtpPort(String.valueOf(emailSmtpConfiguration.getSmtpPort()));

      // this port is not used except in EmailException message, that's why it's set with the same value than SSL port.
      // It prevents from getting bad message.
      email.setSmtpPort(emailSmtpConfiguration.getSmtpPort());
    } else if (StringUtils.equalsIgnoreCase(emailSmtpConfiguration.getSecureConnection(), "STARTTLS")) {
      email.setStartTLSEnabled(true);
      email.setStartTLSRequired(true);
      email.setSSLCheckServerIdentity(true);
      email.setSmtpPort(emailSmtpConfiguration.getSmtpPort());
    } else if (StringUtils.equalsIgnoreCase(emailSmtpConfiguration.getSecureConnection(), "NONE")) {
      email.setSmtpPort(emailSmtpConfiguration.getSmtpPort());
    } else {
      throw new IllegalStateException("Unknown type of SMTP secure connection: " + emailSmtpConfiguration.getSecureConnection());
    }
  }

}
