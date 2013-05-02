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

import javax.annotation.Nullable;

import java.util.Date;

public class IssueUpdater implements BatchComponent, ServerComponent {

  public boolean setSeverity(DefaultIssue issue, String severity, IssueChangeContext context) {
    if (issue.manualSeverity()) {
      throw new IllegalStateException("Severity can't be changed");
    }
    if (!Objects.equal(severity, issue.severity())) {
      issue.setFieldDiff(context, "severity", issue.severity(), severity);
      issue.setSeverity(severity);
      return true;
    }
    return false;
  }

  public boolean setManualSeverity(DefaultIssue issue, String severity, IssueChangeContext context) {
    if (!issue.manualSeverity() || !Objects.equal(severity, issue.severity())) {
      issue.setFieldDiff(context, "severity", issue.severity(), severity);
      issue.setSeverity(severity);
      issue.setManualSeverity(true);
      return true;
    }
    return false;
  }

  public boolean assign(DefaultIssue issue, @Nullable String assignee, IssueChangeContext context) {
    String sanitizedAssignee = StringUtils.defaultIfBlank(assignee, null);
    if (!Objects.equal(sanitizedAssignee, issue.assignee())) {
      issue.setFieldDiff(context, "assignee", issue.assignee(), sanitizedAssignee);
      issue.setAssignee(sanitizedAssignee);
      return true;
    }
    return false;
  }

  public boolean setLine(DefaultIssue issue, @Nullable Integer line) {
    if (!Objects.equal(line, issue.line())) {
      issue.setLine(line);
      return true;
    }
    return false;
  }

  public boolean setResolution(DefaultIssue issue, String resolution, IssueChangeContext context) {
    if (!Objects.equal(resolution, issue.resolution())) {
      issue.setFieldDiff(context, "resolution", issue.resolution(), resolution);
      issue.setResolution(resolution);
      return true;
    }
    return false;
  }

  public boolean setStatus(DefaultIssue issue, String status, IssueChangeContext context) {
    if (!Objects.equal(status, issue.status())) {
      issue.setFieldDiff(context, "status", issue.status(), status);
      issue.setStatus(status);
      return true;
    }
    return false;
  }

  public void setAuthorLogin(DefaultIssue issue, @Nullable String authorLogin) {
    issue.setAuthorLogin(authorLogin);
  }

  public void setDescription(DefaultIssue issue, @Nullable String description) {
    issue.setDescription(description);
  }

  public void addComment(DefaultIssue issue, String text, IssueChangeContext context) {
    issue.addComment(IssueComment.create(context.login(), text));
  }

  public void setClosedDate(DefaultIssue issue, @Nullable Date date) {
    issue.setClosedAt(date);
  }

  // TODO setAttribute
  // TODO comment
}
