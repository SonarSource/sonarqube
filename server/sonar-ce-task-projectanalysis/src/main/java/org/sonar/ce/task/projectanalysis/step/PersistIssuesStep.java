/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.issue.IssueCache;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.ce.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.server.issue.IssueStorage;

public class PersistIssuesStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final UpdateConflictResolver conflictResolver;
  private final RuleRepository ruleRepository;
  private final IssueCache issueCache;
  private final IssueStorage issueStorage;

  public PersistIssuesStep(DbClient dbClient, System2 system2, UpdateConflictResolver conflictResolver,
    RuleRepository ruleRepository, IssueCache issueCache, IssueStorage issueStorage) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.conflictResolver = conflictResolver;
    this.ruleRepository = ruleRepository;
    this.issueCache = issueCache;
    this.issueStorage = issueStorage;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    IssueStatistics statistics = new IssueStatistics();
    try (DbSession dbSession = dbClient.openSession(true);
      CloseableIterator<DefaultIssue> issues = issueCache.traverse()) {

      IssueMapper mapper = dbSession.getMapper(IssueMapper.class);
      IssueChangeMapper changeMapper = dbSession.getMapper(IssueChangeMapper.class);
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        boolean saved = persistIssueIfRequired(mapper, issue, statistics);
        if (saved) {
          issueStorage.insertChanges(changeMapper, issue);
        }
      }
      dbSession.flushStatements();
      dbSession.commit();
    } finally {
      statistics.dumpTo(context);
    }
  }

  private boolean persistIssueIfRequired(IssueMapper mapper, DefaultIssue issue, IssueStatistics issueStatistics) {
    if (issue.isNew() || issue.isCopied()) {
      persistNewIssue(mapper, issue);
      issueStatistics.inserts++;
      return true;
    }

    if (issue.isChanged()) {
      persistChangedIssue(mapper, issue);
      issueStatistics.updates++;
      return true;
    }

    issueStatistics.untouched++;
    return false;
  }

  private void persistNewIssue(IssueMapper mapper, DefaultIssue issue) {
    Integer ruleId = ruleRepository.getByKey(issue.ruleKey()).getId();
    IssueDto dto = IssueDto.toDtoForComputationInsert(issue, ruleId, system2.now());
    mapper.insert(dto);
  }

  private void persistChangedIssue(IssueMapper mapper, DefaultIssue issue) {
    IssueDto dto = IssueDto.toDtoForUpdate(issue, system2.now());
    int updateCount = mapper.updateIfBeforeSelectedDate(dto);
    if (updateCount == 0) {
      // End-user and scan changed the issue at the same time.
      // See https://jira.sonarsource.com/browse/SONAR-4309
      conflictResolver.resolve(issue, mapper);
    }
  }

  @Override
  public String getDescription() {
    return "Persist issues";
  }

  private static class IssueStatistics {
    private int inserts = 0;
    private int updates = 0;
    private int untouched = 0;

    private void dumpTo(ComputationStep.Context context) {
      context.getStatistics()
        .add("inserts", String.valueOf(inserts))
        .add("updates", String.valueOf(updates))
        .add("untouched", String.valueOf(untouched));
    }
  }
}
