/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.ws;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.Action;
import org.sonar.server.issue.ActionContext;
import org.sonar.server.issue.AddTagsAction;
import org.sonar.server.issue.AssignAction;
import org.sonar.server.issue.IssueChangePostProcessor;
import org.sonar.server.issue.RemoveTagsAction;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Issues;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.issue.DefaultTransitions.REOPEN;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.issue.AbstractChangeTagsAction.TAGS_PARAMETER;
import static org.sonar.server.issue.AssignAction.ASSIGNEE_PARAMETER;
import static org.sonar.server.issue.CommentAction.COMMENT_KEY;
import static org.sonar.server.issue.CommentAction.COMMENT_PROPERTY;
import static org.sonar.server.issue.SetSeverityAction.SET_SEVERITY_KEY;
import static org.sonar.server.issue.SetSeverityAction.SEVERITY_PARAMETER;
import static org.sonar.server.issue.SetTypeAction.SET_TYPE_KEY;
import static org.sonar.server.issue.SetTypeAction.TYPE_PARAMETER;
import static org.sonar.server.issue.TransitionAction.DO_TRANSITION_KEY;
import static org.sonar.server.issue.TransitionAction.TRANSITION_PARAMETER;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_BULK_CHANGE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADD_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMMENT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DO_TRANSITION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_REMOVE_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEND_NOTIFICATIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SET_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SET_TYPE;

public class BulkChangeAction implements IssuesWsAction {

  private static final Logger LOG = Loggers.get(BulkChangeAction.class);

  private final System2 system2;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final WebIssueStorage issueStorage;
  private final NotificationManager notificationService;
  private final List<Action> actions;
  private final IssueChangePostProcessor issueChangePostProcessor;
  private final IssuesChangesNotificationSerializer notificationSerializer;

  public BulkChangeAction(System2 system2, UserSession userSession, DbClient dbClient, WebIssueStorage issueStorage,
    NotificationManager notificationService, List<Action> actions,
    IssueChangePostProcessor issueChangePostProcessor, IssuesChangesNotificationSerializer notificationSerializer) {
    this.system2 = system2;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueStorage = issueStorage;
    this.notificationService = notificationService;
    this.actions = actions;
    this.issueChangePostProcessor = issueChangePostProcessor;
    this.notificationSerializer = notificationSerializer;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_BULK_CHANGE)
      .setDescription("Bulk change on issues.<br/>" +
        "Requires authentication.")
      .setSince("3.7")
      .setChangelog(
        new Change("6.3", "'actions' parameter is ignored"))
      .setHandler(this)
      .setResponseExample(getClass().getResource("bulk_change-example.json"))
      .setPost(true);

    action.createParam(PARAM_ISSUES)
      .setDescription("Comma-separated list of issue keys")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01 + "," + UUID_EXAMPLE_02);
    action.createParam(PARAM_ASSIGN)
      .setDescription("To assign the list of issues to a specific user (login), or un-assign all the issues")
      .setExampleValue("john.smith")
      .setDeprecatedKey("assign.assignee", "6.2");
    action.createParam(PARAM_SET_SEVERITY)
      .setDescription("To change the severity of the list of issues")
      .setExampleValue(BLOCKER)
      .setPossibleValues(Severity.ALL)
      .setDeprecatedKey("set_severity.severity", "6.2");
    action.createParam(PARAM_SET_TYPE)
      .setDescription("To change the type of the list of issues")
      .setExampleValue(BUG)
      .setPossibleValues(RuleType.names())
      .setSince("5.5")
      .setDeprecatedKey("set_type.type", "6.2");
    action.createParam(PARAM_DO_TRANSITION)
      .setDescription("Transition")
      .setExampleValue(REOPEN)
      .setPossibleValues(DefaultTransitions.ALL)
      .setDeprecatedKey("do_transition.transition", "6.2");
    action.createParam(PARAM_ADD_TAGS)
      .setDescription("Add tags")
      .setExampleValue("security,java8")
      .setDeprecatedKey("add_tags.tags", "6.2");
    action.createParam(PARAM_REMOVE_TAGS)
      .setDescription("Remove tags")
      .setExampleValue("security,java8")
      .setDeprecatedKey("remove_tags.tags", "6.2");
    action.createParam(PARAM_COMMENT)
      .setDescription("To add a comment to a list of issues")
      .setExampleValue("Here is my comment");
    action.createParam(PARAM_SEND_NOTIFICATIONS)
      .setSince("4.0")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      BulkChangeResult result = executeBulkChange(dbSession, request);
      writeProtobuf(toWsResponse(result), request, response);
    }
  }

  private BulkChangeResult executeBulkChange(DbSession dbSession, Request request) {
    BulkChangeData bulkChangeData = new BulkChangeData(dbSession, request);
    BulkChangeResult result = new BulkChangeResult(bulkChangeData.issues.size());
    IssueChangeContext issueChangeContext = IssueChangeContext.createUser(new Date(system2.now()), userSession.getUuid());

    List<DefaultIssue> items = bulkChangeData.issues.stream()
      .filter(bulkChange(issueChangeContext, bulkChangeData, result))
      .collect(MoreCollectors.toList());
    issueStorage.save(dbSession, items);

    refreshLiveMeasures(dbSession, bulkChangeData, result);

    Set<String> assigneeUuids = items.stream().map(DefaultIssue::assignee).filter(Objects::nonNull).collect(Collectors.toSet());
    Map<String, UserDto> userDtoByUuid = dbClient.userDao().selectByUuids(dbSession, assigneeUuids).stream().collect(toMap(UserDto::getUuid, u -> u));
    String authorUuid = requireNonNull(userSession.getUuid(), "User uuid cannot be null");
    UserDto author = dbClient.userDao().selectByUuid(dbSession, authorUuid);
    checkState(author != null, "User with uuid '%s' does not exist");
    sendNotification(items, bulkChangeData, userDtoByUuid, author);

    return result;
  }

  private void refreshLiveMeasures(DbSession dbSession, BulkChangeData data, BulkChangeResult result) {
    if (!data.shouldRefreshMeasures()) {
      return;
    }
    Set<String> touchedComponentUuids = result.success.stream()
      .map(DefaultIssue::componentUuid)
      .collect(Collectors.toSet());
    List<ComponentDto> touchedComponents = touchedComponentUuids.stream()
      .map(data.componentsByUuid::get)
      .collect(MoreCollectors.toList(touchedComponentUuids.size()));

    List<DefaultIssue> changedIssues = data.issues.stream().filter(result.success::contains).collect(MoreCollectors.toList());
    issueChangePostProcessor.process(dbSession, changedIssues, touchedComponents);
  }

  private static Predicate<DefaultIssue> bulkChange(IssueChangeContext issueChangeContext, BulkChangeData bulkChangeData, BulkChangeResult result) {
    return issue -> {
      ActionContext actionContext = new ActionContext(issue, issueChangeContext, bulkChangeData.projectsByUuid.get(issue.projectUuid()));
      bulkChangeData.getActionsWithoutComment().forEach(applyAction(actionContext, bulkChangeData, result));
      addCommentIfNeeded(actionContext, bulkChangeData);
      return result.success.contains(issue);
    };
  }

  private static Consumer<Action> applyAction(ActionContext actionContext, BulkChangeData bulkChangeData, BulkChangeResult result) {
    return action -> {
      DefaultIssue issue = actionContext.issue();
      try {
        if (action.supports(issue) && action.execute(bulkChangeData.getProperties(action.key()), actionContext)) {
          result.increaseSuccess(issue);
        }
      } catch (Exception e) {
        result.increaseFailure();
        LOG.error(format("An error occur when trying to apply the action : %s on issue : %s. This issue has been ignored. Error is '%s'",
          action.key(), issue.key(), e.getMessage()), e);
      }
    };
  }

  private static void addCommentIfNeeded(ActionContext actionContext, BulkChangeData bulkChangeData) {
    bulkChangeData.getCommentAction().ifPresent(action -> action.execute(bulkChangeData.getProperties(action.key()), actionContext));
  }

  private void sendNotification(Collection<DefaultIssue> issues, BulkChangeData bulkChangeData, Map<String, UserDto> userDtoByUuid, UserDto author) {
    if (!bulkChangeData.sendNotification) {
      return;
    }
    Set<ChangedIssue> changedIssues = issues.stream()
      // should not happen but filter it out anyway to avoid NPE in oldestUpdateDate call below
      .filter(issue -> issue.updateDate() != null)
      .map(issue -> toNotification(bulkChangeData, userDtoByUuid, issue))
      .filter(Objects::nonNull)
      .collect(toSet(issues.size()));

    if (changedIssues.isEmpty()) {
      return;
    }

    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(
      changedIssues,
      new UserChange(oldestUpdateDate(issues), new User(author.getUuid(), author.getLogin(), author.getName())));
    notificationService.scheduleForSending(notificationSerializer.serialize(builder));
  }

  @CheckForNull
  private ChangedIssue toNotification(BulkChangeData bulkChangeData, Map<String, UserDto> userDtoByUuid, DefaultIssue issue) {
    BranchDto branchDto = bulkChangeData.branchesByProjectUuid.get(issue.projectUuid());
    if (!hasNotificationSupport(branchDto)) {
      return null;
    }

    RuleDefinitionDto ruleDefinitionDto = bulkChangeData.rulesByKey.get(issue.ruleKey());
    ComponentDto projectDto = bulkChangeData.projectsByUuid.get(issue.projectUuid());
    if (ruleDefinitionDto == null || projectDto == null) {
      return null;
    }

    Optional<UserDto> assignee = Optional.ofNullable(issue.assignee()).map(userDtoByUuid::get);
    return new ChangedIssue.Builder(issue.key())
      .setNewStatus(issue.status())
      .setNewResolution(issue.resolution())
      .setAssignee(assignee.map(u -> new User(u.getUuid(), u.getLogin(), u.getName())).orElse(null))
      .setRule(new IssuesChangesNotificationBuilder.Rule(ruleDefinitionDto.getKey(), ruleDefinitionDto.getName()))
      .setProject(new Project.Builder(projectDto.uuid())
        .setKey(projectDto.getKey())
        .setProjectName(projectDto.name())
        .setBranchName(branchDto.isMain() ? null : branchDto.getKey())
        .build())
      .build();
  }

  private static boolean hasNotificationSupport(@Nullable BranchDto branch) {
    return branch != null && branch.getBranchType() != BranchType.PULL_REQUEST && branch.getBranchType() != BranchType.SHORT;
  }

  private static long oldestUpdateDate(Collection<DefaultIssue> issues) {
    long res = Long.MAX_VALUE;
    for (DefaultIssue issue : issues) {
      long issueUpdateDate = issue.updateDate().getTime();
      if (issueUpdateDate < res) {
        res = issueUpdateDate;
      }
    }
    return res;
  }

  private static Issues.BulkChangeWsResponse toWsResponse(BulkChangeResult result) {
    return Issues.BulkChangeWsResponse.newBuilder()
      .setTotal(result.countTotal())
      .setSuccess(result.countSuccess())
      .setIgnored((long) result.countTotal() - (result.countSuccess() + result.countFailures()))
      .setFailures(result.countFailures())
      .build();
  }

  private class BulkChangeData {
    private final Map<String, Map<String, Object>> propertiesByActions;
    private final boolean sendNotification;
    private final Collection<DefaultIssue> issues;
    private final Map<String, ComponentDto> projectsByUuid;
    private final Map<String, BranchDto> branchesByProjectUuid;
    private final Map<String, ComponentDto> componentsByUuid;
    private final Map<RuleKey, RuleDefinitionDto> rulesByKey;
    private final List<Action> availableActions;

    BulkChangeData(DbSession dbSession, Request request) {
      this.sendNotification = request.mandatoryParamAsBoolean(PARAM_SEND_NOTIFICATIONS);
      this.propertiesByActions = toPropertiesByActions(request);

      List<String> issueKeys = request.mandatoryParamAsStrings(PARAM_ISSUES);
      checkArgument(issueKeys.size() <= MAX_LIMIT, "Number of issues is limited to %s", MAX_LIMIT);
      List<IssueDto> allIssues = dbClient.issueDao().selectByKeys(dbSession, issueKeys);

      List<ComponentDto> allProjects = getComponents(dbSession, allIssues.stream().map(IssueDto::getProjectUuid).collect(MoreCollectors.toSet()));
      this.projectsByUuid = getAuthorizedProjects(allProjects).stream().collect(uniqueIndex(ComponentDto::uuid, identity()));
      this.branchesByProjectUuid = dbClient.branchDao().selectByUuids(dbSession, projectsByUuid.keySet()).stream()
        .collect(uniqueIndex(BranchDto::getUuid, identity()));
      this.issues = getAuthorizedIssues(allIssues);
      this.componentsByUuid = getComponents(dbSession,
        issues.stream().map(DefaultIssue::componentUuid).collect(MoreCollectors.toSet())).stream()
          .collect(uniqueIndex(ComponentDto::uuid, identity()));
      this.rulesByKey = dbClient.ruleDao().selectDefinitionByKeys(dbSession,
        issues.stream().map(DefaultIssue::ruleKey).collect(MoreCollectors.toSet())).stream()
        .collect(uniqueIndex(RuleDefinitionDto::getKey, identity()));

      this.availableActions = actions.stream()
        .filter(action -> propertiesByActions.containsKey(action.key()))
        .filter(action -> action.verify(getProperties(action.key()), issues, userSession))
        .collect(MoreCollectors.toList());
    }

    private List<ComponentDto> getComponents(DbSession dbSession, Collection<String> componentUuids) {
      return dbClient.componentDao().selectByUuids(dbSession, componentUuids);
    }

    private List<ComponentDto> getAuthorizedProjects(List<ComponentDto> projectDtos) {
      return userSession.keepAuthorizedComponents(UserRole.USER, projectDtos);
    }

    private List<DefaultIssue> getAuthorizedIssues(List<IssueDto> allIssues) {
      Set<String> projectUuids = projectsByUuid.values().stream().map(ComponentDto::uuid).collect(MoreCollectors.toSet());
      return allIssues.stream()
        .filter(issue -> projectUuids.contains(issue.getProjectUuid()))
        .map(IssueDto::toDefaultIssue)
        .collect(MoreCollectors.toList());
    }

    Map<String, Object> getProperties(String actionKey) {
      return propertiesByActions.get(actionKey);
    }

    List<Action> getActionsWithoutComment() {
      return availableActions.stream().filter(action -> !action.key().equals(COMMENT_KEY)).collect(MoreCollectors.toList());
    }

    Optional<Action> getCommentAction() {
      return availableActions.stream().filter(action -> action.key().equals(COMMENT_KEY)).findFirst();
    }

    private Map<String, Map<String, Object>> toPropertiesByActions(Request request) {
      Map<String, Map<String, Object>> properties = new HashMap<>();
      request.getParam(PARAM_ASSIGN, value -> properties.put(AssignAction.ASSIGN_KEY, new HashMap<>(of(ASSIGNEE_PARAMETER, value))));
      request.getParam(PARAM_SET_SEVERITY, value -> properties.put(SET_SEVERITY_KEY, new HashMap<>(of(SEVERITY_PARAMETER, value))));
      request.getParam(PARAM_SET_TYPE, value -> properties.put(SET_TYPE_KEY, new HashMap<>(of(TYPE_PARAMETER, value))));
      request.getParam(PARAM_DO_TRANSITION, value -> properties.put(DO_TRANSITION_KEY, new HashMap<>(of(TRANSITION_PARAMETER, value))));
      request.getParam(PARAM_ADD_TAGS, value -> properties.put(AddTagsAction.KEY, new HashMap<>(of(TAGS_PARAMETER, value))));
      request.getParam(PARAM_REMOVE_TAGS, value -> properties.put(RemoveTagsAction.KEY, new HashMap<>(of(TAGS_PARAMETER, value))));
      request.getParam(PARAM_COMMENT, value -> properties.put(COMMENT_KEY, new HashMap<>(of(COMMENT_PROPERTY, value))));
      checkAtLeastOneActionIsDefined(properties.keySet());
      return properties;
    }

    private void checkAtLeastOneActionIsDefined(Set<String> actions) {
      long actionsDefined = actions.stream().filter(action -> !action.equals(COMMENT_KEY)).count();
      checkArgument(actionsDefined > 0, "At least one action must be provided");
    }

    private boolean shouldRefreshMeasures() {
      return availableActions.stream().anyMatch(Action::shouldRefreshMeasures);
    }
  }

  private static class BulkChangeResult {
    private final int total;
    private final Set<DefaultIssue> success = new HashSet<>();
    private int failures = 0;

    BulkChangeResult(int total) {
      this.total = total;
    }

    void increaseSuccess(DefaultIssue issue) {
      this.success.add(issue);
    }

    void increaseFailure() {
      this.failures++;
    }

    int countTotal() {
      return total;
    }

    int countSuccess() {
      return success.size();
    }

    int countFailures() {
      return failures;
    }
  }
}
