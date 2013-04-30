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

  public boolean setSeverity(DefaultIssue issue, String severity) {
    if (!Objects.equal(severity, issue.severity())) {
      issue.setDiff("severity", issue.severity(), severity);
      issue.setSeverity(severity);
      return true;
    }
    return false;
  }

  public boolean setManualSeverity(DefaultIssue issue, String severity) {
    if (!issue.manualSeverity() || !Objects.equal(severity, issue.severity())) {
      issue.setDiff("severity", issue.severity(), severity);
      issue.setSeverity(severity);
      issue.setManualSeverity(true);
      return true;
    }
    return false;
  }

  public boolean assign(DefaultIssue issue, @Nullable String assignee) {
    String sanitizedAssignee = StringUtils.defaultIfBlank(assignee, null);
    if (!Objects.equal(sanitizedAssignee, issue.assignee())) {
      issue.setDiff("assignee", issue.assignee(), sanitizedAssignee);
      issue.setAssignee(sanitizedAssignee);
      return true;
    }
    return false;
  }

  public DefaultIssue setLine(DefaultIssue issue, @Nullable Integer line) {
    if (!Objects.equal(line, issue.line())) {
      issue.setLine(line);
    }
    return issue;
  }

  public DefaultIssue setResolution(DefaultIssue issue, String resolution) {
    if (!Objects.equal(resolution, issue.resolution())) {
      issue.setDiff("resolution", issue.resolution(), resolution);
      issue.setResolution(resolution);
    }
    return issue;
  }

  public DefaultIssue setStatus(DefaultIssue issue, String status) {
    if (!Objects.equal(status, issue.status())) {
      issue.setDiff("status", issue.status(), status);
      issue.setStatus(status);
    }
    return issue;
  }

  public DefaultIssue setAuthorLogin(DefaultIssue issue, @Nullable String authorLogin) {
    if (!Objects.equal(authorLogin, issue.authorLogin())) {
      issue.setAuthorLogin(authorLogin);
    }
    return issue;
  }

  public DefaultIssue setDescription(DefaultIssue issue, @Nullable String description) {
    if (!Objects.equal(description, issue.description())) {
      if (issue.manual()) {
        issue.setDiff("description", issue.description(), description);
      }
      issue.setDescription(description);
    }
    return issue;
  }

  public DefaultIssue setClosedDate(DefaultIssue issue, @Nullable Date date) {
    if (!Objects.equal(date, issue.closedAt())) {
      issue.setDiff("closedDate", issue.closedAt(), date);
      issue.setClosedAt(date);
    }
    return issue;
  }

  // TODO setAttribute
  // TODO comment
}
