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
package org.sonar.server.issue.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import org.sonar.api.component.Component;
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.user.User;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.issue.index.IssueResult;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.ws.SearchOptions;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class SearchAction implements RequestHandler {

  public static final String SEARCH_ACTION = "es-search";

  private static final String ACTIONS_EXTRA_FIELD = "actions";
  private static final String TRANSITIONS_EXTRA_FIELD = "transitions";
  private static final String ASSIGNEE_NAME_EXTRA_FIELD = "assigneeName";
  private static final String REPORTER_NAME_EXTRA_FIELD = "reporterName";
  private static final String ACTION_PLAN_NAME_EXTRA_FIELD = "actionPlanName";

  private static final String EXTRA_FIELDS_PARAM = "extra_fields";

  public static final String PARAM_FACETS = "facets";

  private final IssueService service;
  private final IssueActionsWriter actionsWriter;
  private final I18n i18n;
  private final Durations durations;

  public SearchAction(IssueService service, IssueActionsWriter actionsWriter, I18n i18n, Durations durations) {
    this.service = service;
    this.actionsWriter = actionsWriter;
    this.i18n = i18n;
    this.durations = durations;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(SEARCH_ACTION)
      .setDescription("Get a list of issues. If the number of issues is greater than 10,000, only the first 10,000 ones are returned by the web service. " +
        "Requires Browse permission on project(s)")
      .setSince("3.6")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"));

    // Add globalized search options. Will also support legacy params
    // Generic search parameters
    SearchOptions.definePageParams(action);
    SearchOptions.defineFieldsParam(action, Collections.<String>emptyList());

    // Issue-specific search parameters
    defineIssueSearchParameters(action);

    // Other parameters
    action.createParam(PARAM_FACETS)
      .setDescription("Compute predefined facets")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  public static void defineIssueSearchParameters(WebService.NewAction action) {

    action.createParam(IssueFilterParameters.ISSUES)
      .setDescription("Comma-separated list of issue keys")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam(IssueFilterParameters.SEVERITIES)
      .setDescription("Comma-separated list of severities")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL);
    action.createParam(IssueFilterParameters.STATUSES)
      .setDescription("Comma-separated list of statuses")
      .setExampleValue(Issue.STATUS_OPEN + "," + Issue.STATUS_REOPENED)
      .setPossibleValues(Issue.STATUSES);
    action.createParam(IssueFilterParameters.RESOLUTIONS)
      .setDescription("Comma-separated list of resolutions")
      .setExampleValue(Issue.RESOLUTION_FIXED + "," + Issue.RESOLUTION_REMOVED)
      .setPossibleValues(Issue.RESOLUTIONS);
    action.createParam(IssueFilterParameters.RESOLVED)
      .setDescription("To match resolved or unresolved issues")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.COMPONENTS)
      .setDescription("To retrieve issues associated to a specific list of components (comma-separated list of component keys). " +
        "Note that if you set the value to a project key, only issues associated to this project are retrieved. " +
        "Issues associated to its sub-components (such as files, packages, etc.) are not retrieved. See also componentRoots")
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam(IssueFilterParameters.COMPONENT_ROOTS)
      .setDescription("To retrieve issues associated to a specific list of components and their sub-components (comma-separated list of component keys). " +
        "Views are not supported")
      .setExampleValue("org.apache.struts:struts");
    action.createParam(IssueFilterParameters.RULES)
      .setDescription("Comma-separated list of coding rule keys. Format is <repository>:<rule>")
      .setExampleValue("squid:AvoidCycles");
    action.createParam(IssueFilterParameters.HIDE_RULES)
      .setDescription("To not return rules")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.ACTION_PLANS)
      .setDescription("Comma-separated list of action plan keys (not names)")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam(IssueFilterParameters.PLANNED)
      .setDescription("To retrieve issues associated to an action plan or not")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.REPORTERS)
      .setDescription("Comma-separated list of reporter logins")
      .setExampleValue("admin");
    action.createParam(IssueFilterParameters.ASSIGNEES)
      .setDescription("Comma-separated list of assignee logins")
      .setExampleValue("admin,usera");
    action.createParam(IssueFilterParameters.ASSIGNED)
      .setDescription("To retrieve assigned or unassigned issues")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.LANGUAGES)
      .setDescription("Comma-separated list of languages. Available since 4.4")
      .setExampleValue("java,js");
    action.createParam(EXTRA_FIELDS_PARAM)
      .setDescription("Add some extra fields on each issue. Available since 4.4")
      .setPossibleValues(ACTIONS_EXTRA_FIELD, TRANSITIONS_EXTRA_FIELD, ASSIGNEE_NAME_EXTRA_FIELD, REPORTER_NAME_EXTRA_FIELD, ACTION_PLAN_NAME_EXTRA_FIELD);
    action.createParam(IssueFilterParameters.CREATED_AT)
      .setDescription("To retrieve issues created at a given date. Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_AFTER)
      .setDescription("To retrieve issues created after the given date (inclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_BEFORE)
      .setDescription("To retrieve issues created before the given date (exclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.PAGE_SIZE)
      .setDescription("Maximum number of results per page. " +
        "Default value: 100 (except when the 'components' parameter is set, value is set to \"-1\" in this case). " +
        "If set to \"-1\", the max possible value is used")
      .setExampleValue("50");
    action.createParam(IssueFilterParameters.PAGE_INDEX)
      .setDescription("Index of the selected page")
      .setDefaultValue("1")
      .setExampleValue("2");
    action.createParam(IssueFilterParameters.SORT)
      .setDescription("Sort field")
      .setPossibleValues(IssueQuery.SORTS);
    action.createParam(IssueFilterParameters.ASC)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues();
    action.createParam("format")
      .setDescription("Only json format is available. This parameter is kept only for backward compatibility and shouldn't be used anymore");
  }

  @Override
  public void handle(Request request, Response response) {
    IssueQuery query = createQuery(request);
    SearchOptions searchOptions = SearchOptions.create(request);
    QueryOptions queryOptions = new QueryOptions();
    queryOptions.setPage(searchOptions.page(), searchOptions.pageSize());
    queryOptions.setFacet(request.mandatoryParamAsBoolean(PARAM_FACETS));

    IssueResult results = service.search(query, queryOptions);

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    writePaging(results, json);
    writeIssues(results, request.paramAsStrings(EXTRA_FIELDS_PARAM), json);
    writeComponents(results, json);
    writeProjects(results, json);
    writeRules(results, json);
    writeUsers(results, json);
    writeActionPlans(results, json);

    json.endObject().close();
  }

  private void writePaging(IssueQueryResult result, JsonWriter json) {
    json.prop("maxResultsReached", result.maxResultsReached());
    json.name("paging").beginObject()
      .prop("pageIndex", result.paging().pageIndex())
      .prop("pageSize", result.paging().pageSize())
      .prop("total", result.paging().total())
      // TODO Remove as part of Front-end rework on Issue Domain
      .prop("fTotal", i18n.formatInteger(UserSession.get().locale(), result.paging().total()))
      .prop("pages", result.paging().pages())
      .endObject();
  }

  private void writeIssues(IssueQueryResult result, @Nullable List<String> extraFields, JsonWriter json) {
    json.name("issues").beginArray();

    for (Issue issue : result.issues()) {
      json.beginObject();

      String actionPlanKey = issue.actionPlanKey();
      Duration debt = issue.debt();
      Date updateDate = issue.updateDate();

      json
        .prop("key", issue.key())
        .prop("component", issue.componentKey())
        .prop("project", issue.projectKey())
        .prop("rule", issue.ruleKey().toString())
        .prop("status", issue.status())
        .prop("resolution", issue.resolution())
        .prop("severity", issue.severity())
        .prop("message", issue.message())
        .prop("line", issue.line())
        .prop("debt", debt != null ? durations.encode(debt) : null)
        .prop("reporter", issue.reporter())
        .prop("assignee", issue.assignee())
        .prop("author", issue.authorLogin())
        .prop("actionPlan", actionPlanKey)
        .prop("creationDate", isoDate(issue.creationDate()))
        .prop("updateDate", isoDate(updateDate))
        // TODO Remove as part of Front-end rework on Issue Domain
        .prop("fUpdateAge", formatAgeDate(updateDate))
        .prop("closeDate", isoDate(issue.closeDate()));

      writeIssueComments(result, issue, json);
      writeIssueAttributes(issue, json);
      writeIssueExtraFields(result, issue, extraFields, json);
      json.endObject();
    }

    json.endArray();
  }

  private void writeIssueComments(IssueQueryResult queryResult, Issue issue, JsonWriter json) {
    if (!issue.comments().isEmpty()) {
      json.name("comments").beginArray();
      String login = UserSession.get().login();
      for (IssueComment comment : issue.comments()) {
        String userLogin = comment.userLogin();
        User user = userLogin != null ? queryResult.user(userLogin) : null;
        json.beginObject()
          .prop("key", comment.key())
          .prop("login", comment.userLogin())
          .prop("userName", user != null ? user.name() : null)
          .prop("htmlText", Markdown.convertToHtml(comment.markdownText()))
          .prop("markdown", comment.markdownText())
          .prop("updatable", login != null && login.equals(userLogin))
          .prop("createdAt", DateUtils.formatDateTime(comment.createdAt()))
          .endObject();
      }
      json.endArray();
    }
  }

  private void writeIssueAttributes(Issue issue, JsonWriter json) {
    if (!issue.attributes().isEmpty()) {
      json.name("attr").beginObject();
      for (Map.Entry<String, String> entry : issue.attributes().entrySet()) {
        json.prop(entry.getKey(), entry.getValue());
      }
      json.endObject();
    }
  }

  private void writeIssueExtraFields(IssueQueryResult result, Issue issue, @Nullable List<String> extraFields, JsonWriter json) {
    if (extraFields != null && UserSession.get().isLoggedIn()) {
      if (extraFields.contains(ACTIONS_EXTRA_FIELD)) {
        actionsWriter.writeActions(issue, json);
      }

      if (extraFields.contains(TRANSITIONS_EXTRA_FIELD)) {
        actionsWriter.writeTransitions(issue, json);
      }

      String assignee = issue.assignee();
      if (extraFields.contains(ASSIGNEE_NAME_EXTRA_FIELD) && assignee != null) {
        User user = result.user(assignee);
        json.prop(ASSIGNEE_NAME_EXTRA_FIELD, user != null ? user.name() : null);
      }

      String reporter = issue.reporter();
      if (extraFields.contains(REPORTER_NAME_EXTRA_FIELD) && reporter != null) {
        User user = result.user(reporter);
        json.prop(REPORTER_NAME_EXTRA_FIELD, user != null ? user.name() : null);
      }

      String actionPlanKey = issue.actionPlanKey();
      if (extraFields.contains(ACTION_PLAN_NAME_EXTRA_FIELD) && actionPlanKey != null) {
        ActionPlan actionPlan = result.actionPlan(issue);
        json.prop(ACTION_PLAN_NAME_EXTRA_FIELD, actionPlan != null ? actionPlan.name() : null);
      }
    }
  }

  private void writeComponents(IssueQueryResult result, JsonWriter json) {
    json.name("components").beginArray();
    for (Component component : result.components()) {
      ComponentDto componentDto = (ComponentDto) component;
      json.beginObject()
        .prop("key", component.key())
        .prop("id", componentDto.getId())
        .prop("qualifier", component.qualifier())
        .prop("name", component.name())
        .prop("longName", component.longName())
        .prop("path", component.path())
        // On a root project, subProjectId is null but projectId is equal to itself, which make no sense.
        .prop("projectId", (componentDto.projectId() != null && componentDto.subProjectId() != null) ? componentDto.projectId() : null)
        .prop("subProjectId", componentDto.subProjectId())
        .endObject();
    }
    json.endArray();
  }

  private void writeProjects(IssueQueryResult result, JsonWriter json) {
    json.name("projects").beginArray();
    for (Component project : result.projects()) {
      ComponentDto componentDto = (ComponentDto) project;
      json.beginObject()
        .prop("key", project.key())
        .prop("id", componentDto.getId())
        .prop("qualifier", project.qualifier())
        .prop("name", project.name())
        .prop("longName", project.longName())
        .endObject();
    }
    json.endArray();
  }

  private void writeRules(IssueQueryResult result, JsonWriter json) {
    json.name("rules").beginArray();
    for (Rule rule : result.rules()) {
      json.beginObject()
        .prop("key", rule.ruleKey().toString())
        .prop("name", rule.getName())
        .prop("desc", rule.getDescription())
        .prop("status", rule.getStatus())
        .endObject();
    }
    json.endArray();
  }

  private void writeUsers(IssueQueryResult result, JsonWriter json) {
    json.name("users").beginArray();
    for (User user : result.users()) {
      json.beginObject()
        .prop("login", user.login())
        .prop("name", user.name())
        .prop("active", user.active())
        .prop("email", user.email())
        .endObject();
    }
    json.endArray();
  }

  private void writeActionPlans(IssueQueryResult result, JsonWriter json) {
    if (!result.actionPlans().isEmpty()) {
      json.name("actionPlans").beginArray();
      for (ActionPlan actionPlan : result.actionPlans()) {
        Date deadLine = actionPlan.deadLine();
        Date updatedAt = actionPlan.updatedAt();

        json.beginObject()
          .prop("key", actionPlan.key())
          .prop("name", actionPlan.name())
          .prop("status", actionPlan.status())
          .prop("project", actionPlan.projectKey())
          .prop("userLogin", actionPlan.userLogin())
          .prop("deadLine", isoDate(deadLine))
          .prop("fDeadLine", formatDate(deadLine))
          .prop("createdAt", isoDate(actionPlan.createdAt()))
          .prop("fCreatedAt", formatDate(actionPlan.createdAt()))
          .prop("updatedAt", isoDate(actionPlan.updatedAt()))
          .prop("fUpdatedAt", formatDate(updatedAt))
          .endObject();
      }
      json.endArray();
    }
  }

  @CheckForNull
  private String isoDate(@Nullable Date date) {
    if (date != null) {
      return DateUtils.formatDateTime(date);
    }
    return null;
  }

  @CheckForNull
  private String formatDate(@Nullable Date date) {
    if (date != null) {
      return i18n.formatDateTime(UserSession.get().locale(), date);
    }
    return null;
  }

  @CheckForNull
  private String formatAgeDate(@Nullable Date date) {
    if (date != null) {
      return i18n.ageFromNow(UserSession.get().locale(), date);
    }
    return null;
  }

  @CheckForNull
  private static Collection<RuleKey> stringsToRules(@Nullable Collection<String> rules) {
    if (rules != null) {
      return newArrayList(Iterables.transform(rules, new Function<String, RuleKey>() {
        @Override
        public RuleKey apply(@Nullable String s) {
          return s != null ? RuleKey.parse(s) : null;
        }
      }));
    }
    return null;
  }

  @VisibleForTesting
  static IssueQuery createQuery(Request request) {
    IssueQuery.Builder builder = IssueQuery.builder()
      .requiredRole(UserRole.USER)
      .issueKeys(request.paramAsStrings(IssueFilterParameters.ISSUES))
      .severities(request.paramAsStrings(IssueFilterParameters.SEVERITIES))
      .statuses(request.paramAsStrings(IssueFilterParameters.STATUSES))
      .resolutions(request.paramAsStrings(IssueFilterParameters.RESOLUTIONS))
      .resolved(request.paramAsBoolean(IssueFilterParameters.RESOLVED))
      .components(request.paramAsStrings(IssueFilterParameters.COMPONENTS))
      .componentRoots(request.paramAsStrings(IssueFilterParameters.COMPONENT_ROOTS))
      .rules(stringsToRules(request.paramAsStrings(IssueFilterParameters.RULES)))
      .actionPlans(request.paramAsStrings(IssueFilterParameters.ACTION_PLANS))
      .reporters(request.paramAsStrings(IssueFilterParameters.REPORTERS))
      .assignees(request.paramAsStrings(IssueFilterParameters.ASSIGNEES))
      .languages(request.paramAsStrings(IssueFilterParameters.LANGUAGES))
      .assigned(request.paramAsBoolean(IssueFilterParameters.ASSIGNED))
      .planned(request.paramAsBoolean(IssueFilterParameters.PLANNED))
      .hideRules(request.paramAsBoolean(IssueFilterParameters.HIDE_RULES))
      .createdAt(request.paramAsDateTime(IssueFilterParameters.CREATED_AT))
      .createdAfter(request.paramAsDateTime(IssueFilterParameters.CREATED_AFTER))
      .createdBefore(request.paramAsDateTime(IssueFilterParameters.CREATED_BEFORE))
      .pageSize(request.paramAsInt(IssueFilterParameters.PAGE_SIZE))
      .pageIndex(request.paramAsInt(IssueFilterParameters.PAGE_INDEX));
    String sort = request.param(IssueFilterParameters.SORT);
    if (!Strings.isNullOrEmpty(sort)) {
      builder.sort(sort);
      builder.asc(request.paramAsBoolean(IssueFilterParameters.ASC));
    }
    return builder.build();
  }
}
