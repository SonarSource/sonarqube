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
import java.io.Serializable;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.notifications.Notification;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;

import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_BRANCH;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_PROJECT_KEY;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_PROJECT_NAME;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_PULL_REQUEST;

public class IssueChangeNotification extends Notification {

  public static final String TYPE = "issue-changes";

  public IssueChangeNotification() {
    super(TYPE);
  }

  public IssueChangeNotification setIssue(DefaultIssue issue) {
    setFieldValue("key", issue.key());
    setFieldValue("message", issue.message());
    FieldDiffs currentChange = issue.currentChange();
    if (currentChange != null) {
      for (Map.Entry<String, FieldDiffs.Diff> entry : currentChange.diffs().entrySet()) {
        String type = entry.getKey();
        FieldDiffs.Diff diff = entry.getValue();
        setFieldValue("old." + type, neverEmptySerializableToString(diff.oldValue()));
        setFieldValue("new." + type, neverEmptySerializableToString(diff.newValue()));
      }
    }
    return this;
  }

  public IssueChangeNotification setProject(ComponentDto project) {
    return setProject(project.getKey(), project.name(), project.getBranch(), project.getPullRequest());
  }

  public IssueChangeNotification setProject(String projectKey, String projectName, @Nullable String branch, @Nullable String pullRequest) {
    setFieldValue(FIELD_PROJECT_NAME, projectName);
    setFieldValue(FIELD_PROJECT_KEY, projectKey);
    if (branch != null) {
      setFieldValue(FIELD_BRANCH, branch);
    }
    if (pullRequest != null) {
      setFieldValue(FIELD_PULL_REQUEST, pullRequest);
    }
    return this;
  }

  public IssueChangeNotification setComponent(ComponentDto component) {
    return setComponent(component.getKey(), component.longName());
  }

  public IssueChangeNotification setComponent(String componentKey, String componentName) {
    setFieldValue("componentName", componentName);
    setFieldValue("componentKey", componentKey);
    return this;
  }

  public IssueChangeNotification setChangeAuthor(@Nullable UserDto author) {
    if (author == null) {
      return this;
    }
    setFieldValue("changeAuthor", author.getLogin());
    return this;
  }

  public IssueChangeNotification setRuleName(@Nullable String s) {
    if (s != null) {
      setFieldValue("ruleName", s);
    }
    return this;
  }

  public IssueChangeNotification setComment(@Nullable String s) {
    if (s != null) {
      setFieldValue("comment", s);
    }
    return this;
  }

  @CheckForNull
  private static String neverEmptySerializableToString(@Nullable Serializable s) {
    return s != null ? Strings.emptyToNull(s.toString()) : null;
  }

  public IssueChangeNotification setAssignee(@Nullable UserDto assignee) {
    if (assignee != null) {
      setFieldValue("assignee", assignee.getLogin());
    }
    return this;
  }
}
