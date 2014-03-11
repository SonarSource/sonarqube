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
package org.sonar.plugins.core.issue.notification;

import com.google.common.collect.Lists;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * Creates email message for notification "new-issues".
 *
 * @since 2.10
 */
public class NewIssuesEmailTemplate extends EmailTemplate {

  public static final String FIELD_PROJECT_NAME = "projectName";
  public static final String FIELD_PROJECT_KEY = "projectKey";
  public static final String FIELD_PROJECT_DATE = "projectDate";

  private final EmailSettings settings;
  private final I18n i18n;

  public NewIssuesEmailTemplate(EmailSettings settings, I18n i18n) {
    this.settings = settings;
    this.i18n = i18n;
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!"new-issues".equals(notification.getType())) {
      return null;
    }
    String projectName = notification.getFieldValue(FIELD_PROJECT_NAME);

    StringBuilder sb = new StringBuilder();
    sb.append("Project: ").append(projectName).append("\n\n");
    sb.append(notification.getFieldValue("count")).append(" new issues").append("\n\n");
    sb.append("   ");
    for (Iterator<String> severityIterator = Lists.reverse(Severity.ALL).iterator(); severityIterator.hasNext(); ) {
      String severity = severityIterator.next();
      String severityLabel = i18n.message(getLocale(), "severity."+ severity, severity);
      sb.append(severityLabel).append(": ").append(notification.getFieldValue("count-"+ severity));
      if (severityIterator.hasNext()) {
        sb.append("   ");
      }
    }
    sb.append('\n');

    appendFooter(sb, notification);

    EmailMessage message = new EmailMessage()
      .setMessageId("new-issues/" + notification.getFieldValue(FIELD_PROJECT_KEY))
      .setSubject(projectName + ": new issues")
      .setMessage(sb.toString());

    return message;
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String projectKey = notification.getFieldValue(FIELD_PROJECT_KEY);
    String dateString = notification.getFieldValue(FIELD_PROJECT_DATE);
    if (projectKey != null && dateString != null) {
      Date date = DateUtils.parseDateTime(dateString);
      String url = String.format("%s/issues/search#componentRoots=%s|createdAt=%s",
        settings.getServerBaseURL(), encode(projectKey), encode(DateUtils.formatDateTime(date)));
      sb.append("\n").append("See it in SonarQube: ").append(url).append("\n");
    }
  }

  public static String encode(String toEncode) {
    try {
      return URLEncoder.encode(toEncode, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

  private Locale getLocale() {
    return Locale.ENGLISH;
  }

}
