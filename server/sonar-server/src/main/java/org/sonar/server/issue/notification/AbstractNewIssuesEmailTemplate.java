/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.issue.notification;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import javax.annotation.Nullable;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rules.RuleType;
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
  static final String FIELD_PROJECT_VERSION = "projectVersion";
  static final String FIELD_ASSIGNEE = "assignee";
  static final String FIELD_BRANCH = "branch";

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
    String branchName = notification.getFieldValue(FIELD_BRANCH);

    StringBuilder message = new StringBuilder();
    message.append("Project: ").append(projectName).append(NEW_LINE);
    if (branchName != null) {
      message.append("Branch: ").append(branchName).append(NEW_LINE);
    }
    String version = notification.getFieldValue(FIELD_PROJECT_VERSION);
    if (version != null) {
      message.append("Version: ").append(version).append(NEW_LINE);
    }
    message.append(NEW_LINE);
    appendRuleType(message, notification);
    appendAssignees(message, notification);
    appendRules(message, notification);
    appendTags(message, notification);
    appendComponents(message, notification);
    appendFooter(message, notification);

    return new EmailMessage()
      .setMessageId(notification.getType() + "/" + notification.getFieldValue(FIELD_PROJECT_KEY))
      .setSubject(subject(notification, computeFullProjectName(projectName, branchName)))
      .setMessage(message.toString());
  }

  private static String computeFullProjectName(String projectName, @Nullable String branchName) {
    if (branchName == null || branchName.isEmpty()) {
      return projectName;
    }
    return String.format("%s (%s)", projectName, branchName);
  }

  protected abstract boolean shouldNotFormat(Notification notification);

  protected String subject(Notification notification, String fullProjectName) {
    int issueCount = Integer.parseInt(notification.getFieldValue(Metric.RULE_TYPE + COUNT));
    return String.format("%s: %s new issue%s (new debt: %s)",
      fullProjectName,
      issueCount,
      issueCount > 1 ? "s" : "",
      notification.getFieldValue(Metric.EFFORT + COUNT));
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

  protected void appendRuleType(StringBuilder message, Notification notification) {
    String count = notification.getFieldValue(Metric.RULE_TYPE + COUNT);
    message
      .append(String.format("%s new issue%s (new debt: %s)",
        count,
        Integer.valueOf(count) > 1 ? "s" : "",
        notification.getFieldValue(Metric.EFFORT + COUNT)))
      .append(NEW_LINE).append(NEW_LINE)
      .append(TAB)
      .append("Type")
      .append(NEW_LINE)
      .append(TAB)
      .append(TAB);

    for (Iterator<RuleType> ruleTypeIterator = Arrays.asList(RuleType.BUG, RuleType.VULNERABILITY, RuleType.CODE_SMELL).iterator(); ruleTypeIterator.hasNext();) {
      RuleType ruleType = ruleTypeIterator.next();
      String ruleTypeLabel = i18n.message(getLocale(), "issue.type." + ruleType, ruleType.name());
      message.append(ruleTypeLabel).append(": ").append(notification.getFieldValue(Metric.RULE_TYPE + DOT + ruleType + COUNT));
      if (ruleTypeIterator.hasNext()) {
        message.append(TAB);
      }
    }

    message
      .append(NEW_LINE)
      .append(NEW_LINE);
  }

  protected void appendFooter(StringBuilder message, Notification notification) {
    String projectKey = notification.getFieldValue(FIELD_PROJECT_KEY);
    String dateString = notification.getFieldValue(FIELD_PROJECT_DATE);
    if (projectKey != null && dateString != null) {
      Date date = DateUtils.parseDateTime(dateString);
      String url = String.format("%s/project/issues?id=%s",
        settings.getServerBaseURL(), encode(projectKey));
      String branchName = notification.getFieldValue(FIELD_BRANCH);
      if (branchName != null) {
        url += "&branch=" + encode(branchName);
      }
      url += "&createdAt=" + encode(DateUtils.formatDateTime(date));
      message
        .append("More details at: ")
        .append(url)
        .append(NEW_LINE);
    }
  }

  private static Locale getLocale() {
    return Locale.ENGLISH;
  }

}
