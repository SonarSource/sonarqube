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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.AuthorizationDao;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
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
  private final ResourceDao resourceDao;
  private final AuthorizationDao authorizationDao;
  private final RuleFinder ruleFinder;

  public DefaultIssueFinder(MyBatis myBatis, IssueDao issueDao, ResourceDao resourceDao,
                            AuthorizationDao authorizationDao, RuleFinder ruleFinder) {
    this.myBatis = myBatis;
    this.issueDao = issueDao;
    this.resourceDao = resourceDao;
    this.authorizationDao = authorizationDao;
    this.ruleFinder = ruleFinder;
  }

  public Results find(IssueQuery query, @Nullable Integer currentUserId) {
    LOG.debug("IssueQuery : {}", query);
    SqlSession sqlSession = myBatis.openSession();
    try {
      List<IssueDto> issueDtos = issueDao.select(query, sqlSession);

      Set<Integer> componentIds = Sets.newLinkedHashSet();
      Set<Integer> ruleIds = Sets.newLinkedHashSet();
      for (IssueDto issueDto : issueDtos) {
        componentIds.add(issueDto.getResourceId());
        ruleIds.add(issueDto.getRuleId());
      }

      componentIds = authorizationDao.keepAuthorizedComponentIds(componentIds, currentUserId, ROLE, sqlSession);

      final Map<Integer, Rule> rules = Maps.newHashMap();
      for (Integer ruleId : ruleIds) {
        Rule rule = ruleFinder.findById(ruleId);
        if (rule != null) {
          rules.put(rule.getId(), rule);
        }
      }
      final Map<Integer, ResourceDto> resources = Maps.newHashMap();
      for (Integer componentId : componentIds) {
        // TODO replace N+1 SQL requests by a single one
        ResourceDto resource = resourceDao.getResource(componentId);
        if (resource != null) {
          resources.put(resource.getId().intValue(), resource);
        }
      }

      List<Issue> issues = ImmutableList.copyOf(Iterables.transform(issueDtos, new Function<IssueDto, Issue>() {
        @Override
        public Issue apply(IssueDto dto) {
          Rule rule = rules.get(dto.getRuleId());
          ResourceDto resource = resources.get(dto.getResourceId());
          return toIssue(dto, rule, resource);
        }
      }));

      return new DefaultResults(issues);
    } finally {
      MyBatis.closeQuietly(sqlSession);
    }
  }

  public Issue findByKey(String key) {
    IssueDto dto = issueDao.selectByKey(key);
    Issue issue = null;
    if (dto != null) {
      Rule rule = ruleFinder.findById(dto.getRuleId());
      ResourceDto resource = resourceDao.getResource(dto.getResourceId());
      issue = toIssue(dto, rule, resource);
    }
    return issue;
  }

  private Issue toIssue(IssueDto dto, Rule rule, ResourceDto resource) {
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

    if (resource != null) {
      issue.setComponentKey(resource.getKey());
    }
    if (rule != null) {
      issue.setRuleKey(RuleKey.of(rule.getRepositoryKey(), rule.getKey()));
    }
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
