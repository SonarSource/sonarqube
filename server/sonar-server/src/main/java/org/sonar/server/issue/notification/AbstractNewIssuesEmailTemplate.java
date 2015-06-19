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

import com.google.common.collect.Lists;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;
import org.sonar.server.issue.notification.NewIssuesStatistics.Metric;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class to create emails for new issues
 */
public abstract class AbstractNewIssuesEmailTemplate extends EmailTemplate {

  protected static final char NEW_LINE = '\n';
  protected static final String TAB = "    ";
  protected static final String DOT = ".";
  protected static final String COUNT = DOT + "count";
  protected static final String LABEL = DOT + "label";

  static final String FIELD_PROJECT_NAME = "projectName";
  static final String FIELD_PROJECT_KEY = "projectKey";
  static final String FIELD_PROJECT_DATE = "projectDate";
  static final String FIELD_PROJECT_UUID = "projectUuid";
  static final String FIELD_ASSIGNEE = "assignee";

  protected final EmailSettings settings;
  protected final I18n i18n;

  public AbstractNewIssuesEmailTemplate(EmailSettings settings, I18n i18n) {
    this.settings = settings;
    this.i18n = i18n;
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
    if (shouldNotFormat(notification)) {
      return null;
    }
    String projectName = checkNotNull(notification.getFieldValue(FIELD_PROJECT_NAME));

    StringBuilder message = new StringBuilder();
    message.append("Project: ").append(projectName).append(NEW_LINE).append(NEW_LINE);
    appendSeverity(message, notification);
    appendAssignees(message, notification);
    appendRules(message, notification);
    appendTags(message, notification);
    appendComponents(message, notification);
    appendFooter(message, notification);

    return new EmailMessage()
      .setMessageId(notification.getType() + "/" + notification.getFieldValue(FIELD_PROJECT_KEY))
      .setSubject(subject(notification, projectName))
      .setMessage(message.toString());
  }

  protected abstract boolean shouldNotFormat(Notification notification);

  protected String subject(Notification notification, String projectName) {
    return String.format("%s: %s new issues (new debt: %s)",
      projectName,
      notification.getFieldValue(Metric.SEVERITY + COUNT),
      notification.getFieldValue(Metric.DEBT + COUNT));
  }

  private static boolean doNotHaveValue(Notification notification, Metric metric) {
    return notification.getFieldValue(metric + DOT + "1" + LABEL) == null;
  }

  private static void genericAppendOfMetric(Metric metric, String label, StringBuilder message, Notification notification) {
    if (doNotHaveValue(notification, metric)) {
      return;
    }

    message
      .append(TAB)
      .append(label)
      .append(NEW_LINE);
    int i = 1;
    while (notification.getFieldValue(metric + DOT + i + LABEL) != null && i <= 5) {
      String name = notification.getFieldValue(metric + DOT + i + LABEL);
      message
        .append(TAB).append(TAB)
        .append(name)
        .append(": ")
        .append(notification.getFieldValue(metric + DOT + i + COUNT))
        .append(NEW_LINE);
      i += 1;
    }

    message.append(NEW_LINE);
  }

  protected void appendAssignees(StringBuilder message, Notification notification) {
    genericAppendOfMetric(Metric.ASSIGNEE, "Assignees", message, notification);
  }

  protected void appendComponents(StringBuilder message, Notification notification) {
    genericAppendOfMetric(Metric.COMPONENT, "Most impacted files", message, notification);
  }

  protected void appendTags(StringBuilder message, Notification notification) {
    genericAppendOfMetric(Metric.TAG, "Tags", message, notification);
  }

  protected void appendRules(StringBuilder message, Notification notification) {
    genericAppendOfMetric(Metric.RULE, "Rules", message, notification);
  }

  protected void appendSeverity(StringBuilder message, Notification notification) {
    message
      .append(String.format("%s new issues (new debt: %s)",
        notification.getFieldValue(Metric.SEVERITY + COUNT),
        notification.getFieldValue(Metric.DEBT + COUNT)))
      .append(NEW_LINE).append(NEW_LINE)
      .append(TAB)
      .append("Severity")
      .append(NEW_LINE)
      .append(TAB)
      .append(TAB);

    for (Iterator<String> severityIterator = Lists.reverse(Severity.ALL).iterator(); severityIterator.hasNext();) {
      String severity = severityIterator.next();
      String severityLabel = i18n.message(getLocale(), "severity." + severity, severity);
      message.append(severityLabel).append(": ").append(notification.getFieldValue(Metric.SEVERITY + DOT + severity + COUNT));
      if (severityIterator.hasNext()) {
        message.append(TAB);
      }
    }

    message
      .append(NEW_LINE)
      .append(NEW_LINE);
  }

  protected void appendFooter(StringBuilder message, Notification notification) {
    String projectUuid = notification.getFieldValue(FIELD_PROJECT_UUID);
    String dateString = notification.getFieldValue(FIELD_PROJECT_DATE);
    if (projectUuid != null && dateString != null) {
      Date date = DateUtils.parseDateTime(dateString);
      String url = String.format("%s/issues/search#projectUuids=%s|createdAt=%s",
        settings.getServerBaseURL(), encode(projectUuid), encode(DateUtils.formatDateTime(date)));
      message
        .append("See it in SonarQube: ")
        .append(url)
        .append(NEW_LINE);
    }
  }

  private static Locale getLocale() {
    return Locale.ENGLISH;
  }

}
