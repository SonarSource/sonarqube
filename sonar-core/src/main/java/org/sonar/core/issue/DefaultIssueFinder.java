/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.issue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.AuthorizationDao;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @since 3.6
 */
public class DefaultIssueFinder implements IssueFinder {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultIssueFinder.class);

  /**
   * The role required to access issues
   */
  private static final String ROLE = "user";

  private final MyBatis myBatis;
  private final IssueDao issueDao;
  private final AuthorizationDao authorizationDao;


  public DefaultIssueFinder(MyBatis myBatis, IssueDao issueDao, AuthorizationDao authorizationDao) {
    this.myBatis = myBatis;
    this.issueDao = issueDao;
    this.authorizationDao = authorizationDao;
  }

  public Results find(IssueQuery query, @Nullable Integer currentUserId) {
    LOG.debug("IssueQuery : {}", query);
    SqlSession sqlSession = myBatis.openSession();
    try {
      List<IssueDto> dtos = issueDao.select(query, sqlSession);

      Set<Integer> componentIds = Sets.newLinkedHashSet();
      for (IssueDto issueDto : dtos) {
        componentIds.add(issueDto.getResourceId());
      }
      Set<Integer> authorizedComponentIds = authorizationDao.keepAuthorizedComponentIds(componentIds, currentUserId, ROLE, sqlSession);
      List<Issue> issues = Lists.newArrayList();
      for (IssueDto dto : dtos) {
        if (authorizedComponentIds.contains(dto.getResourceId())) {
          issues.add(toIssue(dto));
        }
      }
      return new DefaultResults(issues);
    } finally {
      MyBatis.closeQuietly(sqlSession);
    }
  }

  public Issue findByKey(String key) {
    IssueDto dto = issueDao.selectByKey(key);
    return dto != null ? toIssue(dto) : null;
  }

  private Issue toIssue(IssueDto dto) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(dto.getUuid());
    issue.setStatus(dto.getStatus());
    issue.setResolution(dto.getResolution());
    issue.setMessage(dto.getMessage());
    issue.setTitle(dto.getTitle());
    issue.setCost(dto.getCost());
    issue.setLine(dto.getLine());
    issue.setSeverity(dto.getSeverity());
    issue.setUserLogin(dto.getUserLogin());
    issue.setAssigneeLogin(dto.getAssigneeLogin());
    issue.setCreatedAt(dto.getCreatedAt());
    issue.setUpdatedAt(dto.getUpdatedAt());
    issue.setClosedAt(dto.getClosedAt());
    issue.setAttributes(KeyValueFormat.parse(dto.getData()));
    issue.setManual(dto.isManualIssue());
    issue.setManualSeverity(dto.isManualSeverity());
    issue.setComponentKey(dto.getComponentKey());
    issue.setRuleKey(RuleKey.of(dto.getRuleRepo(), dto.getRule()));
    return issue;
  }

  static class DefaultResults implements Results {
    private final List<Issue> issues;

    DefaultResults(List<Issue> issues) {
      this.issues = issues;
    }

    @Override
    public List<Issue> issues() {
      return issues;
    }
  }
}
