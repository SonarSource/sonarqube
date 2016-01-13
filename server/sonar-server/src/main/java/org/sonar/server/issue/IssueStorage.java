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
package org.sonar.server.issue;

import java.util.List;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.BatchSession;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueChangeMapper;

import static com.google.common.collect.Lists.newArrayList;

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

  private final RuleFinder ruleFinder;
  private final DbClient dbClient;

  protected IssueStorage(DbClient dbClient, RuleFinder ruleFinder) {
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
    DbSession session = dbClient.openSession(true);
    try {
      doSave(session, issues);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void doSave(DbSession session, Iterable<DefaultIssue> issues) {
    // Batch session can not be used for updates. It does not return the number of updated rows,
    // required for detecting conflicts.
    long now = System.currentTimeMillis();
    List<DefaultIssue> toBeUpdated = batchInsertAndReturnIssuesToUpdate(session, issues, now);
    update(toBeUpdated, now);
    doAfterSave();
  }

  protected void doAfterSave() {
    // overridden on server-side to index ES
  }

  private List<DefaultIssue> batchInsertAndReturnIssuesToUpdate(DbSession session, Iterable<DefaultIssue> issues, long now) {
    List<DefaultIssue> toBeUpdated = newArrayList();
    int count = 0;
    IssueChangeMapper issueChangeMapper = session.getMapper(IssueChangeMapper.class);
    for (DefaultIssue issue : issues) {
      if (issue.isNew()) {
        doInsert(session, now, issue);
        insertChanges(issueChangeMapper, issue);
        if (count > BatchSession.MAX_BATCH_SIZE) {
          session.commit();
        }
        count++;
      } else if (issue.isChanged()) {
        toBeUpdated.add(issue);
      }
    }
    session.commit();
    return toBeUpdated;
  }

  protected abstract void doInsert(DbSession batchSession, long now, DefaultIssue issue);

  private void update(List<DefaultIssue> toBeUpdated, long now) {
    if (!toBeUpdated.isEmpty()) {
      DbSession session = dbClient.openSession(false);
      try {
        IssueChangeMapper issueChangeMapper = session.getMapper(IssueChangeMapper.class);
        for (DefaultIssue issue : toBeUpdated) {
          doUpdate(session, now, issue);
          insertChanges(issueChangeMapper, issue);
        }
        session.commit();
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  protected abstract void doUpdate(DbSession batchSession, long now, DefaultIssue issue);

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
