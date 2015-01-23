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
package org.sonar.core.issue.db;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.internal.DefaultIssue;

/**
 * Support concurrent modifications on issues made by analysis and users at the same time
 * See https://jira.codehaus.org/browse/SONAR-4309
 */
public class UpdateConflictResolver {

  private static final Logger LOG = LoggerFactory.getLogger(IssueStorage.class);

  public void resolve(DefaultIssue issue, IssueMapper mapper) {
    LOG.debug("Resolve conflict on issue " + issue.key());

    IssueDto dbIssue = mapper.selectByKey(issue.key());
    if (dbIssue != null) {
      mergeFields(dbIssue, issue);
      mapper.update(IssueDto.toDtoForUpdate(issue, System.currentTimeMillis()));
    }
  }

  @VisibleForTesting
  void mergeFields(IssueDto dbIssue, DefaultIssue issue) {
    resolveAssignee(dbIssue, issue);
    resolvePlan(dbIssue, issue);
    resolveSeverity(dbIssue, issue);
    resolveEffortToFix(dbIssue, issue);
    resolveResolution(dbIssue, issue);
    resolveStatus(dbIssue, issue);
  }

  private void resolveStatus(IssueDto dbIssue, DefaultIssue issue) {
    issue.setStatus(dbIssue.getStatus());
  }

  private void resolveResolution(IssueDto dbIssue, DefaultIssue issue) {
    issue.setResolution(dbIssue.getResolution());
  }

  private void resolveEffortToFix(IssueDto dbIssue, DefaultIssue issue) {
    issue.setEffortToFix(dbIssue.getEffortToFix());
  }

  private void resolveSeverity(IssueDto dbIssue, DefaultIssue issue) {
    if (dbIssue.isManualSeverity()) {
      issue.setManualSeverity(true);
      issue.setSeverity(dbIssue.getSeverity());
    }
    // else keep severity as declared in quality profile
  }

  private void resolvePlan(IssueDto dbIssue, DefaultIssue issue) {
    issue.setActionPlanKey(dbIssue.getActionPlanKey());
  }

  private void resolveAssignee(IssueDto dbIssue, DefaultIssue issue) {
    issue.setAssignee(dbIssue.getAssignee());
  }
}
