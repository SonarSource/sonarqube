/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.sonar.api.issue.IssueComment;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.server.computation.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCache;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepository;
import org.sonar.db.DbClient;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.task.step.ComputationStep;

public class PersistIssuesStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final UpdateConflictResolver conflictResolver;
  private final RuleRepository ruleRepository;
  private final IssueCache issueCache;

  public PersistIssuesStep(DbClient dbClient, System2 system2, UpdateConflictResolver conflictResolver,
    RuleRepository ruleRepository, IssueCache issueCache) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.conflictResolver = conflictResolver;
    this.ruleRepository = ruleRepository;
    this.issueCache = issueCache;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    IssueMapper mapper = session.getMapper(IssueMapper.class);
    IssueChangeMapper changeMapper = session.getMapper(IssueChangeMapper.class);

    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    try {
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        boolean saved = false;
        if (issue.isNew()) {
          Integer ruleId = ruleRepository.getByKey(issue.ruleKey()).getId();
          IssueDto dto = IssueDto.toDtoForComputationInsert(issue, ruleId, system2.now());
          mapper.insert(dto);
          saved = true;
        } else if (issue.isChanged()) {
          IssueDto dto = IssueDto.toDtoForUpdate(issue, system2.now());
          int updateCount = mapper.updateIfBeforeSelectedDate(dto);
          if (updateCount == 0) {
            // End-user and scan changed the issue at the same time.
            // See https://jira.sonarsource.com/browse/SONAR-4309
            conflictResolver.resolve(issue, mapper);
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

  private static void insertChanges(IssueChangeMapper mapper, DefaultIssue issue) {
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
