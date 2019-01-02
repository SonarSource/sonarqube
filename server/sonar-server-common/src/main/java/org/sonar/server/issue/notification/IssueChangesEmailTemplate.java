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
package org.sonar.server.issue.notification;

import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import static java.net.URLEncoder.encode;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_BRANCH;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_PULL_REQUEST;

/**
 * Creates email message for notification "issue-changes".
 */
public class IssueChangesEmailTemplate extends EmailTemplate {

  private static final char NEW_LINE = '\n';
  private final DbClient dbClient;
  private final EmailSettings settings;

  public IssueChangesEmailTemplate(DbClient dbClient, EmailSettings settings) {
    this.dbClient = dbClient;
    this.settings = settings;
  }

  @Override
  public EmailMessage format(Notification notif) {
    if (!IssueChangeNotification.TYPE.equals(notif.getType())) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    appendHeader(notif, sb);
    sb.append(NEW_LINE);
    appendChanges(notif, sb);
    sb.append(NEW_LINE);
    appendFooter(sb, notif);

    String projectName = notif.getFieldValue("projectName");
    String issueKey = notif.getFieldValue("key");
    String author = notif.getFieldValue("changeAuthor");

    EmailMessage message = new EmailMessage()
      .setMessageId("issue-changes/" + issueKey)
      .setSubject(projectName + ", change on issue #" + issueKey)
      .setMessage(sb.toString());
    if (author != null) {
      message.setFrom(getUserFullName(author));
    }
    return message;
  }

  private static void appendChanges(Notification notif, StringBuilder sb) {
    appendField(sb, "Comment", null, notif.getFieldValue("comment"));
    appendFieldWithoutHistory(sb, "Assignee", notif.getFieldValue("old.assignee"), notif.getFieldValue("new.assignee"));
    appendField(sb, "Severity", notif.getFieldValue("old.severity"), notif.getFieldValue("new.severity"));
    appendField(sb, "Type", notif.getFieldValue("old.type"), notif.getFieldValue("new.type"));
    appendField(sb, "Resolution", notif.getFieldValue("old.resolution"), notif.getFieldValue("new.resolution"));
    appendField(sb, "Status", notif.getFieldValue("old.status"), notif.getFieldValue("new.status"));
    appendField(sb, "Message", notif.getFieldValue("old.message"), notif.getFieldValue("new.message"));
    appendField(sb, "Author", notif.getFieldValue("old.author"), notif.getFieldValue("new.author"));
    appendFieldWithoutHistory(sb, "Action Plan", notif.getFieldValue("old.actionPlan"), notif.getFieldValue("new.actionPlan"));
    appendField(sb, "Tags", formatTagChange(notif.getFieldValue("old.tags")), formatTagChange(notif.getFieldValue("new.tags")));
  }

  @CheckForNull
  private static String formatTagChange(@Nullable String tags) {
    if (tags == null) {
      return null;
    } else {
      return "[" + tags + "]";
    }
  }

  private static void appendHeader(Notification notif, StringBuilder sb) {
    appendLine(sb, StringUtils.defaultString(notif.getFieldValue("componentName"), notif.getFieldValue("componentKey")));
    String branchName = notif.getFieldValue(FIELD_BRANCH);
    if (branchName != null) {
      appendField(sb, "Branch", null, branchName);
    }
    String pullRequest = notif.getFieldValue(FIELD_PULL_REQUEST);
    if (pullRequest != null) {
      appendField(sb, "Pull request", null, pullRequest);
    }
    appendField(sb, "Rule", null, notif.getFieldValue("ruleName"));
    appendField(sb, "Message", null, notif.getFieldValue("message"));
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String issueKey = notification.getFieldValue("key");
    try {
      sb.append("More details at: ").append(settings.getServerBaseURL())
        .append("/project/issues?id=").append(encode(notification.getFieldValue("projectKey"), "UTF-8"))
        .append("&issues=").append(issueKey)
        .append("&open=").append(issueKey);
      String branchName = notification.getFieldValue(FIELD_BRANCH);
      if (branchName != null) {
        sb.append("&branch=").append(branchName);
      }
      String pullRequest = notification.getFieldValue(FIELD_PULL_REQUEST);
      if (pullRequest != null) {
        sb.append("&pullRequest=").append(pullRequest);
      }
      sb.append(NEW_LINE);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

  private static void appendLine(StringBuilder sb, @Nullable String line) {
    if (!Strings.isNullOrEmpty(line)) {
      sb.append(line).append(NEW_LINE);
    }
  }

  private static void appendField(StringBuilder sb, String name, @Nullable String oldValue, @Nullable String newValue) {
    if (oldValue != null || newValue != null) {
      sb.append(name).append(": ");
      if (newValue != null) {
        sb.append(newValue);
      }
      if (oldValue != null) {
        sb.append(" (was ").append(oldValue).append(")");
      }
      sb.append(NEW_LINE);
    }
  }

  private static void appendFieldWithoutHistory(StringBuilder sb, String name, @Nullable String oldValue, @Nullable String newValue) {
    if (oldValue != null || newValue != null) {
      sb.append(name);
      if (newValue != null) {
        sb.append(" changed to ");
        sb.append(newValue);
      } else {
        sb.append(" removed");
      }
      sb.append(NEW_LINE);
    }
  }

  private String getUserFullName(@Nullable String login) {
    if (login == null) {
      return null;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = dbClient.userDao().selectByLogin(dbSession, login);
      if (userDto == null || !userDto.isActive()) {
        // most probably user was deleted
        return login;
      }
      return StringUtils.defaultIfBlank(userDto.getName(), login);
    }
  }

}
