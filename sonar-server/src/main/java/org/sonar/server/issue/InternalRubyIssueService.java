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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.*;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.SonarException;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.DefaultIssueBuilder;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * All the issue features that are not published to public API.
 */
public class InternalRubyIssueService implements ServerComponent {

  private final IssueService issueService;
  private final IssueCommentService commentService;
  private final IssueChangelogService changelogService;
  private final ActionPlanService actionPlanService;
  private final IssueStatsFinder issueStatsFinder;
  private final ResourceDao resourceDao;
  private final ActionService actionService;
  private final IssueFilterService issueFilterService;

  public InternalRubyIssueService(IssueService issueService,
                                  IssueCommentService commentService,
                                  IssueChangelogService changelogService, ActionPlanService actionPlanService,
                                  IssueStatsFinder issueStatsFinder, ResourceDao resourceDao, ActionService actionService,
                                  IssueFilterService issueFilterService) {
    this.issueService = issueService;
    this.commentService = commentService;
    this.changelogService = changelogService;
    this.actionPlanService = actionPlanService;
    this.issueStatsFinder = issueStatsFinder;
    this.resourceDao = resourceDao;
    this.actionService = actionService;
    this.issueFilterService = issueFilterService;
  }

  public IssueStatsFinder.IssueStatsResult findIssueAssignees(Map<String, Object> params) {
    return issueStatsFinder.findIssueAssignees(PublicRubyIssueService.toQuery(params));
  }

  public List<Transition> listTransitions(String issueKey) {
    return issueService.listTransitions(issueKey);
  }

  public List<Transition> listTransitions(Issue issue) {
    return issueService.listTransitions(issue);
  }

  public List<String> listStatus() {
    return issueService.listStatus();
  }

  public List<String> listResolutions() {
    return Issue.RESOLUTIONS;
  }

  public IssueChangelog changelog(String issueKey) {
    // TODO verify security
    return changelogService.changelog(issueKey, UserSession.get());
  }

  public Result<Issue> doTransition(String issueKey, String transitionKey) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.doTransition(issueKey, transitionKey, UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<Issue> assign(String issueKey, @Nullable String assignee) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.assign(issueKey, StringUtils.defaultIfBlank(assignee, null), UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<Issue> setSeverity(String issueKey, String severity) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.setSeverity(issueKey, severity, UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<Issue> plan(String issueKey, @Nullable String actionPlanKey) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.plan(issueKey, actionPlanKey, UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<IssueComment> addComment(String issueKey, String text) {
    Result<IssueComment> result = Result.of();
    try {
      result.set(commentService.addComment(issueKey, text, UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public IssueComment deleteComment(String commentKey) {
    return commentService.deleteComment(commentKey, UserSession.get());
  }

  public IssueComment editComment(String commentKey, String newText) {
    return commentService.editComment(commentKey, newText, UserSession.get());
  }

  public IssueComment findComment(String commentKey) {
    return commentService.findComment(commentKey);
  }

  /**
   * Create manual issue
   */
  public Result<DefaultIssue> create(Map<String, String> params) {
    Result<DefaultIssue> result = Result.of();
    try {
      // mandatory parameters
      String componentKey = params.get("component");
      if (StringUtils.isBlank(componentKey)) {
        result.addError("Component is not set");
      }
      RuleKey ruleKey = null;
      String rule = params.get("rule");
      if (StringUtils.isBlank(rule)) {
        result.addError(Result.Message.ofL10n("issue.manual.missing_rule"));
      } else {
        ruleKey = RuleKey.parse(rule);
      }

      if (result.ok()) {
        DefaultIssue issue = (DefaultIssue) new DefaultIssueBuilder()
          .componentKey(componentKey)
          .line(RubyUtils.toInteger(params.get("line")))
          .message(params.get("message"))
          .severity(Objects.firstNonNull(params.get("severity"), Severity.MAJOR))
          .effortToFix(RubyUtils.toDouble(params.get("effortToFix")))
          .ruleKey(ruleKey)
          .reporter(UserSession.get().login())
          .build();
        issue = issueService.createManualIssue(issue, UserSession.get());
        result.set(issue);
      }

    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Collection<ActionPlan> findOpenActionPlans(String projectKey) {
    return actionPlanService.findOpenByProjectKey(projectKey, UserSession.get());
  }

  public ActionPlan findActionPlan(String actionPlanKey) {
    return actionPlanService.findByKey(actionPlanKey, UserSession.get());
  }

  public List<ActionPlanStats> findActionPlanStats(String projectKey) {
    return actionPlanService.findActionPlanStats(projectKey, UserSession.get());
  }

  public Result<ActionPlan> createActionPlan(Map<String, String> parameters) {
    Result<ActionPlan> result = createActionPlanResult(parameters);
    if (result.ok()) {
      result.set(actionPlanService.create(result.get(), UserSession.get()));
    }
    return result;
  }

  public Result<ActionPlan> updateActionPlan(String key, Map<String, String> parameters) {
    DefaultActionPlan existingActionPlan = (DefaultActionPlan) actionPlanService.findByKey(key, UserSession.get());
    if (existingActionPlan == null) {
      Result<ActionPlan> result = Result.of();
      result.addError(Result.Message.ofL10n("action_plans.errors.action_plan_does_not_exist", key));
      return result;
    } else {
      Result<ActionPlan> result = createActionPlanResult(parameters, existingActionPlan);
      if (result.ok()) {
        DefaultActionPlan actionPlan = (DefaultActionPlan) result.get();
        actionPlan.setKey(existingActionPlan.key());
        actionPlan.setUserLogin(existingActionPlan.userLogin());
        result.set(actionPlanService.update(actionPlan, UserSession.get()));
      }
      return result;
    }
  }

  public Result<ActionPlan> closeActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.set(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_CLOSED, UserSession.get()));
    }
    return result;
  }

  public Result<ActionPlan> openActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.set(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_OPEN, UserSession.get()));
    }
    return result;
  }

  public Result<ActionPlan> deleteActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      actionPlanService.delete(actionPlanKey, UserSession.get());
    }
    return result;
  }

  @VisibleForTesting
  Result createActionPlanResult(Map<String, String> parameters) {
    return createActionPlanResult(parameters, null);
  }

  @VisibleForTesting
  Result<ActionPlan> createActionPlanResult(Map<String, String> parameters, @Nullable DefaultActionPlan existingActionPlan) {
    Result<ActionPlan> result = Result.of();

    String name = parameters.get("name");
    String description = parameters.get("description");
    String deadLineParam = parameters.get("deadLine");
    String projectParam = parameters.get("project");

    checkMandatorySizeParameter(name, "name", 200, result);
    checkOptionnalSizeParameter(description, "description", 1000, result);

    // Can only set project on creation
    if (existingActionPlan == null) {
      checkProject(projectParam, result);
    }
    Date deadLine = checkAndReturnDeadline(deadLineParam, result);

    if (!Strings.isNullOrEmpty(projectParam) && !Strings.isNullOrEmpty(name) && (existingActionPlan == null || !name.equals(existingActionPlan.name()))
      && actionPlanService.isNameAlreadyUsedForProject(name, projectParam)) {
      result.addError(Result.Message.ofL10n("action_plans.same_name_in_same_project"));
    }

    if (result.ok()) {
      DefaultActionPlan actionPlan = DefaultActionPlan.create(name)
        .setDescription(description)
        .setUserLogin(UserSession.get().login())
        .setDeadLine(deadLine);

      // Can only set project on creation
      if (existingActionPlan == null) {
        actionPlan.setProjectKey(projectParam);
      } else {
        actionPlan.setProjectKey(existingActionPlan.projectKey());
      }

      result.set(actionPlan);
    }
    return result;
  }

  private void checkProject(String projectParam, Result<ActionPlan> result) {
    if (Strings.isNullOrEmpty(projectParam)) {
      result.addError(Result.Message.ofL10n("errors.cant_be_empty", "project"));
    } else {
      ResourceDto project = resourceDao.getResource(ResourceQuery.create().setKey(projectParam));
      if (project == null) {
        result.addError(Result.Message.ofL10n("action_plans.errors.project_does_not_exist", projectParam));
      }
    }
  }

  private Date checkAndReturnDeadline(String deadLineParam, Result<ActionPlan> result) {
    Date deadLine = null;
    if (!Strings.isNullOrEmpty(deadLineParam)) {
      try {
        deadLine = RubyUtils.toDate(deadLineParam);
        Date today = new Date();
        if (deadLine != null && deadLine.before(today) && !org.apache.commons.lang.time.DateUtils.isSameDay(deadLine, today)) {
          result.addError(Result.Message.ofL10n("action_plans.date_cant_be_in_past"));
        }
      } catch (SonarException e) {
        result.addError(Result.Message.ofL10n("errors.is_not_valid", "date"));
      }
    }
    return deadLine;
  }

  private Result<ActionPlan> createResultForExistingActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = Result.of();
    if (findActionPlan(actionPlanKey) == null) {
      result.addError(Result.Message.ofL10n("action_plans.errors.action_plan_does_not_exist", actionPlanKey));
    }
    return result;
  }

  public Result<Issue> executeAction(String issueKey, String actionKey) {
    Result<Issue> result = Result.of();
    try {
      result.set(actionService.execute(issueKey, actionKey, UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public List<Action> listActions(String issueKey) {
    return actionService.listAvailableActions(issueKey);
  }

  public List<Action> listActions(Issue issue) {
    return actionService.listAvailableActions(issue);
  }

  public IssueQuery toQuery(Map<String, Object> props) {
    return PublicRubyIssueService.toQuery(props);
  }

  public DefaultIssueFilter findIssueFilter(Long id) {
    return issueFilterService.findById(id, UserSession.get());
  }

  public List<DefaultIssueFilter> findUserIssueFilters() {
    return issueFilterService.findByUser(UserSession.get());
  }

  public DefaultIssueFilter createFilterFromMap(Map<String, Object> mapData) {
    return new DefaultIssueFilter(mapData);
  }

  /**
   * Execute issue filter from issue query
   */
  public IssueQueryResult execute(IssueQuery issueQuery) {
    return issueFilterService.execute(issueQuery);
  }

  /**
   * Execute issue filter from existing filter
   */
  public IssueQueryResult execute(Long issueFilterId) {
    return issueFilterService.execute(issueFilterId, UserSession.get());
  }

  /**
   * List user issue filter
   */
  public List<DefaultIssueFilter> findIssueFiltersForCurrentUser() {
    return issueFilterService.findByUser(UserSession.get());
  }

  /**
   * Create issue filter
   */
  public Result<DefaultIssueFilter> createIssueFilter(Map<String, String> parameters) {
    Result<DefaultIssueFilter> result = createIssueFilterResult(parameters, false);
    if (result.ok()) {
      try {
        result.set(issueFilterService.save(result.get(), UserSession.get()));
      } catch (Exception e) {
        result.addError(e.getMessage());
      }
    }
    return result;
  }

  /**
   * Update issue filter
   */
  public Result<DefaultIssueFilter> updateIssueFilter(Map<String, String> parameters) {
    Result<DefaultIssueFilter> result = createIssueFilterResult(parameters, true);
    if (result.ok()) {
      try {
        result.set(issueFilterService.update(result.get(), UserSession.get()));
      } catch (Exception e) {
        result.addError(e.getMessage());
      }
    }
    return result;
  }

  /**
   * Update issue filter data
   */
  public Result<DefaultIssueFilter> updateIssueFilterData(Long issueFilterId, Map<String, Object> data) {
    Result<DefaultIssueFilter> result = Result.of();
    try {
      result.set(issueFilterService.updateData(issueFilterId, data, UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  /**
   * Delete issue filter
   */
  public Result<DefaultIssueFilter> deleteIssueFilter(Long issueFilterId) {
    Result<DefaultIssueFilter> result = Result.of();
    try {
      issueFilterService.delete(issueFilterId, UserSession.get());
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  /**
   * Copy issue filter
   */
  public Result<DefaultIssueFilter> copyIssueFilter(Long issueFilterIdToCopy, Map<String, String> parameters) {
    Result<DefaultIssueFilter> result = createIssueFilterResult(parameters, false);
    if (result.ok()) {
      try {
        result.set(issueFilterService.copy(issueFilterIdToCopy, result.get(), UserSession.get()));
      } catch (Exception e) {
        result.addError(e.getMessage());
      }
    }
    return result;
  }

  @VisibleForTesting
  Result<DefaultIssueFilter> createIssueFilterResult(Map<String, String> params, boolean isUpdate) {
    Result<DefaultIssueFilter> result = Result.of();

    String id = params.get("id");
    String name = params.get("name");
    String description = params.get("description");
    String data = params.get("data");
    Boolean shared = RubyUtils.toBoolean(params.get("shared"));

    if (isUpdate) {
      checkMandatoryParameter(id, "id", result);
    }
    checkMandatorySizeParameter(name, "name", 100, result);
    checkOptionnalSizeParameter(description, "description", 4000, result);

    if (result.ok()) {
      DefaultIssueFilter defaultIssueFilter = DefaultIssueFilter.create(name)
        .setDescription(description)
        .setShared(shared)
        .setData(data);
      if (isUpdate) {
        defaultIssueFilter.setId(Long.valueOf(id));
      }

      result.set(defaultIssueFilter);
    }
    return result;
  }

  public List<DefaultIssueFilter> findFavouriteIssueFiltersForCurrentUser() {
    return issueFilterService.findFavoriteFilters(UserSession.get());
  }

  public Result toggleFavouriteIssueFilter(Long issueFilterId) {
    Result result = Result.of();
    try {
      issueFilterService.toggleFavouriteIssueFilter(issueFilterId, UserSession.get());
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  private void checkMandatoryParameter(String value, String paramName, Result result) {
    if (Strings.isNullOrEmpty(value)) {
      result.addError(Result.Message.ofL10n("errors.cant_be_empty", paramName));
    }
  }

  private void checkMandatorySizeParameter(String value, String paramName, Integer size, Result result) {
    checkMandatoryParameter(value, paramName, result);
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      result.addError(Result.Message.ofL10n("errors.is_too_long", paramName, size));
    }
  }

  private void checkOptionnalSizeParameter(String value, String paramName, Integer size, Result result) {
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      result.addError(Result.Message.ofL10n("errors.is_too_long", paramName, size));
    }
  }

}