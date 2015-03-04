/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.notification;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;
import org.sonar.server.issue.notification.NewIssuesStatistics.METRIC;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * Creates email message for notification "new-issues".
 */
public class NewIssuesEmailTemplate extends EmailTemplate {

  public static final String FIELD_PROJECT_NAME = "projectName";
  public static final String FIELD_PROJECT_KEY = "projectKey";
  public static final String FIELD_PROJECT_DATE = "projectDate";
  public static final String FIELD_PROJECT_UUID = "projectUuid";

  private static final char NEW_LINE = '\n';
  private static final String TAB = "   ";
  private static final String DOT = ".";
  private static final String COUNT = DOT + "count";
  private static final String LABEL = DOT + "label";

  private final EmailSettings settings;
  private final I18n i18n;
  private final UserIndex userIndex;

  public NewIssuesEmailTemplate(EmailSettings settings, I18n i18n, UserIndex userIndex) {
    this.settings = settings;
    this.i18n = i18n;
    this.userIndex = userIndex;
  }

  public static String encode(String toEncode) {
    try {
      return URLEncoder.encode(toEncode, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!NewIssuesNotification.TYPE.equals(notification.getType())) {
      return null;
    }
    String projectName = notification.getFieldValue(FIELD_PROJECT_NAME);

    StringBuilder message = new StringBuilder();
    message.append("Project: ").append(projectName).append(NEW_LINE).append(NEW_LINE);
    appendSeverity(message, notification);
    appendAssignees(message, notification);
    appendTags(message, notification);
    appendComponents(message, notification);
    appendFooter(message, notification);

    return new EmailMessage()
      .setMessageId("new-issues/" + notification.getFieldValue(FIELD_PROJECT_KEY))
      .setSubject(projectName + ": " + notification.getFieldValue(METRIC.SEVERITY + COUNT) + " new issues")
      .setMessage(message.toString());
  }

  private void appendComponents(StringBuilder message, Notification notification) {
    if (notification.getFieldValue(METRIC.COMPONENT + ".1.label") == null) {
      return;
    }

    message.append("   Components:\n");
    int i = 1;
    while (notification.getFieldValue(METRIC.COMPONENT + DOT + i + LABEL) != null && i <= 5) {
      String component = notification.getFieldValue(METRIC.COMPONENT + DOT + i + LABEL);
      message
        .append(TAB).append(TAB)
        .append(component)
        .append(" : ")
        .append(notification.getFieldValue(METRIC.COMPONENT + DOT + i + COUNT))
        .append("\n");
      i += 1;
    }
  }

  private void appendAssignees(StringBuilder message, Notification notification) {
    if (notification.getFieldValue(METRIC.LOGIN + DOT + "1" + LABEL) == null) {
      return;
    }

    message.append(TAB + "Assignee - ");
    int i = 1;
    while (notification.getFieldValue(METRIC.LOGIN + DOT + i + LABEL) != null && i <= 5) {
      String login = notification.getFieldValue(METRIC.LOGIN + DOT + i + LABEL);
      UserDoc user = userIndex.getNullableByLogin(login);
      String name = user == null ? null : user.name();
      message.append(Objects.firstNonNull(name, login))
        .append(": ")
        .append(notification.getFieldValue(METRIC.LOGIN + DOT + i + COUNT));
      if (i < 5) {
        message.append(TAB);
      }
      i += 1;
    }

    message.append("\n");
  }

  private void appendTags(StringBuilder message, Notification notification) {
    if (notification.getFieldValue(METRIC.TAGS + DOT + "1" + LABEL) == null) {
      return;
    }

    message.append(TAB + "Tags - ");
    int i = 1;
    while (notification.getFieldValue(METRIC.TAGS + DOT + i + LABEL) != null && i <= 5) {
      String tag = notification.getFieldValue(METRIC.TAGS + DOT + i + LABEL);
      message.append(tag)
        .append(": ")
        .append(notification.getFieldValue(METRIC.TAGS + DOT + i + COUNT));
      if (i < 5) {
        message.append(TAB);
      }
      i += 1;
    }
    message.append(NEW_LINE);
  }

  private void appendSeverity(StringBuilder message, Notification notification) {
    message.append(notification.getFieldValue(METRIC.SEVERITY + COUNT)).append(" new issues - Total debt: ")
      .append(notification.getFieldValue(METRIC.DEBT + COUNT))
      .append(NEW_LINE).append(NEW_LINE)
      .append(TAB + "Severity - ");
    for (Iterator<String> severityIterator = Lists.reverse(Severity.ALL).iterator(); severityIterator.hasNext();) {
      String severity = severityIterator.next();
      String severityLabel = i18n.message(getLocale(), "severity." + severity, severity);
      message.append(severityLabel).append(": ").append(notification.getFieldValue(METRIC.SEVERITY + DOT + severity + COUNT));
      if (severityIterator.hasNext()) {
        message.append(TAB);
      }
    }
    message.append(NEW_LINE);
  }

  private void appendFooter(StringBuilder message, Notification notification) {
    String projectUuid = notification.getFieldValue(FIELD_PROJECT_UUID);
    String dateString = notification.getFieldValue(FIELD_PROJECT_DATE);
    if (projectUuid != null && dateString != null) {
      Date date = DateUtils.parseDateTime(dateString);
      String url = String.format("%s/issues/search#projectUuids=%s|createdAt=%s",
        settings.getServerBaseURL(), encode(projectUuid), encode(DateUtils.formatDateTime(date)));
      message.append("\n").append("See it in SonarQube: ").append(url).append(NEW_LINE);
    }
  }

  private Locale getLocale() {
    return Locale.ENGLISH;
  }

}
