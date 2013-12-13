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
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
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
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * All the issue features that are not published to public API.
 *
 * @since 3.6
 */
public class InternalRubyIssueService implements ServerComponent {

  private static final String ID_PARAM = "id";
  private static final String NAME_PARAM = "name";
  private static final String DESCRIPTION_PARAM = "description";
  private static final String PROJECT_PARAM = "project";
  private static final String USER_PARAM = "user";

  private static final String ACTION_PLANS_ERRORS_ACTION_PLAN_DOES_NOT_EXIST_MESSAGE = "action_plans.errors.action_plan_does_not_exist";

  private final IssueService issueService;
  private final IssueCommentService commentService;
  private final IssueChangelogService changelogService;
  private final ActionPlanService actionPlanService;
  private final IssueStatsFinder issueStatsFinder;
  private final ResourceDao resourceDao;
  private final ActionService actionService;
  private final IssueFilterService issueFilterService;
  private final IssueBulkChangeService issueBulkChangeService;
  private final IssueChangelogFormatter issueChangelogFormatter;

  public InternalRubyIssueService(IssueService issueService,
    IssueCommentService commentService,
    IssueChangelogService changelogService, ActionPlanService actionPlanService,
    IssueStatsFinder issueStatsFinder, ResourceDao resourceDao, ActionService actionService,
    IssueFilterService issueFilterService, IssueBulkChangeService issueBulkChangeService, IssueChangelogFormatter issueChangelogFormatter) {
    this.issueService = issueService;
    this.commentService = commentService;
    this.changelogService = changelogService;
    this.actionPlanService = actionPlanService;
    this.issueStatsFinder = issueStatsFinder;
    this.resourceDao = resourceDao;
    this.actionService = actionService;
    this.issueFilterService = issueFilterService;
    this.issueBulkChangeService = issueBulkChangeService;
    this.issueChangelogFormatter = issueChangelogFormatter;
  }

  public IssueStatsFinder.IssueStatsResult findIssueAssignees(Map<String, Object> params) {
    return issueStatsFinder.findIssueAssignees(PublicRubyIssueService.toQuery(params));
  }

  public List<Transition> listTransitions(String issueKey) {
    return issueService.listTransitions(issueKey, UserSession.get());
  }

  public List<Transition> listTransitions(Issue issue) {
    return issueService.listTransitions(issue, UserSession.get());
  }

  public List<String> listStatus() {
    return issueService.listStatus();
  }

  public List<String> listResolutions() {
    return Issue.RESOLUTIONS;
  }

  public IssueChangelog changelog(String issueKey) {
    return changelogService.changelog(issueKey);
  }

  public IssueChangelog changelog(Issue issue) {
    return changelogService.changelog(issue);
  }

  public List<String> formatChangelog(FieldDiffs diffs) {
    return issueChangelogFormatter.format(UserSession.get().locale(), diffs);
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

  public Result<IssueComment> editComment(String commentKey, String newText) {
    Result<IssueComment> result = Result.of();
    try {
      result.set(commentService.editComment(commentKey, newText, UserSession.get()));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
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
      result.addError(Result.Message.ofL10n(ACTION_PLANS_ERRORS_ACTION_PLAN_DOES_NOT_EXIST_MESSAGE, key));
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

    String name = parameters.get(NAME_PARAM);
    String description = parameters.get(DESCRIPTION_PARAM);
    String deadLineParam = parameters.get("deadLine");
    String projectParam = parameters.get(PROJECT_PARAM);

    checkMandatorySizeParameter(name, NAME_PARAM, 200, result);
    checkOptionalSizeParameter(description, DESCRIPTION_PARAM, 1000, result);

    // Can only set project on creation
    if (existingActionPlan == null) {
      checkProject(projectParam, result);
    }
    Date deadLine = checkAndReturnDeadline(deadLineParam, result);

    // TODO move this check in the service, on creation and update
    if (!Strings.isNullOrEmpty(projectParam) && !Strings.isNullOrEmpty(name) && isActionPlanNameAvailable(existingActionPlan, name, projectParam)) {
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

  private boolean isActionPlanNameAvailable(@Nullable DefaultActionPlan existingActionPlan, String name, String projectParam) {
    return (existingActionPlan == null || !name.equals(existingActionPlan.name())) && actionPlanService.isNameAlreadyUsedForProject(name, projectParam);
  }

  private void checkProject(String projectParam, Result<ActionPlan> result) {
    if (Strings.isNullOrEmpty(projectParam)) {
      result.addError(Result.Message.ofL10n(Validation.ERRORS_CANT_BE_EMPTY_MESSAGE, PROJECT_PARAM));
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
      result.addError(Result.Message.ofL10n(ACTION_PLANS_ERRORS_ACTION_PLAN_DOES_NOT_EXIST_MESSAGE, actionPlanKey));
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

  public IssueQuery emptyIssueQuery() {
    return PublicRubyIssueService.toQuery(Maps.<String, Object>newHashMap());
  }

  @CheckForNull
  public DefaultIssueFilter findIssueFilterById(Long id) {
    return issueFilterService.findById(id);
  }

  /**
   * Return the issue filter if the user has the right to see it
   * Never return null
   */
  public DefaultIssueFilter findIssueFilter(Long id) {
    return issueFilterService.find(id, UserSession.get());
  }

  public List<DefaultIssueFilter> findUserIssueFilters() {
    return issueFilterService.findByUser(UserSession.get());
  }

  public boolean isUserAuthorized(DefaultIssueFilter issueFilter) {
    try {
      UserSession userSession = UserSession.get();
      String user = issueFilterService.getLoggedLogin(userSession);
      issueFilterService.verifyCurrentUserCanReadFilter(issueFilter, user);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean canUserShareIssueFilter() {
    return issueFilterService.canShareFilter(UserSession.get());
  }

  public String serializeFilterQuery(Map<String, Object> filterQuery) {
    return issueFilterService.serializeFilterQuery(filterQuery);
  }

  public Map<String, Object> deserializeFilterQuery(DefaultIssueFilter issueFilter) {
    return issueFilterService.deserializeIssueFilterQuery(issueFilter);
  }

  public Map<String, Object> sanitizeFilterQuery(Map<String, Object> filterQuery) {
    return Maps.filterEntries(filterQuery, new Predicate<Map.Entry<String, Object>>() {
      @Override
      public boolean apply(Map.Entry<String, Object> input) {
        return IssueFilterParameters.ALL.contains(input.getKey());
      }
    });
  }

  /**
   * Execute issue filter from parameters
   */
  public IssueFilterResult execute(Map<String, Object> props) {
    IssueQuery issueQuery = PublicRubyIssueService.toQuery(props);
    return issueFilterService.execute(issueQuery);
  }

  /**
   * Execute issue filter from existing filter with optional overridable parameters
   */
  public IssueFilterResult execute(Long issueFilterId, Map<String, Object> overrideProps) {
    DefaultIssueFilter issueFilter = issueFilterService.find(issueFilterId, UserSession.get());
    Map<String, Object> props = issueFilterService.deserializeIssueFilterQuery(issueFilter);
    overrideProps(props, overrideProps);
    IssueQuery issueQuery = PublicRubyIssueService.toQuery(props);
    return issueFilterService.execute(issueQuery);
  }

  private void overrideProps(Map<String, Object> props, Map<String, Object> overrideProps) {
    for (Map.Entry<String, Object> entry : overrideProps.entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
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
  public DefaultIssueFilter createIssueFilter(Map<String, String> parameters) {
    DefaultIssueFilter result = createIssueFilterResultForNew(parameters);
    return issueFilterService.save(result, UserSession.get());
  }

  /**
   * Update issue filter
   */
  public DefaultIssueFilter updateIssueFilter(Map<String, String> parameters) {
    DefaultIssueFilter result = createIssueFilterResultForUpdate(parameters);
    return issueFilterService.update(result, UserSession.get());
  }

  /**
   * Update issue filter data
   */
  public DefaultIssueFilter updateIssueFilterQuery(Long issueFilterId, Map<String, Object> data) {
    return issueFilterService.updateFilterQuery(issueFilterId, data, UserSession.get());
  }

  /**
   * Delete issue filter
   */
  public void deleteIssueFilter(Long issueFilterId) {
    issueFilterService.delete(issueFilterId, UserSession.get());
  }

  /**
   * Copy issue filter
   */
  public DefaultIssueFilter copyIssueFilter(Long issueFilterIdToCopy, Map<String, String> parameters) {
    DefaultIssueFilter result = createIssueFilterResultForCopy(parameters);
    return issueFilterService.copy(issueFilterIdToCopy, result, UserSession.get());
  }

  @VisibleForTesting
  DefaultIssueFilter createIssueFilterResultForNew(Map<String, String> params) {
    return createIssueFilterResult(params, false, false);
  }

  @VisibleForTesting
  DefaultIssueFilter createIssueFilterResultForUpdate(Map<String, String> params) {
    return createIssueFilterResult(params, true, true);
  }

  @VisibleForTesting
  DefaultIssueFilter createIssueFilterResultForCopy(Map<String, String> params) {
    return createIssueFilterResult(params, false, false);
  }

  @VisibleForTesting
  DefaultIssueFilter createIssueFilterResult(Map<String, String> params, boolean checkId, boolean checkUser) {
    String id = params.get(ID_PARAM);
    String name = params.get(NAME_PARAM);
    String description = params.get(DESCRIPTION_PARAM);
    String data = params.get("data");
    String user = params.get(USER_PARAM);
    Boolean sharedParam = RubyUtils.toBoolean(params.get("shared"));
    boolean shared = sharedParam != null ? sharedParam : false;

    if (checkId) {
      Validation.checkMandatoryParameter(id, ID_PARAM);
    }
    if (checkUser) {
      Validation.checkMandatoryParameter(user, USER_PARAM);
    }
    Validation.checkMandatorySizeParameter(name, NAME_PARAM, 100);
    checkOptionalSizeParameter(description, DESCRIPTION_PARAM, 4000);

    DefaultIssueFilter defaultIssueFilter = DefaultIssueFilter.create(name)
      .setDescription(description)
      .setShared(shared)
      .setUser(user)
      .setData(data);
    if (!Strings.isNullOrEmpty(id)) {
      defaultIssueFilter.setId(Long.valueOf(id));
    }
    return defaultIssueFilter;
  }

  public List<DefaultIssueFilter> findSharedFiltersForCurrentUser() {
    return issueFilterService.findSharedFiltersWithoutUserFilters(UserSession.get());
  }

  public List<DefaultIssueFilter> findFavouriteIssueFiltersForCurrentUser() {
    return issueFilterService.findFavoriteFilters(UserSession.get());
  }

  public boolean toggleFavouriteIssueFilter(Long issueFilterId) {
    return issueFilterService.toggleFavouriteIssueFilter(issueFilterId, UserSession.get());
  }

  /**
   * Execute a bulk change
   */
  public IssueBulkChangeResult bulkChange(Map<String, Object> props, String comment, boolean sendNotifications) {
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(props, comment, sendNotifications);
    return issueBulkChangeService.execute(issueBulkChangeQuery, UserSession.get());
  }

  private void checkMandatoryParameter(String value, String paramName, Result result) {
    if (Strings.isNullOrEmpty(value)) {
      result.addError(Result.Message.ofL10n(Validation.ERRORS_CANT_BE_EMPTY_MESSAGE, paramName));
    }
  }

  private void checkMandatorySizeParameter(String value, String paramName, Integer size, Result result) {
    checkMandatoryParameter(value, paramName, result);
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      result.addError(Result.Message.ofL10n(Validation.ERRORS_IS_TOO_LONG_MESSAGE, paramName, size));
    }
  }

  private void checkOptionalSizeParameter(String value, String paramName, Integer size, Result result) {
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      result.addError(Result.Message.ofL10n(Validation.ERRORS_IS_TOO_LONG_MESSAGE, paramName, size));
    }
  }

  private void checkOptionalSizeParameter(String value, String paramName, Integer size) {
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      throw BadRequestException.ofL10n(Validation.ERRORS_IS_TOO_LONG_MESSAGE, paramName, size);
    }
  }

}
