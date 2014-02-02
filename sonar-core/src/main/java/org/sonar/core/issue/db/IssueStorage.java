/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.persistence.BatchSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

  private final MyBatis mybatis;
  private final RuleFinder ruleFinder;
  private final UpdateConflictResolver conflictResolver = new UpdateConflictResolver();

  protected IssueStorage(MyBatis mybatis, RuleFinder ruleFinder) {
    this.mybatis = mybatis;
    this.ruleFinder = ruleFinder;
  }

  public void save(DefaultIssue issue) {
    save(Arrays.asList(issue));
  }

  public void save(Iterable<DefaultIssue> issues) {
    // Batch session can not be used for updates. It does not return the number of updated rows,
    // required for detecting conflicts.
    Date now = new Date();
    List<DefaultIssue> toBeUpdated = batchInsert(issues, now);
    update(toBeUpdated, now);
  }

  private List<DefaultIssue> batchInsert(Iterable<DefaultIssue> issues, Date now) {
    List<DefaultIssue> toBeUpdated = Lists.newArrayList();
    SqlSession batchSession = mybatis.openBatchSession();
    int count = 0;
    IssueMapper issueMapper = batchSession.getMapper(IssueMapper.class);
    IssueChangeMapper issueChangeMapper = batchSession.getMapper(IssueChangeMapper.class);
    try {
      for (DefaultIssue issue : issues) {
        if (issue.isNew()) {
          insert(issueMapper, now, issue);
          insertChanges(issueChangeMapper, issue);
          if (count > BatchSession.MAX_BATCH_SIZE) {
            batchSession.commit();
          }
          count++;
        } else if (issue.isChanged()) {
          toBeUpdated.add(issue);
        }
      }
      batchSession.commit();
    } finally {
      MyBatis.closeQuietly(batchSession);
    }
    return toBeUpdated;
  }

  private void insert(IssueMapper issueMapper, Date now, DefaultIssue issue) {
    long componentId = componentId(issue);
    long projectId = projectId(issue);
    int ruleId = ruleId(issue);
    IssueDto dto = IssueDto.toDtoForInsert(issue, componentId, projectId, ruleId, now);
    issueMapper.insert(dto);
  }

  private void update(List<DefaultIssue> toBeUpdated, Date now) {
    if (!toBeUpdated.isEmpty()) {
      SqlSession session = mybatis.openSession();
      try {
        IssueMapper issueMapper = session.getMapper(IssueMapper.class);
        IssueChangeMapper issueChangeMapper = session.getMapper(IssueChangeMapper.class);
        for (DefaultIssue issue : toBeUpdated) {
          update(issueMapper, now, issue);
          insertChanges(issueChangeMapper, issue);
        }
        session.commit();
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  private void update(IssueMapper issueMapper, Date now, DefaultIssue issue) {
    IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
    if (Issue.STATUS_CLOSED.equals(issue.status()) || issue.selectedAt() == null) {
      // Issue is closed by scan or changed by end-user
      issueMapper.update(dto);

    } else {
      int count = issueMapper.updateIfBeforeSelectedDate(dto);
      if (count == 0) {
        // End-user and scan changed the issue at the same time.
        // See https://jira.codehaus.org/browse/SONAR-4309
        conflictResolver.resolve(issue, issueMapper);
      }
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

  protected abstract long componentId(DefaultIssue issue);

  protected abstract long projectId(DefaultIssue issue);

  private int ruleId(Issue issue) {
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    if (rule == null) {
      throw new IllegalStateException("Rule not found: " + issue.ruleKey());
    }
    return rule.getId();
  }
}
