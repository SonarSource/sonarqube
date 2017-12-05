/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.Action;
import org.sonar.server.issue.AddTagsAction;
import org.sonar.server.issue.AssignAction;
import org.sonar.server.issue.IssueChangePostProcessor;
import org.sonar.server.issue.IssueStorage;
import org.sonar.server.issue.RemoveTagsAction;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Issues;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static org.sonar.api.issue.DefaultTransitions.REOPEN;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
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
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PLAN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_REMOVE_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEND_NOTIFICATIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SET_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SET_TYPE;

public class BulkChangeAction implements IssuesWsAction {

  private static final Logger LOG = Loggers.get(BulkChangeAction.class);

  private final System2 system2;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueStorage issueStorage;
  private final NotificationManager notificationService;
  private final List<Action> actions;
  private final IssueChangePostProcessor issueChangePostProcessor;

  public BulkChangeAction(System2 system2, UserSession userSession, DbClient dbClient, IssueStorage issueStorage,
    NotificationManager notificationService, List<Action> actions,
    IssueChangePostProcessor issueChangePostProcessor) {
    this.system2 = system2;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueStorage = issueStorage;
    this.notificationService = notificationService;
    this.actions = actions;
    this.issueChangePostProcessor = issueChangePostProcessor;
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
    action.createParam(PARAM_PLAN)
      .setDescription("In 5.5, action plans are dropped. Has no effect. To plan the list of issues to a specific action plan (key), or unlink all the issues from an action plan")
      .setDeprecatedSince("5.5");
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
    IssueChangeContext issueChangeContext = IssueChangeContext.createUser(new Date(system2.now()), userSession.getLogin());

    List<DefaultIssue> items = bulkChangeData.issues.stream()
      .filter(bulkChange(issueChangeContext, bulkChangeData, result))
      .collect(MoreCollectors.toList());
    issueStorage.save(items);

    refreshLiveMeasures(dbSession, bulkChangeData, result);

    items.forEach(sendNotification(issueChangeContext, bulkChangeData));

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

  private Consumer<DefaultIssue> sendNotification(IssueChangeContext issueChangeContext, BulkChangeData bulkChangeData) {
    return issue -> {
      if (bulkChangeData.sendNotification) {
        notificationService.scheduleForSending(new IssueChangeNotification()
          .setIssue(issue)
          .setChangeAuthorLogin(issueChangeContext.login())
          .setRuleName(bulkChangeData.rulesByKey.get(issue.ruleKey()).getName())
          .setProject(bulkChangeData.projectsByUuid.get(issue.projectUuid()))
          .setComponent(bulkChangeData.componentsByUuid.get(issue.componentUuid())));
      }
    };
  }

  private static Issues.BulkChangeWsResponse toWsResponse(BulkChangeResult result) {
    return Issues.BulkChangeWsResponse.newBuilder()
      .setTotal(result.countTotal())
      .setSuccess(result.countSuccess())
      .setIgnored((long) result.countTotal() - (result.countSuccess() + result.countFailures()))
      .setFailures(result.countFailures())
      .build();
  }

  public static class ActionContext implements Action.Context {
    private final DefaultIssue issue;
    private final IssueChangeContext changeContext;
    private final ComponentDto project;

    public ActionContext(DefaultIssue issue, IssueChangeContext changeContext, ComponentDto project) {
      this.issue = issue;
      this.changeContext = changeContext;
      this.project = project;
    }

    @Override
    public DefaultIssue issue() {
      return issue;
    }

    @Override
    public IssueChangeContext issueChangeContext() {
      return changeContext;
    }

    @Override
    public ComponentDto project() {
      return project;
    }
  }

  private class BulkChangeData {
    private final Map<String, Map<String, Object>> propertiesByActions;
    private final boolean sendNotification;
    private final Collection<DefaultIssue> issues;
    private final Map<String, ComponentDto> projectsByUuid;
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
