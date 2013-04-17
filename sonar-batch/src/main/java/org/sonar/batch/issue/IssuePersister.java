/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.issue;

import org.sonar.api.database.model.Snapshot;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.ScanPersister;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class IssuePersister implements ScanPersister {

  private final IssueDao dao;
  private final IssueCache issueCache;
  private final SnapshotCache snapshotCache;
  private final RuleFinder ruleFinder;

  public IssuePersister(IssueDao dao, IssueCache issueCache, SnapshotCache snapshotCache, RuleFinder ruleFinder) {
    this.dao = dao;
    this.issueCache = issueCache;
    this.snapshotCache = snapshotCache;
    this.ruleFinder = ruleFinder;
  }

  @Override
  public void persist() {
    for (Map.Entry<String, Snapshot> componentEntry : snapshotCache.snapshots()) {
      String componentKey = componentEntry.getKey();
      Snapshot snapshot = componentEntry.getValue();
      Collection<Issue> issues = issueCache.componentIssues(componentKey);

      for (Issue issue : issues) {
        Rule rule = ruleFinder.findByKey(issue.ruleKey().repository(), issue.ruleKey().rule());
        if (rule == null) {
          throw new IllegalStateException("Rule not found: " + issue.ruleKey());
        }

        IssueDto dto = toIssueDto((DefaultIssue) issue, snapshot.getResourceId(), rule.getId());
        if (issue.isNew()) {
          dao.insert(dto);
        } else {
          // TODO do a batch update to get modified issues by user during the analysis
          dao.update(newArrayList(dto));
        }
      }
    }
  }

  private IssueDto toIssueDto(DefaultIssue issue, Integer componentId, Integer ruleId) {
    return new IssueDto()
      .setUuid(issue.key())
      .setLine(issue.line())
      .setTitle(issue.title())
      .setMessage(issue.message())
      .setCost(issue.cost())
      .setResolution(issue.resolution())
      .setStatus(issue.status())
      .setSeverity(issue.severity())
      .setChecksum(issue.getChecksum())
      .setManualIssue(issue.isManual())
      .setManualSeverity(issue.isManualSeverity())
      .setUserLogin(issue.userLogin())
      .setAssigneeLogin(issue.assigneeLogin())
      .setCreatedAt(issue.createdAt())
      .setUpdatedAt(issue.updatedAt())
      .setClosedAt(issue.closedAt())
      .setRuleId(ruleId)
      .setResourceId(componentId)
      .setData(issue.attributes() != null ? KeyValueFormat.format(issue.attributes()) : null)
      // TODO
//        .setPersonId()
      ;
  }
}
