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

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @since 3.6
 */
public class IssueService implements ServerComponent {

  private final DefaultIssueFinder finder;
  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;
  private final IssueNotifications issueNotifications;
  private final ActionPlanService actionPlanService;
  private final RuleFinder ruleFinder;
  private final ResourceDao resourceDao;
  private final AuthorizationDao authorizationDao;
  private final UserFinder userFinder;
  private final PreviewCache dryRunCache;

  public IssueService(DefaultIssueFinder finder,
    IssueWorkflow workflow,
    IssueStorage issueStorage,
    IssueUpdater issueUpdater,
    IssueNotifications issueNotifications,
    ActionPlanService actionPlanService,
    RuleFinder ruleFinder,
    ResourceDao resourceDao,
    AuthorizationDao authorizationDao,
    UserFinder userFinder,
    PreviewCache dryRunCache) {
    this.finder = finder;
    this.workflow = workflow;
    this.issueStorage = issueStorage;
    this.issueUpdater = issueUpdater;
    this.actionPlanService = actionPlanService;
    this.ruleFinder = ruleFinder;
    this.issueNotifications = issueNotifications;
    this.resourceDao = resourceDao;
    this.authorizationDao = authorizationDao;
    this.userFinder = userFinder;
    this.dryRunCache = dryRunCache;
  }

  /**
   * List of available transitions.
   * <p/>
   * Never return null, but return an empty list if the issue does not exist.
   */
  public List<Transition> listTransitions(String issueKey, UserSession userSession) {
    Issue issue = loadIssue(issueKey).first();
    return listTransitions(issue, userSession);
  }

  /**
   * Never return null, but an empty list if the issue does not exist.
   * No security check is done since it should already have been done to get the issue
   */
  public List<Transition> listTransitions(@Nullable Issue issue, UserSession userSession) {
    if (issue == null) {
      return Collections.emptyList();
    }
    List<Transition> outTransitions = workflow.outTransitions(issue);
    List<Transition> allowedTransitions = new ArrayList<Transition>();
    for (Transition transition : outTransitions) {
      DefaultIssue defaultIssue = (DefaultIssue) issue;
      if (StringUtils.isBlank(transition.requiredProjectPermission()) ||
        userSession.hasProjectPermission(UserRole.ISSUE_ADMIN, defaultIssue.projectKey())) {
        allowedTransitions.add(transition);
      }
    }
    return allowedTransitions;
  }

  public Issue doTransition(String issueKey, String transition, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueQueryResult queryResult = loadIssue(issueKey);
    DefaultIssue issue = (DefaultIssue) queryResult.first();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (workflow.doTransition(issue, transition, context)) {
      issueStorage.save(issue);
      issueNotifications.sendChanges(issue, context, queryResult);
      dryRunCache.reportResourceModification(issue.componentKey());
    }
    return issue;
  }

  public Issue assign(String issueKey, @Nullable String assignee, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueQueryResult queryResult = loadIssue(issueKey);
    DefaultIssue issue = (DefaultIssue) queryResult.first();
    User user = null;
    if (!Strings.isNullOrEmpty(assignee)) {
      user = userFinder.findByLogin(assignee);
      if (user == null) {
        throw new IllegalArgumentException("Unknown user: " + assignee);
      }
    }
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.assign(issue, user, context)) {
      issueStorage.save(issue);
      issueNotifications.sendChanges(issue, context, queryResult);
      dryRunCache.reportResourceModification(issue.componentKey());
    }
    return issue;
  }

  public Issue plan(String issueKey, @Nullable String actionPlanKey, UserSession userSession) {
    verifyLoggedIn(userSession);
    ActionPlan actionPlan = null;
    if (!Strings.isNullOrEmpty(actionPlanKey)) {
      actionPlan = actionPlanService.findByKey(actionPlanKey, userSession);
      if (actionPlan == null) {
        throw new IllegalArgumentException("Unknown action plan: " + actionPlanKey);
      }
    }
    IssueQueryResult queryResult = loadIssue(issueKey);
    DefaultIssue issue = (DefaultIssue) queryResult.first();

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.plan(issue, actionPlan, context)) {
      issueStorage.save(issue);
      issueNotifications.sendChanges(issue, context, queryResult);
      dryRunCache.reportResourceModification(issue.componentKey());
    }
    return issue;
  }

  public Issue setSeverity(String issueKey, String severity, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueQueryResult queryResult = loadIssue(issueKey);
    DefaultIssue issue = (DefaultIssue) queryResult.first();
    userSession.checkProjectPermission(UserRole.ISSUE_ADMIN, issue.projectKey());

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.setManualSeverity(issue, severity, context)) {
      issueStorage.save(issue);
      issueNotifications.sendChanges(issue, context, queryResult);
      dryRunCache.reportResourceModification(issue.componentKey());
    }
    return issue;
  }

  public DefaultIssue createManualIssue(DefaultIssue issue, UserSession userSession) {
    verifyLoggedIn(userSession);
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(issue.componentKey()));
    if (resourceDto == null) {
      throw new IllegalArgumentException("Unknown component: " + issue.componentKey());
    }
    if (!authorizationDao.isAuthorizedComponentKey(issue.componentKey(), userSession.userId(), UserRole.USER)) {
      // TODO throw unauthorized
      throw new IllegalStateException("User does not have the required role");
    }
    if (!"manual".equals(issue.ruleKey().repository())) {
      throw new IllegalArgumentException("Issues can be created only on rules marked as 'manual': " + issue.ruleKey());
    }

    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    if (rule == null) {
      throw new IllegalArgumentException("Unknown rule: " + issue.ruleKey());
    }

    Date now = new Date();
    issue.setCreationDate(now);
    issue.setUpdateDate(now);
    issueStorage.save(issue);
    dryRunCache.reportResourceModification(issue.componentKey());
    return issue;
  }

  public IssueQueryResult loadIssue(String issueKey) {
    IssueQueryResult result = finder.find(IssueQuery.builder().issueKeys(Arrays.asList(issueKey)).requiredRole(UserRole.USER).build());
    if (result.issues().size() != 1) {
      // TODO throw 404
      throw new IllegalArgumentException("Issue not found: " + issueKey);
    }
    return result;
  }

  public List<String> listStatus() {
    return workflow.statusKeys();
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
  }

}
