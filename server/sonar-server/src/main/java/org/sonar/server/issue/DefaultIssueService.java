/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.DefaultIssueBuilder;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DefaultIssueService implements IssueService {

  private final DbClient dbClient;
  private final IndexClient indexClient;

  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;
  private final IssueNotifications issueNotifications;
  private final ActionPlanService actionPlanService;
  private final RuleFinder ruleFinder;
  private final IssueDao issueDao;
  private final UserFinder userFinder;
  private final PreviewCache dryRunCache;

  public DefaultIssueService(DbClient dbClient, IndexClient indexClient,
    IssueWorkflow workflow,
    IssueStorage issueStorage,
    IssueUpdater issueUpdater,
    IssueNotifications issueNotifications,
    ActionPlanService actionPlanService,
    RuleFinder ruleFinder,
    IssueDao issueDao,
    UserFinder userFinder,
    PreviewCache dryRunCache) {
    this.dbClient = dbClient;
    this.indexClient = indexClient;
    this.workflow = workflow;
    this.issueStorage = issueStorage;
    this.issueUpdater = issueUpdater;
    this.actionPlanService = actionPlanService;
    this.ruleFinder = ruleFinder;
    this.issueNotifications = issueNotifications;
    this.issueDao = issueDao;
    this.userFinder = userFinder;
    this.dryRunCache = dryRunCache;
  }

  @Override
  public List<String> listStatus() {
    return workflow.statusKeys();
  }

  /**
   * List of available transitions.
   * <p/>
   * Never return null, but return an empty list if the issue does not exist.
   */
  @Override
  public List<Transition> listTransitions(String issueKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return listTransitions(getIssueByKey(session, issueKey));
    } finally {
      session.close();
    }
  }

  /**
   * Never return null, but an empty list if the issue does not exist.
   * No security check is done since it should already have been done to get the issue
   */
  @Override
  public List<Transition> listTransitions(@Nullable Issue issue) {
    if (issue == null) {
      return Collections.emptyList();
    }
    List<Transition> outTransitions = workflow.outTransitions(issue);
    List<Transition> allowedTransitions = new ArrayList<Transition>();
    for (Transition transition : outTransitions) {
      DefaultIssue defaultIssue = (DefaultIssue) issue;
      String projectKey = defaultIssue.projectKey();
      if (StringUtils.isBlank(transition.requiredProjectPermission()) ||
        (projectKey != null && UserSession.get().hasProjectPermission(transition.requiredProjectPermission(), projectKey))) {
        allowedTransitions.add(transition);
      }
    }
    return allowedTransitions;
  }

  @Override
  public Issue doTransition(String issueKey, String transitionKey) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue defaultIssue = getIssueByKey(session, issueKey);
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      checkTransitionPermission(transitionKey, UserSession.get(), defaultIssue);
      if (workflow.doTransition(defaultIssue, transitionKey, context)) {
        saveIssue(session, defaultIssue, context);
      }
      return defaultIssue;

    } finally {
      session.close();
    }
  }

  private void checkTransitionPermission(String transitionKey, UserSession userSession, DefaultIssue defaultIssue) {
    List<Transition> outTransitions = workflow.outTransitions(defaultIssue);
    for (Transition transition : outTransitions) {
      String projectKey = defaultIssue.projectKey();
      if (transition.key().equals(transitionKey) && StringUtils.isNotBlank(transition.requiredProjectPermission()) && projectKey != null) {
        userSession.checkProjectPermission(transition.requiredProjectPermission(), projectKey);
      }
    }
  }

  @Override
  public Issue assign(String issueKey, @Nullable String assignee) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getIssueByKey(session, issueKey);
      User user = null;
      if (!Strings.isNullOrEmpty(assignee)) {
        user = userFinder.findByLogin(assignee);
        if (user == null) {
          throw new NotFoundException("Unknown user: " + assignee);
        }
      }
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      if (issueUpdater.assign(issue, user, context)) {
        saveIssue(session, issue, context);
      }
      return issue;

    } finally {
      session.close();
    }
  }

  @Override
  public Issue plan(String issueKey, @Nullable String actionPlanKey) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      ActionPlan actionPlan = null;
      if (!Strings.isNullOrEmpty(actionPlanKey)) {
        actionPlan = actionPlanService.findByKey(actionPlanKey, UserSession.get());
        if (actionPlan == null) {
          throw new NotFoundException("Unknown action plan: " + actionPlanKey);
        }
      }
      DefaultIssue issue = getIssueByKey(session, issueKey);

      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      if (issueUpdater.plan(issue, actionPlan, context)) {
        saveIssue(session, issue, context);
      }
      return issue;

    } finally {
      session.close();
    }
  }

  @Override
  public Issue setSeverity(String issueKey, String severity) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getIssueByKey(session, issueKey);
      UserSession.get().checkProjectPermission(UserRole.ISSUE_ADMIN, issue.projectKey());

      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      if (issueUpdater.setManualSeverity(issue, severity, context)) {
        saveIssue(session, issue, context);
      }
      return issue;
    } finally {
      session.close();
    }
  }

  @Override
  public DefaultIssue createManualIssue(String componentKey, RuleKey ruleKey, @Nullable Integer line, @Nullable String message, @Nullable String severity,
    @Nullable Double effortToFix) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);
      ComponentDto project = dbClient.componentDao().getRootProjectByKey(componentKey, session);

      UserSession.get().checkProjectPermission(UserRole.USER, project.getKey());
      if (!ruleKey.isManual()) {
        throw new IllegalArgumentException("Issues can be created only on rules marked as 'manual': " + ruleKey);
      }
      Rule rule = getRuleByKey(ruleKey);

      DefaultIssue issue = new DefaultIssueBuilder()
        .componentKey(component.getKey())
        .projectKey(project.getKey())
        .line(line)
        .message(!Strings.isNullOrEmpty(message) ? message : rule.getName())
        .severity(Objects.firstNonNull(severity, Severity.MAJOR))
        .effortToFix(effortToFix)
        .ruleKey(ruleKey)
        .reporter(UserSession.get().login())
        .build();

      Date now = new Date();
      issue.setCreationDate(now);
      issue.setUpdateDate(now);
      issueStorage.save(issue);
      dryRunCache.reportResourceModification(component.getKey());
      return issue;
    } finally {
      session.close();
    }
  }

  // TODO result should be replaced by an aggregation object in IssueIndex
  @Override
  public RulesAggregation findRulesByComponent(String componentKey, @Nullable Date periodDate, DbSession session) {
    RulesAggregation rulesAggregation = new RulesAggregation();
    for (RuleDto ruleDto : issueDao.findRulesByComponent(componentKey, periodDate, session)) {
      rulesAggregation.add(ruleDto);
    }
    return rulesAggregation;
  }

  // TODO result should be replaced by an aggregation object in IssueIndex
  @Override
  public Multiset<String> findSeveritiesByComponent(String componentKey, @Nullable Date periodDate, DbSession session) {
    Multiset<String> aggregation = HashMultiset.create();
    for (String severity : issueDao.findSeveritiesByComponent(componentKey, periodDate, session)) {
      aggregation.add(severity);
    }
    return aggregation;
  }

  public DefaultIssue getIssueByKey(DbSession session, String key) {
    // Load from index to check permission
    Issue authorizedIssueIndex = indexClient.get(IssueIndex.class).getByKey(key);
    return dbClient.issueDao().getByKey(session, authorizedIssueIndex.key()).toDefaultIssue();
  }

  public DefaultIssue getIssueByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      // Load from index to check permission
      indexClient.get(IssueIndex.class).getByKey(key);
      return dbClient.issueDao().getByKey(session, key).toDefaultIssue();
    } finally {
      session.close();
    }
  }

  private void saveIssue(DbSession session, DefaultIssue issue, IssueChangeContext context) {
    String projectKey = issue.projectKey();
    if (projectKey == null) {
      throw new IllegalStateException(String.format("Issue '%s' has no project key", issue.key()));
    }
    issueStorage.save(issue);
    issueNotifications.sendChanges(issue, context,
      getRuleByKey(issue.ruleKey()),
      dbClient.componentDao().getByKey(session, projectKey),
      dbClient.componentDao().getNullableByKey(session, issue.componentKey()));
    dryRunCache.reportResourceModification(issue.componentKey());
  }

  private Rule getRuleByKey(RuleKey ruleKey) {
    Rule rule = ruleFinder.findByKey(ruleKey);
    if (rule == null) {
      throw new IllegalArgumentException("Unknown rule: " + ruleKey);
    }
    return rule;
  }

  public org.sonar.server.search.Result<Issue> search(IssueQuery query, QueryContext options) {
    return indexClient.get(IssueIndex.class).search(query, options);
  }

  /**
   * Used by the bulk change
   * TODO move it to the IssueBulkChangeService when OldIssueService will be removed
   */
  @Override
  public List<Issue> search(List<String> issueKeys) {
    // Load from index to check permission
    List<Issue> authorizedIndexIssues = search(IssueQuery.builder().issueKeys(issueKeys).build(), new QueryContext().setMaxLimit()).getHits();
    // return ;
    List<String> authorizedIssueKeys = newArrayList(Iterables.transform(authorizedIndexIssues, new Function<Issue, String>() {
      @Override
      public String apply(@Nullable Issue input) {
        return input != null ? input.key() : null;
      }
    }));

    DbSession session = dbClient.openSession(false);
    try {
      List<IssueDto> issueDtos = dbClient.issueDao().getByKeys(session, authorizedIssueKeys);
      return newArrayList(Iterables.transform(issueDtos, new Function<IssueDto, Issue>() {
        @Override
        public Issue apply(@Nullable IssueDto input) {
          return input != null ? input.toDefaultIssue() : null;
        }
      }));
    } finally {
      session.close();
    }
  }

  private void verifyLoggedIn() {
    UserSession.get().checkLoggedIn();
  }
}
