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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.Notification;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.plugins.email.api.EmailMessage;
import org.sonar.plugins.email.api.EmailTemplate;

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

  private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationChannel.class);

  /**
   * @see org.apache.commons.mail.Email#setSocketConnectionTimeout(int)
   * @see org.apache.commons.mail.Email#setSocketTimeout(int)
   */
  private static final int SOCKET_TIMEOUT = 30000;

  private static final String FROM_NAME_DEFAULT = "Sonar";
  private static final String SUBJECT_DEFAULT = "Notification";

  private EmailConfiguration configuration;
  private EmailTemplate[] templates;
  private DatabaseSessionFactory sessionFactory;

  public EmailNotificationChannel(EmailConfiguration configuration, EmailTemplate[] templates, DatabaseSessionFactory sessionFactory) {
    this.configuration = configuration;
    this.templates = templates;
    this.sessionFactory = sessionFactory;
  }

  private User getUserByLogin(String login) {
    DatabaseSession session = sessionFactory.getSession();
    return session.getSingleResult(User.class, "login", login);
  }

  @Override
  public void deliver(Notification notification, String username) {
    EmailMessage emailMessage = format(notification, username);
    if (emailMessage != null) {
      deliver(emailMessage);
    }
  }

  private EmailMessage format(Notification notification, String username) {
    for (EmailTemplate template : templates) {
      EmailMessage email = template.format(notification);
      if (email != null) {
        User user = getUserByLogin(username);
        email.setTo(user.getEmail());
        return email;
      }
    }
    return null;
  }

  /**
   * Visibility has been relaxed for tests.
   */
  void deliver(EmailMessage emailMessage) {
    if (StringUtils.isBlank(configuration.getSmtpHost())) {
      LOG.warn("SMTP host was not configured - email will not be sent");
      return;
    }
    try {
      send(emailMessage);
    } catch (EmailException e) {
      LOG.error("Unable to send email", e);
    }
  }

  private void send(EmailMessage emailMessage) throws EmailException {
    LOG.info("Sending email: {}", emailMessage);
    // TODO
    String domain = "nemo.sonarsource.org";
    String listId = "<sonar." + domain + ">";
    String serverUrl = "http://nemo.sonarsource.org";

    SimpleEmail email = new SimpleEmail();
    /*
     * Set headers for proper threading:
     * GMail will not group messages, even if they have same subject, but don't have "In-Reply-To" and "References" headers.
     * TODO investigate threading in other clients like KMail, Thunderbird, Outlook
     */
    if (StringUtils.isNotEmpty(emailMessage.getMessageId())) {
      String messageId = "<" + emailMessage.getMessageId() + "@" + domain + ">";
      email.addHeader("In-Reply-To", messageId);
      email.addHeader("References", messageId);
    }
    // Set headers for proper filtering
    email.addHeader("List-Id", listId);
    email.addHeader("List-Archive", serverUrl);
    // Set general information
    email.setFrom(configuration.getFrom(), StringUtils.defaultIfBlank(emailMessage.getFrom(), FROM_NAME_DEFAULT));
    email.addTo(emailMessage.getTo(), " ");
    String subject = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(configuration.getPrefix()) + " ", "")
          + StringUtils.defaultString(emailMessage.getSubject(), SUBJECT_DEFAULT);
    email.setSubject(subject);
    email.setMsg(emailMessage.getMessage());
    // Send
    email.setHostName(configuration.getSmtpHost());
    email.setSmtpPort(Integer.parseInt(configuration.getSmtpPort()));
    email.setTLS(configuration.isUseTLS());
    if (StringUtils.isNotBlank(configuration.getSmtpUsername()) || StringUtils.isNotBlank(configuration.getSmtpPassword())) {
      email.setAuthentication(configuration.getSmtpUsername(), configuration.getSmtpPassword());
    }
    email.setSocketConnectionTimeout(SOCKET_TIMEOUT);
    email.setSocketTimeout(SOCKET_TIMEOUT);
    email.send();
  }

  /**
   * Send test email. This method called from Ruby.
   * 
   * @throws EmailException when unable to send
   */
  public void sendTestEmail(String toAddress, String subject, String message) throws EmailException {
    EmailMessage emailMessage = new EmailMessage();
    emailMessage.setTo(toAddress);
    emailMessage.setSubject(subject);
    emailMessage.setMessage(message);
    send(emailMessage);
  }

}
