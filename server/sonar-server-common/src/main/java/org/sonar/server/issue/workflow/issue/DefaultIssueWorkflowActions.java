/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.issue.workflow.issue;

import java.util.Comparator;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowActions;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowActions;

/**
 * Common to code quality issues and security hotspots, doing actions on {@link DefaultIssue}
 */
public class DefaultIssueWorkflowActions implements CodeQualityIssueWorkflowActions, SecurityHotspotWorkflowActions {
  private final IssueFieldsSetter updater;
  private final DefaultIssue issue;
  private final IssueChangeContext changeContext;

  public DefaultIssueWorkflowActions(IssueFieldsSetter updater, DefaultIssue issue, IssueChangeContext changeContext) {
    this.updater = updater;
    this.issue = issue;
    this.changeContext = changeContext;
  }

  @Override
  public void unsetAssignee() {
    updater.assign(issue, null, changeContext);
  }

  @Override
  public void unsetResolution() {
    setResolution(null);
  }

  @Override
  public void setResolution(@Nullable String s) {
    updater.setResolution(issue, s, changeContext);
  }

  @Override
  public void setCloseDate() {
    updater.setCloseDate(issue, changeContext.date(), changeContext);
  }

  @Override
  public void setClosed() {
    if (issue.isOnDisabledRule()) {
      setResolution(Issue.RESOLUTION_REMOVED);
    } else {
      setResolution(Issue.RESOLUTION_FIXED);
    }

    // closed issues are not "tracked" -> the line number does not evolve anymore
    // when code changes. That's misleading for end-users, so line number
    // is unset.
    updater.unsetLine(issue, changeContext);
  }

  @Override
  public void unsetCloseDate() {
    updater.setCloseDate(issue, null, changeContext);
  }

  @Override
  public void addComment(String comment) {
    DefaultIssueComment defaultIssueComment = DefaultIssueComment.create(issue.key(), comment);
    issue.addComment(defaultIssueComment);
  }

  @Override
  public void restoreResolution() {
    String previousResolution = issue.changes().stream()
      // exclude current change (if any)
      .filter(change -> change != issue.currentChange())
      .filter(change -> change.creationDate() != null)
      .sorted(Comparator.comparing(FieldDiffs::creationDate).reversed())
      .map(DefaultIssueWorkflowActions::parse)
      .filter(Objects::nonNull)
      .filter(StatusAndResolutionDiffs::hasResolution)
      .findFirst()
      .map(t -> t.newStatusClosed ? t.oldResolution : t.newResolution)
      .orElse(null);
    setResolution(previousResolution);
  }

  @CheckForNull
  private static StatusAndResolutionDiffs parse(FieldDiffs fieldDiffs) {
    FieldDiffs.Diff status = fieldDiffs.get("status");
    if (status == null) {
      return null;
    }
    FieldDiffs.Diff resolution = fieldDiffs.get("resolution");
    if (resolution == null) {
      return new StatusAndResolutionDiffs(Issue.STATUS_CLOSED.equals(status.newValue()), null, null);
    }
    return new StatusAndResolutionDiffs(Issue.STATUS_CLOSED.equals(status.newValue()), (String) resolution.oldValue(), (String) resolution.newValue());
  }

  private record StatusAndResolutionDiffs(boolean newStatusClosed, String oldResolution, String newResolution) {
    private StatusAndResolutionDiffs(boolean newStatusClosed, @Nullable String oldResolution, @Nullable String newResolution) {
      this.newStatusClosed = newStatusClosed;
      this.oldResolution = emptyToNull(oldResolution);
      this.newResolution = emptyToNull(newResolution);
    }

    private static String emptyToNull(@Nullable String str) {
      if (str == null || str.isEmpty()) {
        return null;
      }
      return str;
    }

    boolean hasResolution() {
      return oldResolution != null || newResolution != null;
    }
  }

  @Override
  public void setStatus(@Nullable IssueStatus previousIssueStatus, String newStatus) {
    updater.setStatus(issue, newStatus, changeContext);
    updater.setIssueStatus(issue, previousIssueStatus, issue.issueStatus(), changeContext);
  }
}
