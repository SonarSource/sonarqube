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
package org.sonar.server.notifications.email;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.notifications.Notification;
import org.sonar.server.notifications.NotificationChannel;

import java.io.Serializable;

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

  private static final String FROM_DEFAULT = "Sonar";
  private static final String SUBJECT_PREFIX = "[Sonar]";
  private static final String SUBJECT_DEFAULT = "Notification";

  private EmailMessageTemplate[] templates;

  public EmailNotificationChannel(EmailMessageTemplate[] templates) {
    this.templates = templates;
  }

  @Override
  public Serializable createDataForPersistance(Notification notification, Integer userId) {
    for (EmailMessageTemplate template : templates) {
      EmailMessage email = template.format(notification);
      if (email != null) {
        email.setTo(userId.toString()); // TODO should be valid email@address
        return email;
      }
    }
    return null;
  }

  @Override
  public void deliver(Serializable notificationData) {
    EmailMessage email = (EmailMessage) notificationData;
    LOG.info("Email:\n{}", create(email));
  }

  /**
   * Visibility has been relaxed for tests.
   */
  String create(EmailMessage email) {
    // TODO
    String serverUrl = "http://nemo.sonarsource.org";
    String domain = "nemo.sonarsource.org";
    String listId = "<sonar." + domain + ">";
    String from = StringUtils.defaultString(email.getFrom(), FROM_DEFAULT) + " <noreply@" + domain + ">";
    String subject = SUBJECT_PREFIX + " " + StringUtils.defaultString(email.getSubject(), SUBJECT_DEFAULT);
    String permalink = null;
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotEmpty(email.getMessageId())) {
      subject = "Re: " + subject;
      String messageId = "<" + email.getMessageId() + "@" + domain + ">";
      appendHeader(sb, "Message-Id", messageId);
      appendHeader(sb, "In-Reply-To", messageId);
      appendHeader(sb, "References", messageId);
      permalink = serverUrl + '/' + email.getMessageId();
    }
    appendHeader(sb, "List-Id", listId);
    appendHeader(sb, "List-Archive", serverUrl);
    appendHeader(sb, "From", from);
    appendHeader(sb, "To", email.getTo());
    appendHeader(sb, "Subject", subject);
    sb.append('\n')
        .append(email.getMessage()).append('\n');
    if (permalink != null) {
      sb.append('\n')
          .append("--\n")
          .append("View it in Sonar: ").append(permalink);
    }
    return sb.toString();
  }

  private void appendHeader(StringBuilder sb, String header, String value) {
    sb.append(header).append(": ").append(value).append('\n');
  }

}
