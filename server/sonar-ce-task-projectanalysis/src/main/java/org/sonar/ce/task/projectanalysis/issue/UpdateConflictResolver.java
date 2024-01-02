/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.issue.IssueDto;

/**
 * Support concurrent modifications on issues made by analysis and users at the same time
 * See https://jira.sonarsource.com/browse/SONAR-4309
 */
public class UpdateConflictResolver {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateConflictResolver.class);

  public IssueDto resolve(DefaultIssue issue, IssueDto dbIssue) {
    LOG.debug("Resolve conflict on issue {}", issue.key());
    mergeFields(dbIssue, issue);
    return IssueDto.toDtoForUpdate(issue, System.currentTimeMillis());
  }

  @VisibleForTesting
  void mergeFields(IssueDto dbIssue, DefaultIssue issue) {
    resolveAssignee(dbIssue, issue);
    resolveSeverity(dbIssue, issue);
    resolveEffortToFix(dbIssue, issue);
    resolveResolution(dbIssue, issue);
    resolveStatus(dbIssue, issue);
  }

  private static void resolveStatus(IssueDto dbIssue, DefaultIssue issue) {
    issue.setStatus(dbIssue.getStatus());
  }

  private static void resolveResolution(IssueDto dbIssue, DefaultIssue issue) {
    issue.setResolution(dbIssue.getResolution());
  }

  private static void resolveEffortToFix(IssueDto dbIssue, DefaultIssue issue) {
    issue.setGap(dbIssue.getGap());
  }

  private static void resolveSeverity(IssueDto dbIssue, DefaultIssue issue) {
    if (dbIssue.isManualSeverity()) {
      issue.setManualSeverity(true);
      issue.setSeverity(dbIssue.getSeverity());
    }
    // else keep severity as declared in quality profile
  }

  private static void resolveAssignee(IssueDto dbIssue, DefaultIssue issue) {
    issue.setAssigneeUuid(dbIssue.getAssigneeUuid());
  }
}
