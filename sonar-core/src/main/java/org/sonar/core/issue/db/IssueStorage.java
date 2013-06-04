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
import org.sonar.core.persistence.MyBatis;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public abstract class IssueStorage {

  private final MyBatis mybatis;
  private final RuleFinder ruleFinder;

  protected IssueStorage(MyBatis mybatis, RuleFinder ruleFinder) {
    this.mybatis = mybatis;
    this.ruleFinder = ruleFinder;
  }

  public void save(DefaultIssue issue) {
    save(Arrays.asList(issue));
  }

  public void save(Iterable<DefaultIssue> issues) {
    SqlSession session = mybatis.openBatchSession();
    IssueMapper issueMapper = session.getMapper(IssueMapper.class);
    IssueChangeMapper issueChangeMapper = session.getMapper(IssueChangeMapper.class);
    Date now = new Date();
    try {
      List<DefaultIssue> conflicts = Lists.newArrayList();
      for (DefaultIssue issue : issues) {
        if (issue.isNew()) {
          long componentId = componentId(issue);
          long projectId = projectId(issue);
          int ruleId = ruleId(issue);
          IssueDto dto = IssueDto.toDtoForInsert(issue, componentId, projectId, ruleId, now);
          issueMapper.insert(dto);

        } else if (issue.isChanged()) {
          IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
          int count = issueMapper.update(dto);
          if (count < 1) {
            conflicts.add(issue);
          }
        }
        insertChanges(issueChangeMapper, issue);

      }
      session.commit();
      // TODO log and fix conflicts
    } finally {
      MyBatis.closeQuietly(session);
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
    if (diffs != null) {
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
