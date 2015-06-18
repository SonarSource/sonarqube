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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.user.User;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.issue.filter.IssueFilterService;
import org.sonar.server.issue.ws.IssueComponentHelper;
import org.sonar.server.issue.ws.IssueJsonWriter;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.ws.UserJsonWriter;
import org.sonar.server.util.RubyUtils;
import org.sonar.server.util.Validation;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Used through ruby code <pre>Internal.issues</pre>
 *
 * All the issue features that are not published to public API.
 *
 * @since 3.6
 */
@ServerSide
public class InternalRubyIssueService {

  private static final String ID_PARAM = "id";
  private static final String NAME_PARAM = "name";
  private static final String DESCRIPTION_PARAM = "description";
  private static final String PROJECT_PARAM = "project";
  private static final String USER_PARAM = "user";

  private static final String ACTION_PLANS_ERRORS_ACTION_PLAN_DOES_NOT_EXIST_MESSAGE = "action_plans.errors.action_plan_does_not_exist";

  private final IssueService issueService;
  private final IssueQueryService issueQueryService;
  private final IssueCommentService commentService;
  private final IssueChangelogService changelogService;
  private final ActionPlanService actionPlanService;
  private final ResourceDao resourceDao;
  private final ActionService actionService;
  private final IssueFilterService issueFilterService;
  private final IssueBulkChangeService issueBulkChangeService;
  private final IssueJsonWriter issueWriter;
  private final IssueComponentHelper issueComponentHelper;
  private final UserIndex userIndex;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;

  public InternalRubyIssueService(
    IssueService issueService,
    IssueQueryService issueQueryService,
    IssueCommentService commentService,
    IssueChangelogService changelogService, ActionPlanService actionPlanService,
    ResourceDao resourceDao, ActionService actionService,
    IssueFilterService issueFilterService, IssueBulkChangeService issueBulkChangeService,
    IssueJsonWriter issueWriter, IssueComponentHelper issueComponentHelper, UserIndex userIndex, DbClient dbClient,
    UserSession userSession, UserJsonWriter userWriter) {
    this.issueService = issueService;
    this.issueQueryService = issueQueryService;
    this.commentService = commentService;
    this.changelogService = changelogService;
    this.actionPlanService = actionPlanService;
    this.resourceDao = resourceDao;
    this.actionService = actionService;
    this.issueFilterService = issueFilterService;
    this.issueBulkChangeService = issueBulkChangeService;
    this.issueWriter = issueWriter;
    this.issueComponentHelper = issueComponentHelper;
    this.userIndex = userIndex;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.userWriter = userWriter;
  }

  public Issue getIssueByKey(String issueKey) {
    return issueService.getByKey(issueKey);
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
    return changelogService.changelog(issueKey);
  }

  public IssueChangelog changelog(Issue issue) {
    return changelogService.changelog(issue);
  }

  public List<String> formatChangelog(FieldDiffs diffs) {
    return changelogService.formatDiffs(diffs);
  }

  public List<String> listPluginActions() {
    return newArrayList(Iterables.transform(actionService.listAllActions(), new Function<Action, String>() {
      @Override
      public String apply(Action input) {
        return input.key();
      }
    }));
  }

  public List<DefaultIssueComment> findComments(String issueKey) {
    return commentService.findComments(issueKey);
  }

  public List<DefaultIssueComment> findCommentsByIssueKeys(Collection<String> issueKeys) {
    return commentService.findComments(issueKeys);
  }

  public Result<Issue> doTransition(String issueKey, String transitionKey) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.doTransition(issueKey, transitionKey));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<Issue> assign(String issueKey, @Nullable String assignee) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.assign(issueKey, StringUtils.defaultIfBlank(assignee, null)));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<Issue> setSeverity(String issueKey, String severity) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.setSeverity(issueKey, severity));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<Issue> plan(String issueKey, @Nullable String actionPlanKey) {
    Result<Issue> result = Result.of();
    try {
      result.set(issueService.plan(issueKey, actionPlanKey));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Result<IssueComment> addComment(String issueKey, String text) {
    Result<IssueComment> result = Result.of();
    try {
      result.set(commentService.addComment(issueKey, text, userSession));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public IssueComment deleteComment(String commentKey) {
    return commentService.deleteComment(commentKey, userSession);
  }

  public Result<IssueComment> editComment(String commentKey, String newText) {
    Result<IssueComment> result = Result.of();
    try {
      result.set(commentService.editComment(commentKey, newText, userSession));
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
        DefaultIssue issue = issueService.createManualIssue(componentKey, ruleKey, RubyUtils.toInteger(params.get("line")), params.get("message"), params.get("severity"),
          RubyUtils.toDouble(params.get("effortToFix")));
        result.set(issue);
      }

    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Collection<ActionPlan> findOpenActionPlans(String projectKey) {
    return actionPlanService.findOpenByProjectKey(projectKey, userSession);
  }

  public ActionPlan findActionPlan(String actionPlanKey) {
    return actionPlanService.findByKey(actionPlanKey, userSession);
  }

  public List<ActionPlanStats> findActionPlanStats(String projectKey) {
    return actionPlanService.findActionPlanStats(projectKey, userSession);
  }

  public Result<ActionPlan> createActionPlan(Map<String, String> parameters) {
    Result<ActionPlan> result = createActionPlanResult(parameters);
    if (result.ok()) {
      result.set(actionPlanService.create(result.get(), userSession));
    }
    return result;
  }

  public Result<ActionPlan> updateActionPlan(String key, Map<String, String> parameters) {
    DefaultActionPlan existingActionPlan = (DefaultActionPlan) actionPlanService.findByKey(key, userSession);
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
        result.set(actionPlanService.update(actionPlan, userSession));
      }
      return result;
    }
  }

  public Result<ActionPlan> closeActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.set(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_CLOSED, userSession));
    }
    return result;
  }

  public Result<ActionPlan> openActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.set(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_OPEN, userSession));
    }
    return result;
  }

  public Result<ActionPlan> deleteActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      actionPlanService.delete(actionPlanKey, userSession);
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
        .setUserLogin(userSession.getLogin())
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
      result.addError(Result.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, PROJECT_PARAM));
    } else {
      ResourceDto project = resourceDao.getResource(ResourceQuery.create().setKey(projectParam));
      if (project == null) {
        result.addError(Result.Message.ofL10n("action_plans.errors.project_does_not_exist", projectParam));
      }
    }
  }

  private static Date checkAndReturnDeadline(String deadLineParam, Result<ActionPlan> result) {
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
      result.set(actionService.execute(issueKey, actionKey, userSession));
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
    return issueQueryService.createFromMap(Maps.<String, Object>newHashMap());
  }

  @CheckForNull
  public IssueFilterDto findIssueFilterById(Long id) {
    return issueFilterService.findById(id);
  }

  /**
   * Return the issue filter if the user has the right to see it
   * Never return null
   */
  public IssueFilterDto findIssueFilter(Long id) {
    return issueFilterService.find(id, userSession);
  }

  public boolean isUserAuthorized(IssueFilterDto issueFilter) {
    try {
      String user = issueFilterService.getLoggedLogin(userSession);
      issueFilterService.verifyCurrentUserCanReadFilter(issueFilter, user);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean canUserShareIssueFilter() {
    return issueFilterService.canShareFilter(userSession);
  }

  public String serializeFilterQuery(Map<String, Object> filterQuery) {
    return issueFilterService.serializeFilterQuery(filterQuery);
  }

  public Map<String, Object> deserializeFilterQuery(IssueFilterDto issueFilter) {
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
  public IssueFilterService.IssueFilterResult execute(Map<String, Object> props) {
    return issueFilterService.execute(issueQueryService.createFromMap(props), toSearchOptions(props));
  }

  /**
   * Execute issue filter from existing filter with optional overridable parameters
   */
  public IssueFilterService.IssueFilterResult execute(Long issueFilterId, Map<String, Object> overrideProps) {
    IssueFilterDto issueFilter = issueFilterService.find(issueFilterId, userSession);
    Map<String, Object> props = issueFilterService.deserializeIssueFilterQuery(issueFilter);
    overrideProps(props, overrideProps);
    return execute(props);
  }

  private static void overrideProps(Map<String, Object> props, Map<String, Object> overrideProps) {
    for (Map.Entry<String, Object> entry : overrideProps.entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * List user issue filter
   */
  public List<IssueFilterDto> findIssueFiltersForCurrentUser() {
    return issueFilterService.findByUser(userSession);
  }

  /**
   * Create issue filter
   */
  public IssueFilterDto createIssueFilter(Map<String, String> parameters) {
    IssueFilterDto result = createIssueFilterResultForNew(parameters);
    return issueFilterService.save(result, userSession);
  }

  /**
   * Update issue filter
   */
  public IssueFilterDto updateIssueFilter(Map<String, String> parameters) {
    IssueFilterDto result = createIssueFilterResultForUpdate(parameters);
    return issueFilterService.update(result, userSession);
  }

  /**
   * Update issue filter data
   */
  public IssueFilterDto updateIssueFilterQuery(Long issueFilterId, Map<String, Object> data) {
    return issueFilterService.updateFilterQuery(issueFilterId, data, userSession);
  }

  /**
   * Delete issue filter
   */
  public void deleteIssueFilter(Long issueFilterId) {
    issueFilterService.delete(issueFilterId, userSession);
  }

  /**
   * Copy issue filter
   */
  public IssueFilterDto copyIssueFilter(Long issueFilterIdToCopy, Map<String, String> parameters) {
    IssueFilterDto result = createIssueFilterResultForCopy(parameters);
    return issueFilterService.copy(issueFilterIdToCopy, result, userSession);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResultForNew(Map<String, String> params) {
    return createIssueFilterResult(params, false, false);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResultForUpdate(Map<String, String> params) {
    return createIssueFilterResult(params, true, true);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResultForCopy(Map<String, String> params) {
    return createIssueFilterResult(params, false, false);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResult(Map<String, String> params, boolean checkId, boolean checkUser) {
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

    IssueFilterDto issueFilterDto = new IssueFilterDto()
      .setName(name)
      .setDescription(description)
      .setShared(shared)
      .setUserLogin(user)
      .setData(data);
    if (!Strings.isNullOrEmpty(id)) {
      issueFilterDto.setId(Long.valueOf(id));
    }
    return issueFilterDto;
  }

  public List<IssueFilterDto> findSharedFiltersForCurrentUser() {
    return issueFilterService.findSharedFiltersWithoutUserFilters(userSession);
  }

  public List<IssueFilterDto> findFavouriteIssueFiltersForCurrentUser() {
    return issueFilterService.findFavoriteFilters(userSession);
  }

  public boolean toggleFavouriteIssueFilter(Long issueFilterId) {
    return issueFilterService.toggleFavouriteIssueFilter(issueFilterId, userSession);
  }

  /**
   * Execute a bulk change
   */
  public IssueBulkChangeResult bulkChange(Map<String, Object> props, String comment, boolean sendNotifications) {
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(props, comment, sendNotifications);
    return issueBulkChangeService.execute(issueBulkChangeQuery, userSession);
  }

  private static void checkMandatoryParameter(String value, String paramName, Result result) {
    if (Strings.isNullOrEmpty(value)) {
      result.addError(Result.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, paramName));
    }
  }

  private static void checkMandatorySizeParameter(String value, String paramName, Integer size, Result result) {
    checkMandatoryParameter(value, paramName, result);
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      result.addError(Result.Message.ofL10n(Validation.IS_TOO_LONG_MESSAGE, paramName, size));
    }
  }

  private static void checkOptionalSizeParameter(String value, String paramName, Integer size, Result result) {
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      result.addError(Result.Message.ofL10n(Validation.IS_TOO_LONG_MESSAGE, paramName, size));
    }
  }

  private static void checkOptionalSizeParameter(String value, String paramName, Integer size) {
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      throw new BadRequestException(Validation.IS_TOO_LONG_MESSAGE, paramName, size);
    }
  }

  /**
   * Do not make this method static as it's called by rails
   */
  public int maxPageSize() {
    return QueryContext.MAX_LIMIT;
  }

  @VisibleForTesting
  static SearchOptions toSearchOptions(Map<String, Object> props) {
    SearchOptions options = new SearchOptions();
    Integer pageIndex = RubyUtils.toInteger(props.get(IssueFilterParameters.PAGE_INDEX));
    Integer pageSize = RubyUtils.toInteger(props.get(IssueFilterParameters.PAGE_SIZE));
    if (pageSize != null && pageSize < 0) {
      options.setLimit(SearchOptions.MAX_LIMIT);
    } else {
      options.setPage(pageIndex != null ? pageIndex : 1, pageSize != null ? pageSize : 100);
    }
    return options;
  }

  public Collection<String> listTags() {
    return issueService.listTags(null, 0);
  }

  public Map<String, Long> listTagsForComponent(String componentUuid, int pageSize) {
    IssueQuery query = issueQueryService.createFromMap(
      ImmutableMap.<String, Object>of(
        "componentUuids", componentUuid,
        "resolved", false));
    return issueService.listTagsForComponent(query, pageSize);
  }

  public boolean isUserIssueAdmin(String projectUuid) {
    return userSession.hasProjectPermissionByUuid(UserRole.ISSUE_ADMIN, projectUuid);
  }

  /**
   * Used by issue modification actions currently implemented in Rails
   * @param issue
   * @return the JSON representation of the modified issue, as a ready to use string
   */
  public String writeIssueJson(@Nullable Issue issue) {
    if (issue == null) {
      return "{}";
    }

    StringWriter writer = new StringWriter();
    JsonWriter json = JsonWriter.of(writer);
    DbSession dbSession = dbClient.openSession(false);
    try {
      Map<String, User> usersByLogin = getIssueUsersByLogin(issue);

      Set<String> componentUuids = ImmutableSet.of(issue.componentUuid());
      Set<String> projectUuids = Sets.newHashSet();
      Set<ComponentDto> componentDtos = Sets.newHashSet();
      List<ComponentDto> projectDtos = Lists.newArrayList();

      Map<String, ComponentDto> componentsByUuid = Maps.newHashMap();
      Map<String, ComponentDto> projectsByComponentUuid = Maps.newHashMap();

      List<ComponentDto> fileDtos = dbClient.componentDao().selectByUuids(dbSession, componentUuids);
      List<ComponentDto> subProjectDtos = dbClient.componentDao().selectSubProjectsByComponentUuids(dbSession, componentUuids);
      componentDtos.addAll(fileDtos);
      componentDtos.addAll(subProjectDtos);
      for (ComponentDto component : componentDtos) {
        projectUuids.add(component.projectUuid());
      }
      projectDtos.addAll(dbClient.componentDao().selectByUuids(dbSession, projectUuids));
      componentDtos.addAll(projectDtos);

      for (ComponentDto componentDto : componentDtos) {
        componentsByUuid.put(componentDto.uuid(), componentDto);
      }

      projectsByComponentUuid = issueComponentHelper.prepareComponentsAndProjects(projectUuids, componentUuids, componentsByUuid, componentDtos, subProjectDtos, dbSession);

      json.beginObject().name("issue");
      issueWriter.write(json, issue,
        usersByLogin,
        componentsByUuid,
        projectsByComponentUuid,
        ImmutableMultimap.<String, DefaultIssueComment>of(),
        ImmutableMap.<String, ActionPlan>of(),
        ImmutableList.of(IssueJsonWriter.ACTIONS_EXTRA_FIELD, IssueJsonWriter.TRANSITIONS_EXTRA_FIELD));

      json.name("users").beginArray();
      String assignee = issue.assignee();
      if (assignee != null && usersByLogin.containsKey(assignee)) {
        userWriter.write(json, usersByLogin.get(assignee));
      }
      json.endArray();

      json.endObject().close();
    } finally {
      MyBatis.closeQuietly(dbSession);
      IOUtils.closeQuietly(writer);
    }
    return writer.toString();
  }

  private Map<String, User> getIssueUsersByLogin(Issue issue) {
    Map<String, User> usersByLogin = Maps.newHashMap();
    String assignee = issue.assignee();
    if (assignee != null) {
      usersByLogin.put(assignee, userIndex.getByLogin(assignee));
    }
    String reporter = issue.reporter();
    if (reporter != null) {
      usersByLogin.put(reporter, userIndex.getByLogin(reporter));
    }
    return usersByLogin;
  }
}
