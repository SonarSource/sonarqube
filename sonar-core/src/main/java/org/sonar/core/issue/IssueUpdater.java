/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.issue;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;

import javax.annotation.Nullable;

import java.util.Date;

/**
 * Updates issue fields and chooses if changes must be kept in history.
 */
public class IssueUpdater implements BatchComponent, ServerComponent {

  public boolean setSeverity(DefaultIssue issue, String severity, IssueChangeContext context) {
    if (issue.manualSeverity()) {
      throw new IllegalStateException("Severity can't be changed");
    }
    if (!Objects.equal(severity, issue.severity())) {
      issue.setFieldDiff(context, "severity", issue.severity(), severity);
      issue.setSeverity(severity);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastSeverity(DefaultIssue issue, @Nullable String previousSeverity, IssueChangeContext context) {
    String currentSeverity = issue.severity();
    issue.setSeverity(previousSeverity);
    return setSeverity(issue, currentSeverity, context);
  }

  public boolean setManualSeverity(DefaultIssue issue, String severity, IssueChangeContext context) {
    if (!issue.manualSeverity() || !Objects.equal(severity, issue.severity())) {
      issue.setFieldDiff(context, "severity", issue.severity(), severity);
      issue.setSeverity(severity);
      issue.setManualSeverity(true);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean assign(DefaultIssue issue, @Nullable String assignee, IssueChangeContext context) {
    String sanitizedAssignee = StringUtils.defaultIfBlank(assignee, null);
    if (!Objects.equal(sanitizedAssignee, issue.assignee())) {
      issue.setFieldDiff(context, "assignee", issue.assignee(), sanitizedAssignee);
      issue.setAssignee(sanitizedAssignee);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setLine(DefaultIssue issue, @Nullable Integer line) {
    if (!Objects.equal(line, issue.line())) {
      issue.setLine(line);
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastLine(DefaultIssue issue, @Nullable Integer previousLine) {
    Integer currentLine = issue.line();
    issue.setLine(previousLine);
    return setLine(issue, currentLine);
  }

  public boolean setResolution(DefaultIssue issue, @Nullable String resolution, IssueChangeContext context) {
    if (!Objects.equal(resolution, issue.resolution())) {
      issue.setFieldDiff(context, "resolution", issue.resolution(), resolution);
      issue.setResolution(resolution);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setStatus(DefaultIssue issue, String status, IssueChangeContext context) {
    if (!Objects.equal(status, issue.status())) {
      issue.setFieldDiff(context, "status", issue.status(), status);
      issue.setStatus(status);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setAuthorLogin(DefaultIssue issue, @Nullable String authorLogin, IssueChangeContext context) {
    if (!Objects.equal(authorLogin, issue.authorLogin())) {
      issue.setFieldDiff(context, "author", issue.authorLogin(), authorLogin);
      issue.setAuthorLogin(authorLogin);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setMessage(DefaultIssue issue, @Nullable String s, IssueChangeContext context) {
    if (!Objects.equal(s, issue.message())) {
      issue.setMessage(s);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastMessage(DefaultIssue issue, @Nullable String previousMessage, IssueChangeContext context) {
    String currentMessage = issue.message();
    issue.setMessage(previousMessage);
    return setMessage(issue, currentMessage, context);
  }

  public void addComment(DefaultIssue issue, String text, IssueChangeContext context) {
    issue.addComment(DefaultIssueComment.create(issue.key(), context.login(), text));
    issue.setUpdateDate(context.date());
    issue.setChanged(true);
  }

  public void setCloseDate(DefaultIssue issue, @Nullable Date d, IssueChangeContext context) {
    if (!Objects.equal(d, issue.closeDate())) {
      issue.setCloseDate(d);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
    }
  }

  public boolean setEffortToFix(DefaultIssue issue, @Nullable Double d, IssueChangeContext context) {
    if (!Objects.equal(d, issue.effortToFix())) {
      issue.setEffortToFix(d);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastEffortToFix(DefaultIssue issue, @Nullable Double previousEffort, IssueChangeContext context) {
    Double currentEffort = issue.effortToFix();
    issue.setEffortToFix(previousEffort);
    return setEffortToFix(issue, currentEffort, context);
  }

  public boolean setAttribute(DefaultIssue issue, String key, @Nullable String value, IssueChangeContext context) {
    String oldValue = issue.attribute(key);
    if (!Objects.equal(oldValue, value)) {
      issue.setFieldDiff(context, key, oldValue, value);
      issue.setAttribute(key, value);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean plan(DefaultIssue issue, @Nullable String actionPlanKey, IssueChangeContext context) {
    String sanitizedActionPlanKey = StringUtils.defaultIfBlank(actionPlanKey, null);
    if (!Objects.equal(sanitizedActionPlanKey, issue.actionPlanKey())) {
      issue.setFieldDiff(context, "actionPlanKey", issue.actionPlanKey(), sanitizedActionPlanKey);
      issue.setActionPlanKey(sanitizedActionPlanKey);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }
}
