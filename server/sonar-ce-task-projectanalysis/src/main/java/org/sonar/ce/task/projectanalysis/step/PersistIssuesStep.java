/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.ce.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.BatchSession;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.AnticipatedTransitionMapper;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.NewCodeReferenceIssueDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.issue.IssueStorage;

import static org.sonar.core.util.FileUtils.humanReadableByteCountSI;
import static org.sonar.db.issue.IssueDto.toDtoForComputationInsert;

public class PersistIssuesStep implements ComputationStep {
  // holding up to 1000 DefaultIssue (max size of addedIssues and updatedIssues at any given time) in memory should not
  // be a problem while making sure we leverage extensively the batch feature to speed up persistence
  private static final int ISSUE_BATCHING_SIZE = BatchSession.MAX_BATCH_SIZE * 2;

  private final DbClient dbClient;
  private final System2 system2;
  private final UpdateConflictResolver conflictResolver;
  private final RuleRepository ruleRepository;
  private final PeriodHolder periodHolder;
  private final ProtoIssueCache protoIssueCache;
  private final IssueStorage issueStorage;
  private final UuidFactory uuidFactory;

  public PersistIssuesStep(DbClient dbClient, System2 system2, UpdateConflictResolver conflictResolver,
    RuleRepository ruleRepository, PeriodHolder periodHolder, ProtoIssueCache protoIssueCache, IssueStorage issueStorage,
    UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.conflictResolver = conflictResolver;
    this.ruleRepository = ruleRepository;
    this.periodHolder = periodHolder;
    this.protoIssueCache = protoIssueCache;
    this.issueStorage = issueStorage;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    context.getStatistics().add("cacheSize", humanReadableByteCountSI(protoIssueCache.fileSize()));
    IssueStatistics statistics = new IssueStatistics();
    try (DbSession dbSession = dbClient.openSession(true);
      CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse()) {
      List<DefaultIssue> addedIssues = new ArrayList<>(ISSUE_BATCHING_SIZE);
      List<DefaultIssue> updatedIssues = new ArrayList<>(ISSUE_BATCHING_SIZE);
      List<DefaultIssue> noLongerNewIssues = new ArrayList<>(ISSUE_BATCHING_SIZE);
      List<DefaultIssue> newCodeIssuesToMigrate = new ArrayList<>(ISSUE_BATCHING_SIZE);
      IssueDao issueDao = dbClient.issueDao();
      IssueChangeMapper changeMapper = dbSession.getMapper(IssueChangeMapper.class);
      AnticipatedTransitionMapper anticipatedTransitionMapper = dbSession.getMapper(AnticipatedTransitionMapper.class);
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        if (issue.isNew() || issue.isCopied()) {
          addedIssues.add(issue);
          if (addedIssues.size() >= ISSUE_BATCHING_SIZE) {
            persistNewIssues(statistics, addedIssues, issueDao, changeMapper, anticipatedTransitionMapper, dbSession);
            addedIssues.clear();
          }
        } else if (issue.isChanged()) {
          updatedIssues.add(issue);
          if (updatedIssues.size() >= ISSUE_BATCHING_SIZE) {
            persistUpdatedIssues(statistics, updatedIssues, issueDao, changeMapper, dbSession);
            updatedIssues.clear();
          }
        } else if (isOnBranchUsingReferenceBranch() && issue.isNoLongerNewCodeReferenceIssue()) {
          noLongerNewIssues.add(issue);
          if (noLongerNewIssues.size() >= ISSUE_BATCHING_SIZE) {
            persistNoLongerNewIssues(statistics, noLongerNewIssues, issueDao, dbSession);
            noLongerNewIssues.clear();
          }
        } else if (isOnBranchUsingReferenceBranch() && issue.isToBeMigratedAsNewCodeReferenceIssue()) {
          newCodeIssuesToMigrate.add(issue);
          if (newCodeIssuesToMigrate.size() >= ISSUE_BATCHING_SIZE) {
            persistNewCodeIssuesToMigrate(statistics, newCodeIssuesToMigrate, issueDao, dbSession);
            newCodeIssuesToMigrate.clear();
          }
        }
      }

      persistNewIssues(statistics, addedIssues, issueDao, changeMapper, anticipatedTransitionMapper, dbSession);
      persistUpdatedIssues(statistics, updatedIssues, issueDao, changeMapper, dbSession);
      persistNoLongerNewIssues(statistics, noLongerNewIssues, issueDao, dbSession);
      persistNewCodeIssuesToMigrate(statistics, newCodeIssuesToMigrate, issueDao, dbSession);
      flushSession(dbSession);
    } finally {
      statistics.dumpTo(context);
    }
  }

  private void persistNewIssues(IssueStatistics statistics, List<DefaultIssue> addedIssues,
    IssueDao issueDao, IssueChangeMapper changeMapper, AnticipatedTransitionMapper anticipatedTransitionMapper, DbSession dbSession) {

    final long now = system2.now();

    List<IssueDto> issueDtos = new LinkedList<>();
    addedIssues.forEach(addedIssue -> {
      String ruleUuid = ruleRepository.getByKey(addedIssue.ruleKey()).getUuid();
      IssueDto dto = toDtoForComputationInsert(addedIssue, ruleUuid, now);
      issueDao.insertWithoutImpacts(dbSession, dto);
      issueDtos.add(dto);
      if (isOnBranchUsingReferenceBranch() && addedIssue.isOnChangedLine()) {
        issueDao.insertAsNewCodeOnReferenceBranch(dbSession, NewCodeReferenceIssueDto.fromIssueDto(dto, now, uuidFactory));
      }
      statistics.inserts++;
      issueStorage.insertChanges(changeMapper, addedIssue, uuidFactory);
      addedIssue.getAnticipatedTransitionUuid().ifPresent(anticipatedTransitionMapper::delete);
    });

    issueDtos.forEach(issueDto -> insertAdditionalIssueData(issueDao, dbSession, issueDto));
  }

  private void insertAdditionalIssueData(IssueDao issueDao, DbSession dbSession, IssueDto issueDto) {
    issueDao.insertIssueImpacts(dbSession, issueDto);
  }

  private void persistUpdatedIssues(IssueStatistics statistics, List<DefaultIssue> updatedIssues, IssueDao issueDao,
    IssueChangeMapper changeMapper, DbSession dbSession) {
    if (updatedIssues.isEmpty()) {
      return;
    }

    long now = system2.now();
    LinkedList<IssueDto> updatedIssueDtos = new LinkedList<>();
    updatedIssues.forEach(i -> {
      IssueDto dto = IssueDto.toDtoForUpdate(i, now);
      boolean isUpdated = issueDao.updateIfBeforeSelectedDate(dbSession, dto);
      if (isUpdated) {
        updatedIssueDtos.add(dto);
      }
      statistics.updates++;
    });
    updatedIssueDtos.forEach(i -> issueDao.deleteIssueImpacts(dbSession, i));
    updatedIssueDtos.forEach(i -> issueDao.insertIssueImpacts(dbSession, i));

    // retrieve those of the updatedIssues which have not been updated and apply conflictResolver on them
    List<String> updatedIssueKeys = updatedIssues.stream().map(DefaultIssue::key).toList();
    List<IssueDto> conflictIssueKeys = issueDao.selectByKeysIfNotUpdatedAt(dbSession, updatedIssueKeys, now);
    if (!conflictIssueKeys.isEmpty()) {
      updatedIssueDtos.clear();
      Map<String, DefaultIssue> issuesByKeys = updatedIssues.stream().collect(Collectors.toMap(DefaultIssue::key, Function.identity()));
      conflictIssueKeys
        .forEach(dbIssue -> {
          DefaultIssue updatedIssue = issuesByKeys.get(dbIssue.getKey());
          IssueDto issueToBeUpdated = conflictResolver.resolve(updatedIssue, dbIssue);
          issueDao.updateWithoutIssueImpacts(dbSession, issueToBeUpdated);
          updatedIssueDtos.add(issueToBeUpdated);
          statistics.merged++;
        });

      updatedIssueDtos.forEach(i -> issueDao.deleteIssueImpacts(dbSession, i));
      updatedIssueDtos.forEach(i -> issueDao.insertIssueImpacts(dbSession, i));
    }

    updatedIssues.forEach(i -> issueStorage.insertChanges(changeMapper, i, uuidFactory));
  }

  private static void persistNoLongerNewIssues(IssueStatistics statistics, List<DefaultIssue> noLongerNewIssues, IssueDao issueDao, DbSession dbSession) {
    if (noLongerNewIssues.isEmpty()) {
      return;
    }

    noLongerNewIssues.forEach(i -> {
      issueDao.deleteAsNewCodeOnReferenceBranch(dbSession, i.key());
      statistics.updates++;
    });

  }

  private void persistNewCodeIssuesToMigrate(IssueStatistics statistics, List<DefaultIssue> newCodeIssuesToMigrate, IssueDao issueDao, DbSession dbSession) {
    if (newCodeIssuesToMigrate.isEmpty()) {
      return;
    }

    long now = system2.now();
    newCodeIssuesToMigrate.forEach(i -> {
      issueDao.insertAsNewCodeOnReferenceBranch(dbSession, NewCodeReferenceIssueDto.fromIssueKey(i.key(), now, uuidFactory));
      statistics.updates++;
    });
  }

  private static void flushSession(DbSession dbSession) {
    dbSession.flushStatements();
    dbSession.commit();
  }

  private boolean isOnBranchUsingReferenceBranch() {
    if (periodHolder.hasPeriod()) {
      return periodHolder.getPeriod().getMode().equals(NewCodePeriodType.REFERENCE_BRANCH.name());
    }
    return false;
  }

  @Override
  public String getDescription() {
    return "Persist issues";
  }

  private static class IssueStatistics {
    private int inserts = 0;
    private int updates = 0;
    private int merged = 0;

    private void dumpTo(ComputationStep.Context context) {
      context.getStatistics()
        .add("inserts", String.valueOf(inserts))
        .add("updates", String.valueOf(updates))
        .add("merged", String.valueOf(merged));
    }
  }
}
