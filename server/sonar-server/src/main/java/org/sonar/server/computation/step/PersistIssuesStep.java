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
package org.sonar.server.computation.step;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.db.*;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.util.CloseableIterator;

public class PersistIssuesStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final UpdateConflictResolver conflictResolver;
  private final RuleCache ruleCache;
  private final IssueCache issueCache;

  public PersistIssuesStep(DbClient dbClient, System2 system2, UpdateConflictResolver conflictResolver,
    RuleCache ruleCache, IssueCache issueCache) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.conflictResolver = conflictResolver;
    this.ruleCache = ruleCache;
    this.issueCache = issueCache;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(true);
    IssueMapper mapper = session.getMapper(IssueMapper.class);
    IssueChangeMapper changeMapper = session.getMapper(IssueChangeMapper.class);

    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    try {
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        boolean saved = false;
        if (issue.isNew()) {
          Integer ruleId = ruleCache.get(issue.ruleKey()).getId();
          mapper.insert(IssueDto.toDtoForComputationInsert(issue, ruleId, system2.now()));
          saved = true;
        } else if (issue.isChanged()) {
          IssueDto dto = IssueDto.toDtoForUpdate(issue, system2.now());
          if (Issue.STATUS_CLOSED.equals(issue.status()) || issue.selectedAt() == null) {
            // Issue is closed by scan or changed by end-user
            mapper.update(dto);
          } else {
            int updateCount = mapper.updateIfBeforeSelectedDate(dto);
            if (updateCount == 0) {
              // End-user and scan changed the issue at the same time.
              // See https://jira.codehaus.org/browse/SONAR-4309
              conflictResolver.resolve(issue, mapper);
            }
          }
          saved = true;
        }
        if (saved) {
          insertChanges(changeMapper, issue);
        }
      }
      session.flushStatements();
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
      issues.close();
    }
  }

  private void insertChanges(IssueChangeMapper mapper, DefaultIssue issue) {
    for (IssueComment comment : issue.comments()) {
      DefaultIssueComment c = (DefaultIssueComment) comment;
      if (c.isNew()) {
        IssueChangeDto changeDto = IssueChangeDto.of(c);
        mapper.insert(changeDto);
      }
    }
    FieldDiffs diffs = issue.currentChange();
    if (!issue.isNew() && diffs != null) {
      IssueChangeDto changeDto = IssueChangeDto.of(issue.key(), diffs);
      mapper.insert(changeDto);
    }
  }

  @Override
  public String getDescription() {
    return "Persist issues";
  }
}
