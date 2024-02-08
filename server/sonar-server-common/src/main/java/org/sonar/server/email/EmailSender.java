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
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.sonar.api.config.EmailSettings;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class EmailSender<T extends BasicEmail> {

  private static final Duration SOCKET_TIMEOUT = Duration.of(30, SECONDS);

  protected final EmailSettings emailSettings;

  protected EmailSender(EmailSettings emailSettings) {
    this.emailSettings = emailSettings;
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
    return isNotBlank(emailSettings.getSmtpHost());
  }

  protected abstract void addReportContent(HtmlEmail email, T report) throws EmailException, MalformedURLException;

  private void setEmailSettings(MultiPartEmail email) throws EmailException {
    configureSecureConnection(email);
    email.setHostName(emailSettings.getSmtpHost());
    email.setSocketConnectionTimeout(SOCKET_TIMEOUT);
    email.setSocketTimeout(SOCKET_TIMEOUT);
    email.setCharset(UTF_8.name());
    email.setFrom(emailSettings.getFrom(), emailSettings.getFromName());

    if (isNotBlank(emailSettings.getSmtpUsername() + emailSettings.getSmtpPassword())) {
      email.setAuthentication(emailSettings.getSmtpUsername(), emailSettings.getSmtpPassword());
    }
  }

  private void configureSecureConnection(MultiPartEmail email) {
    String secureConnection = emailSettings.getSecureConnection();
    int smtpPort = emailSettings.getSmtpPort();
    if (equalsIgnoreCase(secureConnection, "ssl")) {
      email.setSSLOnConnect(true);
      email.setSSLCheckServerIdentity(true);
      email.setSslSmtpPort(String.valueOf(smtpPort));

      // this port is not used except in EmailException message, that's why it's set with the same value than SSL port.
      // It prevents from getting bad message.
      email.setSmtpPort(smtpPort);
    } else if (equalsIgnoreCase(secureConnection, "starttls")) {
      email.setStartTLSEnabled(true);
      email.setStartTLSRequired(true);
      email.setSSLCheckServerIdentity(true);
      email.setSmtpPort(smtpPort);
    } else if (isBlank(secureConnection)) {
      email.setSmtpPort(smtpPort);
    } else {
      throw new IllegalStateException("Unknown type of SMTP secure connection: " + secureConnection);
    }
  }

}
