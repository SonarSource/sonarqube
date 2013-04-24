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
package org.sonar.server.issue;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.Paging;
import org.sonar.api.rules.Rule;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.core.user.AuthorizationDao;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * @since 3.6
 */
public class ServerIssueFinder implements IssueFinder {

  private static final Logger LOG = LoggerFactory.getLogger(ServerIssueFinder.class);
  private final MyBatis myBatis;
  private final IssueDao issueDao;
  private final AuthorizationDao authorizationDao;
  private final DefaultRuleFinder ruleFinder;
  private final ResourceDao resourceDao;

  public ServerIssueFinder(MyBatis myBatis, IssueDao issueDao, AuthorizationDao authorizationDao, DefaultRuleFinder ruleFinder, ResourceDao resourceDao) {
    this.myBatis = myBatis;
    this.issueDao = issueDao;
    this.authorizationDao = authorizationDao;
    this.ruleFinder = ruleFinder;
    this.resourceDao = resourceDao;
  }

  public Results find(IssueQuery query, @Nullable Integer currentUserId, String role) {
    LOG.debug("IssueQuery : {}", query);
    SqlSession sqlSession = myBatis.openSession();
    try {
      List<IssueDto> allIssuesDto = issueDao.selectIssueIdsAndComponentsId(query, sqlSession);
      Set<Integer> componentIds = getResourceIds(allIssuesDto);
      Set<Integer> authorizedComponentIds = authorizationDao.keepAuthorizedComponentIds(componentIds, currentUserId, role, sqlSession);
      List<IssueDto> authorizedIssues = getAuthorizedIssues(allIssuesDto, authorizedComponentIds);
      Paging paging = new Paging(query.pageSize(), query.pageIndex(), authorizedIssues.size());
      Set<Long> paginatedAuthorizedIssueIds = getPaginatedAuthorizedIssueIds(authorizedIssues, paging);

      Collection<IssueDto> dtos = issueDao.selectByIds(paginatedAuthorizedIssueIds, sqlSession);
      Set<Integer> ruleIds = Sets.newLinkedHashSet();
      List<Issue> issues = newArrayList();
      for (IssueDto dto : dtos) {
        if (authorizedComponentIds.contains(dto.getResourceId())) {
          issues.add(dto.toDefaultIssue());
          ruleIds.add(dto.getRuleId());
        }
      }

      return new DefaultResults(issues, getRulesByIssue(issues, ruleIds), getComponentsByIssue(issues, componentIds), paging);
    } finally {
      MyBatis.closeQuietly(sqlSession);
    }
  }

  private Set<Integer> getResourceIds(List<IssueDto> allIssuesDto) {
    Set<Integer> componentIds = Sets.newLinkedHashSet();
    for (IssueDto issueDto : allIssuesDto) {
      componentIds.add(issueDto.getResourceId());
    }
    return componentIds;
  }

  private List<IssueDto> getAuthorizedIssues(List<IssueDto> allIssuesDto, final Set<Integer> authorizedComponentIds) {
    return newArrayList(Iterables.filter(allIssuesDto, new Predicate<IssueDto>() {
      @Override
      public boolean apply(IssueDto issueDto) {
        return authorizedComponentIds.contains(issueDto.getResourceId());
      }
    }));
  }

  private Set<Long> getPaginatedAuthorizedIssueIds(List<IssueDto> authorizedIssues, Paging paging) {
    Set<Long> issueIds = Sets.newLinkedHashSet();
    int index = 0;
    for (IssueDto issueDto : authorizedIssues) {
      if (index >= paging.offset() && issueIds.size() < paging.pageSize()) {
        issueIds.add(issueDto.getId());
      } else if (issueIds.size() >= paging.pageSize()) {
        break;
      }
      index++;
    }
    return issueIds;
  }

  private Map<Issue, Rule> getRulesByIssue(List<Issue> issues, Set<Integer> ruleIds) {
    Map<Issue, Rule> rulesByIssue = newHashMap();
    Collection<Rule> rules = ruleFinder.findByIds(ruleIds);
    for (Issue issue : issues) {
      rulesByIssue.put(issue, findRule(issue, rules));
    }
    return rulesByIssue;
  }

  private Rule findRule(final Issue issue, Collection<Rule> rules) {
    return Iterables.find(rules, new Predicate<Rule>() {
      @Override
      public boolean apply(Rule rule) {
        return issue.ruleKey().equals(rule.ruleKey());
      }
    }, null);
  }

  private Map<Issue, Component> getComponentsByIssue(List<Issue> issues, Set<Integer> componentIds) {
    Map<Issue, Component> componentsByIssue = newHashMap();
    Collection<Component> components = resourceDao.findByIds(componentIds);
    for (Issue issue : issues) {
      componentsByIssue.put(issue, findComponent(issue, components));
    }
    return componentsByIssue;
  }

  private Component findComponent(final Issue issue, Collection<Component> components) {
    return Iterables.find(components, new Predicate<Component>() {
      @Override
      public boolean apply(Component component) {
        return issue.componentKey().equals(component.key());
      }
    }, null);
  }

  public Issue findByKey(String key) {
    IssueDto dto = issueDao.selectByKey(key);
    return dto != null ? dto.toDefaultIssue() : null;
  }

  static class DefaultResults implements Results {
    private final List<Issue> issues;
    private final Paging paging;
    private final Map<Issue, Rule> rulesByIssue;
    private final Map<Issue, Component> componentsByIssue;

    DefaultResults(List<Issue> issues, Map<Issue, Rule> rulesByIssue, Map<Issue, Component> componentsByIssue, Paging paging) {
      this.issues = issues;
      this.rulesByIssue = rulesByIssue;
      this.componentsByIssue = componentsByIssue;
      this.paging = paging;
    }

    @Override
    public List<Issue> issues() {
      return issues;
    }

    public Rule rule(Issue issue) {
      return rulesByIssue.get(issue);
    }

    public Collection<Rule> rules() {
      return rulesByIssue.values();
    }

    public Component component(Issue issue) {
      return componentsByIssue.get(issue);
    }

    public Collection<Component> components() {
      return componentsByIssue.values();
    }

    public Paging paging() {
      return paging;
    }
  }
}
