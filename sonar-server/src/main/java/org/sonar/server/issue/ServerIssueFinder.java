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
import com.google.common.collect.*;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.component.Component;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.Paging;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.db.ActionPlanIssueDao;
import org.sonar.core.issue.db.ActionPlanIssueDto;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;
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
  private final ActionPlanIssueDao actionPlanIssueDao;

  public ServerIssueFinder(MyBatis myBatis, IssueDao issueDao, AuthorizationDao authorizationDao, DefaultRuleFinder ruleFinder, ResourceDao resourceDao, ActionPlanIssueDao actionPlanIssueDao) {
    this.myBatis = myBatis;
    this.issueDao = issueDao;
    this.authorizationDao = authorizationDao;
    this.ruleFinder = ruleFinder;
    this.resourceDao = resourceDao;
    this.actionPlanIssueDao = actionPlanIssueDao;
  }

  public Results find(IssueQuery query, @Nullable Integer currentUserId, String role) {
    LOG.debug("IssueQuery : {}", query);
    SqlSession sqlSession = myBatis.openSession();
    try {
      List<IssueDto> allIssuesDto = issueDao.selectIssueIdsAndComponentsId(query, sqlSession);
      Set<Integer> componentIds = extractResourceIds(allIssuesDto);
      Set<Integer> authorizedComponentIds = authorizationDao.keepAuthorizedComponentIds(componentIds, currentUserId, role, sqlSession);
      List<IssueDto> authorizedIssues = authorized(allIssuesDto, authorizedComponentIds);
      Paging paging = Paging.create(query.pageSize(), query.pageIndex(), authorizedIssues.size());
      Set<Long> pagedAuthorizedIssueIds = pagedAuthorizedIssueIds(authorizedIssues, paging);

      Collection<IssueDto> dtos = issueDao.selectByIds(pagedAuthorizedIssueIds, sqlSession);
      Set<Integer> ruleIds = Sets.newHashSet();
      List<Issue> issues = newArrayList();
      List<Long> issueIds = newArrayList();
      Map<Long, Issue> issuesById = newHashMap();
      for (IssueDto dto : dtos) {
        if (authorizedComponentIds.contains(dto.getResourceId())) {
          DefaultIssue defaultIssue = dto.toDefaultIssue();
          issuesById.put(dto.getId(), defaultIssue);
          issueIds.add(dto.getId());
          issues.add(defaultIssue);
          ruleIds.add(dto.getRuleId());
        }
      }

      Collection<ActionPlanIssueDto> actionPlanIssueDtos = actionPlanIssueDao.findByIssueIds(issueIds, sqlSession);
      ListMultimap<Issue, ActionPlan> actionPlansByIssueKey = createActionPlansByIssue(actionPlanIssueDtos, issuesById);
      setActionPlans(issues, actionPlansByIssueKey);

      return new DefaultResults(issues,
        findRules(ruleIds),
        findComponents(componentIds),
        actionPlansByIssueKey,
        paging, authorizedIssues.size() != allIssuesDto.size());
    } finally {
      MyBatis.closeQuietly(sqlSession);
    }
  }

  private Set<Integer> extractResourceIds(List<IssueDto> dtos) {
    Set<Integer> componentIds = Sets.newLinkedHashSet();
    for (IssueDto issueDto : dtos) {
      componentIds.add(issueDto.getResourceId());
    }
    return componentIds;
  }

  private List<IssueDto> authorized(List<IssueDto> dtos, final Set<Integer> authorizedComponentIds) {
    return newArrayList(Iterables.filter(dtos, new Predicate<IssueDto>() {
      @Override
      public boolean apply(IssueDto issueDto) {
        return authorizedComponentIds.contains(issueDto.getResourceId());
      }
    }));
  }

  private Set<Long> pagedAuthorizedIssueIds(List<IssueDto> authorizedIssues, Paging paging) {
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

  private Collection<Rule> findRules(Set<Integer> ruleIds) {
    return ruleFinder.findByIds(ruleIds);
  }

  private Collection<Component> findComponents(Set<Integer> componentIds) {
    return resourceDao.findByIds(componentIds);
  }

  private ListMultimap createActionPlansByIssue(Collection<ActionPlanIssueDto> actionPlanIssueDtos, Map<Long, Issue> issuesById) {
    ListMultimap<Issue, ActionPlan> actionPlansByIssue = ArrayListMultimap.create();
    for (ActionPlanIssueDto actionPlanIssueDto : actionPlanIssueDtos) {
      Issue issue = issuesById.get(actionPlanIssueDto.getIssueId());
      actionPlansByIssue.put(issue, actionPlanIssueDto.toActionPlan());
    }
    return actionPlansByIssue;
  }

  private void setActionPlans(List<Issue> issues, ListMultimap<Issue, ActionPlan> actionPlansByIssueKey) {
    for (Issue issue : issues) {
      DefaultIssue defaultIssue = (DefaultIssue) issue;
      List<ActionPlan> actionPlans = actionPlansByIssueKey.get(issue);
      defaultIssue.setActionPlans(actionPlans);
    }
  }

  public Issue findByKey(String key) {
    IssueDto dto = issueDao.selectByKey(key);
    return dto != null ? dto.toDefaultIssue() : null;
  }

  static class DefaultResults implements Results {
    private final List<Issue> issues;
    private final Map<RuleKey, Rule> rulesByKey = Maps.newHashMap();
    private final Map<String, Component> componentsByKey = Maps.newHashMap();
    private final ListMultimap<Issue, ActionPlan> actionPlansByIssue;
    private final boolean securityExclusions;
    private final Paging paging;

    DefaultResults(List<Issue> issues,
                   Collection<Rule> rules,
                   Collection<Component> components,
                   ListMultimap<Issue,
                     ActionPlan> actionPlansByIssue,
                   Paging paging, boolean securityExclusions) {
      this.issues = issues;
      for (Rule rule : rules) {
        rulesByKey.put(rule.ruleKey(), rule);
      }
      for (Component component : components) {
        componentsByKey.put(component.key(), component);
      }
      this.actionPlansByIssue = actionPlansByIssue;
      this.paging = paging;
      this.securityExclusions = securityExclusions;
    }

    @Override
    public List<Issue> issues() {
      return issues;
    }

    public Rule rule(Issue issue) {
      return rulesByKey.get(issue.ruleKey());
    }

    public Collection<Rule> rules() {
      return rulesByKey.values();
    }

    public Component component(Issue issue) {
      return componentsByKey.get(issue.componentKey());
    }

    public Collection<Component> components() {
      return componentsByKey.values();
    }

    public Collection<ActionPlan> actionPlans(Issue issue) {
      return actionPlansByIssue.get(issue);
    }

    public Collection<ActionPlan> actionPlans() {
      return actionPlansByIssue.values();
    }

    public boolean securityExclusions() {
      return securityExclusions;
    }

    public Paging paging() {
      return paging;
    }
  }
}
