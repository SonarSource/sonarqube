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
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.user.UserFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.server.user.UserSession;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class IssueBulkChangeService {

  private final DefaultIssueFinder issueFinder;
  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;
  private final IssueNotifications issueNotifications;
  private final ActionPlanService actionPlanService;
  private final UserFinder userFinder;

  public IssueBulkChangeService(DefaultIssueFinder issueFinder, IssueWorkflow workflow, ActionPlanService actionPlanService, UserFinder userFinder,
                                IssueUpdater issueUpdater, IssueStorage issueStorage, IssueNotifications issueNotifications) {
    this.issueFinder = issueFinder;
    this.workflow = workflow;
    this.issueUpdater = issueUpdater;
    this.issueStorage = issueStorage;
    this.issueNotifications = issueNotifications;
    this.actionPlanService = actionPlanService;
    this.userFinder = userFinder;
  }

  public List<Issue> execute(IssueBulkChangeQuery issueBulkChangeQuery, UserSession userSession) {
    List<Issue> issues = newArrayList();
    verifyLoggedIn(userSession);

    IssueQueryResult issueQueryResult = issueFinder.find(IssueQuery.builder().issueKeys(issueBulkChangeQuery.issueKeys()).requiredRole(UserRole.USER).build());

    String assignee = issueBulkChangeQuery.assignee();
    if (assignee != null && userFinder.findByLogin(assignee) == null) {
      throw new IllegalArgumentException("Unknown user: " + assignee);
    }

    String actionPlanKey = issueBulkChangeQuery.plan();
    if (!Strings.isNullOrEmpty(actionPlanKey) && actionPlanService.findByKey(actionPlanKey, userSession) == null) {
      throw new IllegalArgumentException("Unknown action plan: " + actionPlanKey);
    }
    String severity = issueBulkChangeQuery.severity();
    String transition = issueBulkChangeQuery.transition();
    String comment = issueBulkChangeQuery.comment();

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    for (Issue issue : issueQueryResult.issues()) {
      DefaultIssue defaultIssue = (DefaultIssue) issue;
      try {
        if (issueBulkChangeQuery.isOnAssignee()) {
          issueUpdater.assign(defaultIssue, assignee, context);
        }
        if (issueBulkChangeQuery.isOnActionPlan()) {
          issueUpdater.plan(defaultIssue, actionPlanKey, context);
        }
        if (issueBulkChangeQuery.isOnSeverity()) {
          issueUpdater.setManualSeverity(defaultIssue, severity, context);
        }
        if (issueBulkChangeQuery.isOnTransition()) {
          workflow.doTransition(defaultIssue, transition, context);
        }
        if (issueBulkChangeQuery.isOnComment()) {
          issueUpdater.addComment(defaultIssue, comment, context);
        }
        issueStorage.save(defaultIssue);
        issueNotifications.sendChanges(defaultIssue, context, issueQueryResult);
        issues.add(defaultIssue);
      } catch (Exception e) {
        // Do nothing, just go to the next issue
      }
    }
    return issues;
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
  }
}
