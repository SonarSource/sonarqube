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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.user.User;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;

import static java.util.Objects.requireNonNull;

/**
 * References:
 * <ul>
 * <li><a href="http://tools.ietf.org/html/rfc4021">Registration of Mail and MIME Header Fields</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc2919">List-Id: A Structured Field and Namespace for the Identification of Mailing Lists</a></li>
 * <li><a href="https://github.com/blog/798-threaded-email-notifications">GitHub: Threaded Email Notifications</a></li>
 * </ul>
 *
 * @since 2.10
 */
public class EmailNotificationChannel extends NotificationChannel {

  private static final Logger LOG = Loggers.get(EmailNotificationChannel.class);

  /**
   * @see org.apache.commons.mail.Email#setSocketConnectionTimeout(int)
   * @see org.apache.commons.mail.Email#setSocketTimeout(int)
   */
  private static final int SOCKET_TIMEOUT = 30_000;

  /**
   * Email Header Field: "List-ID".
   * Value of this field should contain mailing list identifier as specified in <a href="http://tools.ietf.org/html/rfc2919">RFC 2919</a>.
   */
  private static final String LIST_ID_HEADER = "List-ID";

  /**
   * Email Header Field: "List-Archive".
   * Value of this field should contain URL of mailing list archive as specified in <a href="http://tools.ietf.org/html/rfc2369">RFC 2369</a>.
   */
  private static final String LIST_ARCHIVE_HEADER = "List-Archive";

  /**
   * Email Header Field: "In-Reply-To".
   * Value of this field should contain related message identifier as specified in <a href="http://tools.ietf.org/html/rfc2822">RFC 2822</a>.
   */
  private static final String IN_REPLY_TO_HEADER = "In-Reply-To";

  /**
   * Email Header Field: "References".
   * Value of this field should contain related message identifier as specified in <a href="http://tools.ietf.org/html/rfc2822">RFC 2822</a>
   */
  private static final String REFERENCES_HEADER = "References";

  private static final String SUBJECT_DEFAULT = "Notification";
  private static final String SMTP_HOST_NOT_CONFIGURED_DEBUG_MSG = "SMTP host was not configured - email will not be sent";

  private final EmailSettings configuration;
  private final EmailTemplate[] templates;
  private final DbClient dbClient;

  public EmailNotificationChannel(EmailSettings configuration, EmailTemplate[] templates, DbClient dbClient) {
    this.configuration = configuration;
    this.templates = templates;
    this.dbClient = dbClient;
  }

  public boolean isActivated() {
    return !StringUtils.isBlank(configuration.getSmtpHost());
  }

  @Override
  public boolean deliver(Notification notification, String username) {
    if (!isActivated()) {
      LOG.debug(SMTP_HOST_NOT_CONFIGURED_DEBUG_MSG);
      return false;
    }

    User user = findByLogin(username);
    if (user == null || StringUtils.isBlank(user.email())) {
      LOG.debug("User does not exist or has no email: {}", username);
      return false;
    }

    EmailMessage emailMessage = format(notification);
    if (emailMessage != null) {
      emailMessage.setTo(user.email());
      return deliver(emailMessage);
    }
    return false;
  }

  @Immutable
  public static final class EmailDeliveryRequest {
    private final String recipientEmail;
    private final Notification notification;

    public EmailDeliveryRequest(String recipientEmail, Notification notification) {
      this.recipientEmail = requireNonNull(recipientEmail, "recipientEmail can't be null");
      this.notification = requireNonNull(notification, "notification can't be null");
    }

    public String getRecipientEmail() {
      return recipientEmail;
    }

    public Notification getNotification() {
      return notification;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EmailDeliveryRequest that = (EmailDeliveryRequest) o;
      return Objects.equals(recipientEmail, that.recipientEmail) &&
        Objects.equals(notification, that.notification);
    }

    @Override
    public int hashCode() {
      return Objects.hash(recipientEmail, notification);
    }

    @Override
    public String toString() {
      return "EmailDeliveryRequest{" + "'" + recipientEmail + '\'' + " : " + notification + '}';
    }
  }

  public int deliverAll(Set<EmailDeliveryRequest> deliveries) {
    if (deliveries.isEmpty() || !isActivated()) {
      LOG.debug(SMTP_HOST_NOT_CONFIGURED_DEBUG_MSG);
      return 0;
    }

    return (int) deliveries.stream()
      .filter(t -> !t.getRecipientEmail().trim().isEmpty())
      .map(t -> {
        EmailMessage emailMessage = format(t.getNotification());
        if (emailMessage != null) {
          emailMessage.setTo(t.getRecipientEmail());
          return deliver(emailMessage);
        }
        return false;
      })
      .filter(Boolean::booleanValue)
      .count();
  }

  @CheckForNull
  private User findByLogin(String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto dto = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
      return dto != null ? dto.toUser() : null;
    }
  }

  private EmailMessage format(Notification notification) {
    for (EmailTemplate template : templates) {
      EmailMessage email = template.format(notification);
      if (email != null) {
        return email;
      }
    }
    LOG.warn("Email template not found for notification: {}", notification);
    return null;
  }

  boolean deliver(EmailMessage emailMessage) {
    if (!isActivated()) {
      LOG.debug(SMTP_HOST_NOT_CONFIGURED_DEBUG_MSG);
      return false;
    }
    try {
      send(emailMessage);
      return true;
    } catch (EmailException e) {
      LOG.error("Unable to send email", e);
      return false;
    }
  }

  private void send(EmailMessage emailMessage) throws EmailException {
    // Trick to correctly initialize javax.mail library
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

    try {
      LOG.trace("Sending email: {}", emailMessage);
      String host = resolveHost();

      Email email = createEmailWithMessage(emailMessage);
      setHeaders(email, emailMessage, host);
      setConnectionDetails(email);
      setToAndFrom(email, emailMessage);
      setSubject(email, emailMessage);
      email.send();

    } finally {
      Thread.currentThread().setContextClassLoader(classloader);
    }
  }

  private static Email createEmailWithMessage(EmailMessage emailMessage) throws EmailException {
    if (emailMessage.isHtml()) {
      return new HtmlEmail().setHtmlMsg(emailMessage.getMessage());
    }
    return new SimpleEmail().setMsg(emailMessage.getMessage());
  }

  private void setSubject(Email email, EmailMessage emailMessage) {
    String subject = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(configuration.getPrefix()) + " ", "")
      + StringUtils.defaultString(emailMessage.getSubject(), SUBJECT_DEFAULT);
    email.setSubject(subject);
  }

  private void setToAndFrom(Email email, EmailMessage emailMessage) throws EmailException {
    String fromName = configuration.getFromName();
    String from = StringUtils.isBlank(emailMessage.getFrom()) ? fromName : (emailMessage.getFrom() + " (" + fromName + ")");
    email.setFrom(configuration.getFrom(), from);
    email.addTo(emailMessage.getTo(), " ");
  }

  @CheckForNull
  private String resolveHost() {
    try {
      return new URL(configuration.getServerBaseURL()).getHost();
    } catch (MalformedURLException e) {
      // ignore
      return null;
    }
  }

  private void setHeaders(Email email, EmailMessage emailMessage, @CheckForNull String host) {
    // Set general information
    email.setCharset("UTF-8");
    if (StringUtils.isNotBlank(host)) {
      /*
       * Set headers for proper threading: GMail will not group messages, even if they have same subject, but don't have "In-Reply-To" and
       * "References" headers. TODO investigate threading in other clients like KMail, Thunderbird, Outlook
       */
      if (StringUtils.isNotEmpty(emailMessage.getMessageId())) {
        String messageId = "<" + emailMessage.getMessageId() + "@" + host + ">";
        email.addHeader(IN_REPLY_TO_HEADER, messageId);
        email.addHeader(REFERENCES_HEADER, messageId);
      }
      // Set headers for proper filtering
      email.addHeader(LIST_ID_HEADER, "SonarQube <sonar." + host + ">");
      email.addHeader(LIST_ARCHIVE_HEADER, configuration.getServerBaseURL());
    }
  }

  private void setConnectionDetails(Email email) {
    email.setHostName(configuration.getSmtpHost());
    configureSecureConnection(email);
    if (StringUtils.isNotBlank(configuration.getSmtpUsername()) || StringUtils.isNotBlank(configuration.getSmtpPassword())) {
      email.setAuthentication(configuration.getSmtpUsername(), configuration.getSmtpPassword());
    }
    email.setSocketConnectionTimeout(SOCKET_TIMEOUT);
    email.setSocketTimeout(SOCKET_TIMEOUT);
  }

  private void configureSecureConnection(Email email) {
    if (StringUtils.equalsIgnoreCase(configuration.getSecureConnection(), "ssl")) {
      email.setSSLOnConnect(true);
      email.setSSLCheckServerIdentity(true);
      email.setSslSmtpPort(String.valueOf(configuration.getSmtpPort()));

      // this port is not used except in EmailException message, that's why it's set with the same value than SSL port.
      // It prevents from getting bad message.
      email.setSmtpPort(configuration.getSmtpPort());
    } else if (StringUtils.equalsIgnoreCase(configuration.getSecureConnection(), "starttls")) {
      email.setStartTLSEnabled(true);
      email.setStartTLSRequired(true);
      email.setSSLCheckServerIdentity(true);
      email.setSmtpPort(configuration.getSmtpPort());
    } else if (StringUtils.isBlank(configuration.getSecureConnection())) {
      email.setSmtpPort(configuration.getSmtpPort());
    } else {
      throw new SonarException("Unknown type of SMTP secure connection: " + configuration.getSecureConnection());
    }
  }

  /**
   * Send test email.
   *
   * @throws EmailException when unable to send
   */
  public void sendTestEmail(String toAddress, String subject, String message) throws EmailException {
    try {
      EmailMessage emailMessage = new EmailMessage();
      emailMessage.setTo(toAddress);
      emailMessage.setSubject(subject);
      emailMessage.setPlainTextMessage(message);
      send(emailMessage);
    } catch (EmailException e) {
      LOG.debug("Fail to send test email to {}: {}", toAddress, e);
      throw e;
    }
  }

}
