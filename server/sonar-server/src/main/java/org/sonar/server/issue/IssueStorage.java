/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.BatchSession;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueChangeMapper;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

/**
 * Save issues into database. It is executed :
 * <ul>
 * <li>once at the end of scan, even on multi-module projects</li>
 * <li>on each server-side action initiated by UI or web service</li>
 * </ul>
 *
 * @since 3.6
 */
public abstract class IssueStorage {

  private final System2 system2;
  private final RuleFinder ruleFinder;
  private final DbClient dbClient;

  protected IssueStorage(System2 system2, DbClient dbClient, RuleFinder ruleFinder) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.ruleFinder = ruleFinder;
  }

  protected DbClient getDbClient() {
    return dbClient;
  }

  public void save(DefaultIssue issue) {
    save(newArrayList(issue));
  }

  public void save(DbSession session, DefaultIssue issue) {
    doSave(session, newArrayList(issue));
  }

  public void save(Iterable<DefaultIssue> issues) {
    try (DbSession session = dbClient.openSession(true)) {
      doSave(session, issues);
    }
  }

  private void doSave(DbSession session, Iterable<DefaultIssue> issues) {
    // Batch session can not be used for updates. It does not return the number of updated rows,
    // required for detecting conflicts.
    long now = system2.now();

    Map<Boolean, List<DefaultIssue>> issuesNewOrUpdated = StreamSupport.stream(issues.spliterator(), true).collect(Collectors.groupingBy(DefaultIssue::isNew));
    List<DefaultIssue> issuesToInsert = firstNonNull(issuesNewOrUpdated.get(true), emptyList());
    List<DefaultIssue> issuesToUpdate = firstNonNull(issuesNewOrUpdated.get(false), emptyList());

    Collection<String> inserted = insert(session, issuesToInsert, now);
    Collection<String> updated = update(issuesToUpdate, now);

    Collection<String> issuesInsertedOrUpdated = new ArrayList<>(issuesToInsert.size() + issuesToUpdate.size());
    issuesInsertedOrUpdated.addAll(inserted);
    issuesInsertedOrUpdated.addAll(updated);
    doAfterSave(issuesInsertedOrUpdated);
  }

  protected void doAfterSave(Collection<String> issues) {
    // overridden on server-side to index ES
  }

  /**
   * @return the keys of the inserted issues
   */
  private Collection<String> insert(DbSession session, Iterable<DefaultIssue> issuesToInsert, long now) {
    List<String> inserted = newArrayList();
    int count = 0;
    IssueChangeMapper issueChangeMapper = session.getMapper(IssueChangeMapper.class);
    for (DefaultIssue issue : issuesToInsert) {
      String key = doInsert(session, now, issue);
      inserted.add(key);
      insertChanges(issueChangeMapper, issue);
      if (count > BatchSession.MAX_BATCH_SIZE) {
        session.commit();
      }
      count++;
    }
    session.commit();
    return inserted;
  }

  protected abstract String doInsert(DbSession batchSession, long now, DefaultIssue issue);

  /**
   * @return the keys of the updated issues
   */
  private Collection<String> update(List<DefaultIssue> issuesToUpdate, long now) {
    Collection<String> updated = new ArrayList<>();
    if (!issuesToUpdate.isEmpty()) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        IssueChangeMapper issueChangeMapper = dbSession.getMapper(IssueChangeMapper.class);
        for (DefaultIssue issue : issuesToUpdate) {
          String key = doUpdate(dbSession, now, issue);
          updated.add(key);
          insertChanges(issueChangeMapper, issue);
        }
        dbSession.commit();
      }
    }
    return updated;
  }

  protected abstract String doUpdate(DbSession batchSession, long now, DefaultIssue issue);

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

  protected Rule rule(Issue issue) {
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    if (rule == null) {
      throw new IllegalStateException("Rule not found: " + issue.ruleKey());
    }
    return rule;
  }
}
