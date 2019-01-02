/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.issue.IssueCache;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.ce.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.BatchSession;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.server.issue.IssueStorage;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class PersistIssuesStep implements ComputationStep {
  // holding up to 1000 DefaultIssue (max size of addedIssues and updatedIssues at any given time) in memory should not
  // be a problem while making sure we leverage extensively the batch feature to speed up persistence
  private static final int ISSUE_BATCHING_SIZE = BatchSession.MAX_BATCH_SIZE * 2;

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
      List<DefaultIssue> addedIssues = new ArrayList<>(ISSUE_BATCHING_SIZE);
      List<DefaultIssue> updatedIssues = new ArrayList<>(ISSUE_BATCHING_SIZE);

      IssueMapper mapper = dbSession.getMapper(IssueMapper.class);
      IssueChangeMapper changeMapper = dbSession.getMapper(IssueChangeMapper.class);
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        if (issue.isNew() || issue.isCopied()) {
          addedIssues.add(issue);
          if (addedIssues.size() >= ISSUE_BATCHING_SIZE) {
            persistNewIssues(statistics, addedIssues, mapper, changeMapper);
            addedIssues.clear();
          }
        } else if (issue.isChanged()) {
          updatedIssues.add(issue);
          if (updatedIssues.size() >= ISSUE_BATCHING_SIZE) {
            persistUpdatedIssues(statistics, updatedIssues, mapper, changeMapper);
            updatedIssues.clear();
          }
        } else {
          statistics.untouched++;
        }
      }
      persistNewIssues(statistics, addedIssues, mapper, changeMapper);
      persistUpdatedIssues(statistics, updatedIssues, mapper, changeMapper);
      flushSession(dbSession);
    } finally {
      statistics.dumpTo(context);
    }
  }

  private void persistNewIssues(IssueStatistics statistics, List<DefaultIssue> addedIssues, IssueMapper mapper, IssueChangeMapper changeMapper) {
    if (addedIssues.isEmpty()) {
      return;
    }

    long now = system2.now();
    addedIssues.forEach(i -> {
      int ruleId = ruleRepository.getByKey(i.ruleKey()).getId();
      IssueDto dto = IssueDto.toDtoForComputationInsert(i, ruleId, now);
      mapper.insert(dto);
      statistics.inserts++;
    });

    addedIssues.forEach(i -> issueStorage.insertChanges(changeMapper, i));
  }

  private void persistUpdatedIssues(IssueStatistics statistics, List<DefaultIssue> updatedIssues, IssueMapper mapper, IssueChangeMapper changeMapper) {
    if (updatedIssues.isEmpty()) {
      return;
    }

    long now = system2.now();
    updatedIssues.forEach(i -> {
      IssueDto dto = IssueDto.toDtoForUpdate(i, now);
      mapper.updateIfBeforeSelectedDate(dto);
      statistics.updates++;
    });

    // retrieve those of the updatedIssues which have not been updated and apply conflictResolver on them
    List<String> updatedIssueKeys = updatedIssues.stream().map(DefaultIssue::key).collect(toList(updatedIssues.size()));
    List<IssueDto> conflictIssueKeys = mapper.selectByKeysIfNotUpdatedAt(updatedIssueKeys, now);
    if (!conflictIssueKeys.isEmpty()) {
      Map<String, DefaultIssue> issuesByKeys = updatedIssues.stream().collect(uniqueIndex(DefaultIssue::key, updatedIssues.size()));
      conflictIssueKeys
        .forEach(dbIssue -> {
          DefaultIssue updatedIssue = issuesByKeys.get(dbIssue.getKey());
          conflictResolver.resolve(updatedIssue, dbIssue, mapper);
          statistics.merged++;
        });
    }

    updatedIssues.forEach(i -> issueStorage.insertChanges(changeMapper, i));
  }

  private static void flushSession(DbSession dbSession) {
    dbSession.flushStatements();
    dbSession.commit();
  }

  @Override
  public String getDescription() {
    return "Persist issues";
  }

  private static class IssueStatistics {
    private int inserts = 0;
    private int updates = 0;
    private int merged = 0;
    private int untouched = 0;

    private void dumpTo(ComputationStep.Context context) {
      context.getStatistics()
        .add("inserts", String.valueOf(inserts))
        .add("updates", String.valueOf(updates))
        .add("merged", String.valueOf(merged))
        .add("untouched", String.valueOf(untouched));
    }
  }
}
