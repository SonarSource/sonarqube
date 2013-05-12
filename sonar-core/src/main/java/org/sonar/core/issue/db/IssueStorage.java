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
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.persistence.MyBatis;

import java.util.Arrays;
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
    try {
      List<DefaultIssue> conflicts = Lists.newArrayList();

      for (DefaultIssue issue : issues) {
        int ruleId = ruleId(issue);
        int componentId = componentId(issue);

        IssueDto dto = IssueDto.toDto(issue, componentId, ruleId);
        if (issue.isNew()) {
          issueMapper.insert(dto);
        } else /* TODO if hasChanges */ {
          // TODO manage condition on update date
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
        IssueChangeDto changeDto = ChangeDtoConverter.commentToDto(c);
        mapper.insert(changeDto);
      }
    }
    if (issue.diffs() != null) {
      IssueChangeDto changeDto = ChangeDtoConverter.changeToDto(issue.key(), issue.diffs());
      mapper.insert(changeDto);
    }
  }

  protected abstract int componentId(DefaultIssue issue);

  private int ruleId(Issue issue) {
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    if (rule == null) {
      throw new IllegalStateException("Rule not found: " + issue.ruleKey());
    }
    return rule.getId();
  }
}
