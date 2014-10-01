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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
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
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Maps.newLinkedHashMap;

public class IssueService implements ServerComponent {

  private final DbClient dbClient;
  private final IndexClient indexClient;

  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;
  private final IssueNotifications issueNotifications;
  private final ActionPlanService actionPlanService;
  private final RuleFinder ruleFinder;
  private final IssueDao deprecatedIssueDao;
  private final UserFinder userFinder;
  private final PreviewCache dryRunCache;

  public IssueService(DbClient dbClient, IndexClient indexClient,
    IssueWorkflow workflow,
    IssueStorage issueStorage,
    IssueUpdater issueUpdater,
    IssueNotifications issueNotifications,
    ActionPlanService actionPlanService,
    RuleFinder ruleFinder,
    IssueDao deprecatedIssueDao,
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
    this.deprecatedIssueDao = deprecatedIssueDao;
    this.userFinder = userFinder;
    this.dryRunCache = dryRunCache;
  }

  public List<String> listStatus() {
    return workflow.statusKeys();
  }

  /**
   * List of available transitions.
   * <p/>
   * Never return null, but return an empty list if the issue does not exist.
   */
  public List<Transition> listTransitions(String issueKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return listTransitions(getByKeyForUpdate(session, issueKey).toDefaultIssue());
    } finally {
      session.close();
    }
  }

  /**
   * Never return null, but an empty list if the issue does not exist.
   * No security check is done since it should already have been done to get the issue
   */
  public List<Transition> listTransitions(@Nullable Issue issue) {
    if (issue == null) {
      return Collections.emptyList();
    }
    List<Transition> outTransitions = workflow.outTransitions(issue);
    List<Transition> allowedTransitions = new ArrayList<Transition>();
    for (Transition transition : outTransitions) {
      String projectKey = issue.projectKey();
      if (StringUtils.isBlank(transition.requiredProjectPermission()) ||
        (projectKey != null && UserSession.get().hasProjectPermission(transition.requiredProjectPermission(), projectKey))) {
        allowedTransitions.add(transition);
      }
    }
    return allowedTransitions;
  }

  public Issue doTransition(String issueKey, String transitionKey) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue defaultIssue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      checkTransitionPermission(transitionKey, UserSession.get(), defaultIssue);
      if (workflow.doTransition(defaultIssue, transitionKey, context)) {
        saveIssue(session, defaultIssue, context, null);
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

  public Issue assign(String issueKey, @Nullable String assignee) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      User user = null;
      if (!Strings.isNullOrEmpty(assignee)) {
        user = userFinder.findByLogin(assignee);
        if (user == null) {
          throw new NotFoundException("Unknown user: " + assignee);
        }
      }
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      if (issueUpdater.assign(issue, user, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue;

    } finally {
      session.close();
    }
  }

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
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();

      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      if (issueUpdater.plan(issue, actionPlan, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue;

    } finally {
      session.close();
    }
  }

  public Issue setSeverity(String issueKey, String severity) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      UserSession.get().checkProjectPermission(UserRole.ISSUE_ADMIN, issue.projectKey());

      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      if (issueUpdater.setManualSeverity(issue, severity, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue;
    } finally {
      session.close();
    }
  }

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
  public RulesAggregation findRulesByComponent(String componentKey, @Nullable Date periodDate, DbSession session) {
    RulesAggregation rulesAggregation = new RulesAggregation();
    for (RuleDto ruleDto : deprecatedIssueDao.findRulesByComponent(componentKey, periodDate, session)) {
      rulesAggregation.add(ruleDto);
    }
    return rulesAggregation;
  }

  // TODO result should be replaced by an aggregation object in IssueIndex
  public Multiset<String> findSeveritiesByComponent(String componentKey, @Nullable Date periodDate, DbSession session) {
    Multiset<String> aggregation = HashMultiset.create();
    for (String severity : deprecatedIssueDao.findSeveritiesByComponent(componentKey, periodDate, session)) {
      aggregation.add(severity);
    }
    return aggregation;
  }

  public Map<String, Integer> findIssueAssignees(IssueQuery query) {
    Map<String, Integer> result = newLinkedHashMap();
    List<FacetValue> facetValues = indexClient.get(IssueIndex.class).listAssignees(query);
    for (FacetValue facetValue : facetValues) {
      if (facetValue.getKey().equals("_notAssigned_")) {
        result.put(null, facetValue.getValue());
      } else {
        result.put(facetValue.getKey(), facetValue.getValue());
      }
    }
    return result;
  }

  public Issue getByKey(String key) {
    return indexClient.get(IssueIndex.class).getByKey(key);
  }

  IssueDto getByKeyForUpdate(DbSession session, String key) {
    // Load from index to check permission : if the user has no permission to see the issue an exception will be generated
    Issue authorizedIssueIndex = getByKey(key);
    return dbClient.issueDao().getByKey(session, authorizedIssueIndex.key());
  }

  void saveIssue(DbSession session, DefaultIssue issue, IssueChangeContext context, @Nullable String comment) {
    String projectKey = issue.projectKey();
    if (projectKey == null) {
      throw new IllegalStateException(String.format("Issue '%s' has no project key", issue.key()));
    }
    issueStorage.save(issue);
    issueNotifications.sendChanges(issue, context,
      getRuleByKey(issue.ruleKey()),
      dbClient.componentDao().getByKey(session, projectKey),
      dbClient.componentDao().getNullableByKey(session, issue.componentKey()),
      comment);
    dryRunCache.reportResourceModification(issue.componentKey());
  }

  /**
   * Should use {@link org.sonar.server.rule.RuleService#getByKey(org.sonar.api.rule.RuleKey)}, but it's not possible as IssueNotifications is still used by the batch.
   */
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

  private void verifyLoggedIn() {
    UserSession.get().checkLoggedIn();
  }
}
