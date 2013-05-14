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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.component.Component;
import org.sonar.api.issue.*;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.utils.Paging;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.platform.UserSession;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * @since 3.6
 */
public class DefaultIssueFinder implements IssueFinder {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultIssueFinder.class);
  private final MyBatis myBatis;
  private final IssueDao issueDao;
  private final IssueChangeDao issueChangeDao;
  private final AuthorizationDao authorizationDao;
  private final DefaultRuleFinder ruleFinder;
  private final UserFinder userFinder;
  private final ResourceDao resourceDao;
  private final ActionPlanService actionPlanService;

  public DefaultIssueFinder(MyBatis myBatis,
                            IssueDao issueDao, IssueChangeDao issueChangeDao,
                            AuthorizationDao authorizationDao,
                            DefaultRuleFinder ruleFinder,
                            UserFinder userFinder,
                            ResourceDao resourceDao,
                            ActionPlanService actionPlanService) {
    this.myBatis = myBatis;
    this.issueDao = issueDao;
    this.issueChangeDao = issueChangeDao;
    this.authorizationDao = authorizationDao;
    this.ruleFinder = ruleFinder;
    this.userFinder = userFinder;
    this.resourceDao = resourceDao;
    this.actionPlanService = actionPlanService;
  }

  DefaultIssue findByKey(String issueKey, String requiredRole) {
    IssueDto dto = issueDao.selectByKey(issueKey);
    if (dto == null) {
      throw new IllegalStateException("Unknown issue: " + issueKey);
    }
    if (!authorizationDao.isAuthorizedComponentId(dto.getResourceId(), UserSession.get().userId(), requiredRole)) {
      throw new IllegalStateException("User does not have the role " + requiredRole + " required to change the issue: " + issueKey);
    }
    return dto.toDefaultIssue();
  }

  public IssueQueryResult find(IssueQuery query) {
    LOG.debug("IssueQuery : {}", query);
    SqlSession sqlSession = myBatis.openSession();
    try {
      // 1. Select the ids of all the issues that match the query
      List<IssueDto> allIssues = issueDao.selectIssueAndComponentIds(query, sqlSession);

      // 2. Apply security, if needed
      List<IssueDto> authorizedIssues;
      if (query.requiredRole() != null) {
        authorizedIssues = keepAuthorized(allIssues, query.requiredRole(), sqlSession);
      } else {
        authorizedIssues = allIssues;
      }

      // 3. Apply pagination
      Paging paging = Paging.create(query.pageSize(), query.pageIndex(), authorizedIssues.size());
      Set<Long> pagedIssueIds = pagedIssueIds(authorizedIssues, paging);

      // 4. Load issues and their related data (rules, components, comments, action plans, ...)
      Collection<IssueDto> pagedIssues = issueDao.selectByIds(pagedIssueIds, sqlSession);
      Map<String, DefaultIssue> issuesByKey = newHashMap();
      List<Issue> issues = newArrayList();
      Set<Integer> ruleIds = Sets.newHashSet();
      Set<Integer> componentIds = Sets.newHashSet();
      Set<String> actionPlanKeys = Sets.newHashSet();
      Set<String> users = Sets.newHashSet();
      for (IssueDto dto : pagedIssues) {
        DefaultIssue defaultIssue = dto.toDefaultIssue();
        issuesByKey.put(dto.getKee(), defaultIssue);
        issues.add(defaultIssue);
        ruleIds.add(dto.getRuleId());
        componentIds.add(dto.getResourceId());
        actionPlanKeys.add(dto.getActionPlanKey());
        if (dto.getReporter() != null) {
          users.add(dto.getReporter());
        }
        if (dto.getAssignee() != null) {
          users.add(dto.getAssignee());
        }
      }
      List<DefaultIssueComment> comments = issueChangeDao.selectCommentsByIssues(sqlSession, issuesByKey.keySet());
      for (DefaultIssueComment comment : comments) {
        DefaultIssue issue = issuesByKey.get(comment.issueKey());
        issue.addComment(comment);
        if (comment.userLogin() != null) {
          users.add(comment.userLogin());
        }
      }

      return new DefaultResults(issues,
        findRules(ruleIds),
        findComponents(componentIds),
        findActionPlans(actionPlanKeys),
        findUsers(users),
        paging,
        authorizedIssues.size() != allIssues.size());
    } finally {
      MyBatis.closeQuietly(sqlSession);
    }
  }

  private List<IssueDto> keepAuthorized(List<IssueDto> issues, String requiredRole, SqlSession sqlSession) {
    final Set<Integer> authorizedComponentIds = authorizationDao.keepAuthorizedComponentIds(
      extractResourceIds(issues),
      UserSession.get().userId(),
      requiredRole,
      sqlSession
    );
    return newArrayList(Iterables.filter(issues, new Predicate<IssueDto>() {
      @Override
      public boolean apply(IssueDto issueDto) {
        return authorizedComponentIds.contains(issueDto.getResourceId());
      }
    }));
  }

  private Set<Integer> extractResourceIds(List<IssueDto> issues) {
    Set<Integer> componentIds = Sets.newLinkedHashSet();
    for (IssueDto issue : issues) {
      componentIds.add(issue.getResourceId());
    }
    return componentIds;
  }

  private Set<Long> pagedIssueIds(Collection<IssueDto> issues, Paging paging) {
    Set<Long> issueIds = Sets.newLinkedHashSet();
    int index = 0;
    for (IssueDto issue : issues) {
      if (index >= paging.offset() && issueIds.size() < paging.pageSize()) {
        issueIds.add(issue.getId());
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

  private Collection<User> findUsers(Set<String> logins) {
    return userFinder.findByLogins(Lists.newArrayList(logins));
  }

  private Collection<Component> findComponents(Set<Integer> componentIds) {
    return resourceDao.findByIds(componentIds);
  }

  private Collection<ActionPlan> findActionPlans(Set<String> actionPlanKeys) {
    return actionPlanService.findByKeys(actionPlanKeys);
  }

  public Issue findByKey(String key) {
    IssueDto dto = issueDao.selectByKey(key);
    return dto != null ? dto.toDefaultIssue() : null;
  }

  static class DefaultResults implements IssueQueryResult {
    private final List<Issue> issues;
    private final Map<RuleKey, Rule> rulesByKey = Maps.newHashMap();
    private final Map<String, Component> componentsByKey = Maps.newHashMap();
    private final Map<String, ActionPlan> actionPlansByKey = Maps.newHashMap();
    private final Map<String, User> usersByLogin = Maps.newHashMap();
    private final boolean securityExclusions;
    private final Paging paging;

    DefaultResults(List<Issue> issues,
                   Collection<Rule> rules,
                   Collection<Component> components,
                   Collection<ActionPlan> actionPlans,
                   Collection<User> users,
                   Paging paging, boolean securityExclusions) {
      this.issues = issues;
      for (Rule rule : rules) {
        rulesByKey.put(rule.ruleKey(), rule);
      }
      for (Component component : components) {
        componentsByKey.put(component.key(), component);
      }
      for (ActionPlan actionPlan : actionPlans) {
        actionPlansByKey.put(actionPlan.key(), actionPlan);
      }
      for (User user : users) {
        usersByLogin.put(user.login(), user);
      }
      this.paging = paging;
      this.securityExclusions = securityExclusions;
    }

    @Override
    public List<Issue> issues() {
      return issues;
    }

    @Override
    public Rule rule(Issue issue) {
      return rulesByKey.get(issue.ruleKey());
    }

    @Override
    public Collection<Rule> rules() {
      return rulesByKey.values();
    }

    @Override
    public Component component(Issue issue) {
      return componentsByKey.get(issue.componentKey());
    }

    @Override
    public Collection<Component> components() {
      return componentsByKey.values();
    }

    @Override
    public ActionPlan actionPlan(Issue issue) {
      return actionPlansByKey.get(issue.actionPlanKey());
    }

    @Override
    public Collection<ActionPlan> actionPlans() {
      return actionPlansByKey.values();
    }

    @Override
    public Collection<User> users() {
      return usersByLogin.values();
    }

    @Override
    @CheckForNull
    public User user(String login) {
      return usersByLogin.get(login);
    }

    @Override
    public boolean securityExclusions() {
      return securityExclusions;
    }

    @Override
    public Paging paging() {
      return paging;
    }
  }
}
