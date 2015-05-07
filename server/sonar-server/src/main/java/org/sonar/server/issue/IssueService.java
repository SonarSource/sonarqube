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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.DefaultIssueBuilder;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ServerSide
public class IssueService {

  private final DbClient dbClient;
  private final IssueIndex issueIndex;

  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;
  private final NotificationManager notificationService;
  private final ActionPlanService actionPlanService;
  private final RuleFinder ruleFinder;
  private final UserFinder userFinder;
  private final UserIndex userIndex;
  private final SourceLineIndex sourceLineIndex;

  public IssueService(DbClient dbClient, IssueIndex issueIndex,
    IssueWorkflow workflow,
    IssueStorage issueStorage,
    IssueUpdater issueUpdater,
    NotificationManager notificationService,
    ActionPlanService actionPlanService,
    RuleFinder ruleFinder,
    UserFinder userFinder,
    UserIndex userIndex, SourceLineIndex sourceLineIndex) {
    this.dbClient = dbClient;
    this.issueIndex = issueIndex;
    this.workflow = workflow;
    this.issueStorage = issueStorage;
    this.issueUpdater = issueUpdater;
    this.actionPlanService = actionPlanService;
    this.ruleFinder = ruleFinder;
    this.notificationService = notificationService;
    this.userFinder = userFinder;
    this.userIndex = userIndex;
    this.sourceLineIndex = sourceLineIndex;
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
      String projectUuid = issue.projectUuid();
      if (StringUtils.isBlank(transition.requiredProjectPermission()) ||
        (projectUuid != null && UserSession.get().hasProjectPermissionByUuid(transition.requiredProjectPermission(), projectUuid))) {
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
      ComponentDto project = dbClient.componentDao().getByUuid(session, component.projectUuid());

      UserSession.get().checkProjectPermission(UserRole.USER, project.getKey());
      if (!ruleKey.isManual()) {
        throw new IllegalArgumentException("Issues can be created only on rules marked as 'manual': " + ruleKey);
      }
      Rule rule = getNullableRuleByKey(ruleKey);
      if (rule == null) {
        throw new IllegalArgumentException("Unknown rule: " + ruleKey);
      }

      DefaultIssue issue = new DefaultIssueBuilder()
        .componentKey(component.getKey())
        .projectKey(project.getKey())
        .line(line)
        .message(!Strings.isNullOrEmpty(message) ? message : rule.getName())
        .severity(Objects.firstNonNull(severity, Severity.MAJOR))
        .effortToFix(effortToFix)
        .ruleKey(ruleKey)
        .reporter(UserSession.get().login())
        .assignee(findSourceLineUser(component.uuid(), line))
        .build();

      Date now = new Date();
      issue.setCreationDate(now);
      issue.setUpdateDate(now);
      issueStorage.save(issue);
      return issue;
    } finally {
      session.close();
    }
  }

  public Issue getByKey(String key) {
    return issueIndex.getByKey(key);
  }

  IssueDto getByKeyForUpdate(DbSession session, String key) {
    // Load from index to check permission : if the user has no permission to see the issue an exception will be generated
    Issue authorizedIssueIndex = getByKey(key);
    return dbClient.issueDao().selectByKey(session, authorizedIssueIndex.key());
  }

  void saveIssue(DbSession session, DefaultIssue issue, IssueChangeContext context, @Nullable String comment) {
    String projectKey = issue.projectKey();
    if (projectKey == null) {
      throw new IllegalStateException(String.format("Issue '%s' has no project key", issue.key()));
    }
    issueStorage.save(session, issue);
    Rule rule = getNullableRuleByKey(issue.ruleKey());
    notificationService.scheduleForSending(new IssueChangeNotification()
      .setIssue(issue)
      .setChangeAuthorLogin(context.login())
      .setRuleName(rule != null ? rule.getName() : null)
      .setProject(dbClient.componentDao().getByKey(session, projectKey))
      .setComponent(dbClient.componentDao().getNullableByKey(session, issue.componentKey()))
      .setComment(comment));
  }

  /**
   * Should use {@link org.sonar.server.rule.RuleService#getByKey(org.sonar.api.rule.RuleKey)}, but it's not possible as IssueNotifications is still used by the batch.
   * Can be null for removed rules
   */
  private Rule getNullableRuleByKey(RuleKey ruleKey) {
    return ruleFinder.findByKey(ruleKey);
  }

  public SearchResult<IssueDoc> search(IssueQuery query, SearchOptions options) {
    return issueIndex.search(query, options);
  }

  private void verifyLoggedIn() {
    UserSession.get().checkLoggedIn();
  }

  /**
   * Search for all tags, whatever issue resolution or user access rights
   */
  public List<String> listTags(@Nullable String textQuery, int pageSize) {
    IssueQuery query = IssueQuery.builder()
      .checkAuthorization(false)
      .build();
    return issueIndex.listTags(query, textQuery, pageSize);
  }

  public List<String> listAuthors(@Nullable String textQuery, int pageSize) {
    IssueQuery query = IssueQuery.builder()
      .checkAuthorization(false)
      .build();
    return issueIndex.listAuthors(query, textQuery, pageSize);
  }

  public Collection<String> setTags(String issueKey, Collection<String> tags) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), UserSession.get().login());
      if (issueUpdater.setTags(issue, tags, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue.tags();

    } finally {
      session.close();
    }
  }

  public Map<String, Long> listTagsForComponent(IssueQuery query, int pageSize) {
    return issueIndex.countTags(query, pageSize);
  }

  @CheckForNull
  private String findSourceLineUser(String fileUuid, @Nullable Integer line) {
    if (line != null) {
      SourceLineDoc sourceLine = sourceLineIndex.getLine(fileUuid, line);
      String scmAuthor = sourceLine.scmAuthor();
      if (!Strings.isNullOrEmpty(scmAuthor)) {
        UserDoc userDoc = userIndex.getNullableByScmAccount(scmAuthor);
        if (userDoc != null) {
          return userDoc.login();
        }
      }
    }
    return null;
  }
}
