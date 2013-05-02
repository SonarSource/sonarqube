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
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.persistence.MyBatis;

import java.util.Arrays;
import java.util.List;

public abstract class IssueStorage {

  private static final String MYBATIS_INSERT_CHANGE = "org.sonar.core.issue.db.IssueChangeMapper.insert";
  private static final String MYBATIS_INSERT_ISSUE = "org.sonar.core.issue.db.IssueMapper.insert";
  private static final String MYBATIS_UPDATE_ISSUE = "org.sonar.core.issue.db.IssueMapper.update";

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
    SqlSession session = mybatis.openSession();
    try {
      List<DefaultIssue> conflicts = Lists.newArrayList();
      for (DefaultIssue issue : issues) {
        int ruleId = ruleId(issue);
        int componentId = componentId(issue);

        IssueDto dto = IssueDto.toDto(issue, componentId, ruleId);
        // TODO set technical created/updated dates
        if (issue.isNew()) {
          session.insert(MYBATIS_INSERT_ISSUE, dto);
        } else /* TODO if hasChanges */ {
          // TODO manage condition on update date
          int count = session.update(MYBATIS_UPDATE_ISSUE, dto);
          if (count < 1) {
            conflicts.add(issue);
          }
        }
        for (IssueChangeDto changeDto : ChangeDtoConverter.toChangeDtos(issue)) {
          session.insert(MYBATIS_INSERT_CHANGE, changeDto);
        }
      }
      session.commit();
      // TODO log and fix conflicts
    } finally {
      MyBatis.closeQuietly(session);
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
