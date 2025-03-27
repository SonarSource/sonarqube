/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.measure.ws;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.db.component.SnapshotQuery.SORT_FIELD;
import org.sonar.db.component.SnapshotQuery.SORT_ORDER;
import org.sonar.db.measure.PastMeasureQuery;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.RemovedMetricConverter;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.telemetry.TelemetryPortfolioActivityGraphTypeProvider;
import org.sonar.server.telemetry.TelemetryPortfolioActivityRequestedMetricProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Measures.SearchHistoryResponse;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.db.component.ComponentQualifiers.SUBVIEW;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.server.component.ws.MeasuresWsParameters.ACTION_SEARCH_HISTORY;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_FROM;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRICS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_TO;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchHistoryAction implements MeasuresWsAction {

  private static final int MAX_PAGE_SIZE = 1_000;
  private static final int DEFAULT_PAGE_SIZE = 100;
  public static final Pattern GRAPH_REGEX = Pattern.compile("graph=([^&]+)");

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final TelemetryPortfolioActivityRequestedMetricProvider telemetryRequestedMetricProvider;
  private final TelemetryPortfolioActivityGraphTypeProvider telemetryGraphTypeProvider;

  public SearchHistoryAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession,
    TelemetryPortfolioActivityRequestedMetricProvider telemetryRequestedMetricProvider,
    TelemetryPortfolioActivityGraphTypeProvider telemetryGraphTypeProvider) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.telemetryRequestedMetricProvider = telemetryRequestedMetricProvider;
    this.telemetryGraphTypeProvider = telemetryGraphTypeProvider;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH_HISTORY)
      .setDescription("Search measures history of a component.<br>" +
        "Measures are ordered chronologically.<br>" +
        "Pagination applies to the number of measures for each metric.<br>" +
        "Requires the following permission: 'Browse' on the specified component. <br>" +
        "For applications, it also requires 'Browse' permission on its child projects.")
      .setResponseExample(getClass().getResource("search_history-example.json"))
      .setSince("6.3")
      .setChangelog(
        new Change("10.8", String.format("The following metrics are not deprecated anymore: %s",
          MeasuresWsModule.getUndeprecatedMetricsinSonarQube108())),
        new Change("10.8", String.format("Added new accepted values for the 'metricKeys' param: %s",
          MeasuresWsModule.getNewMetricsInSonarQube108())),
        new Change("10.8", String.format("The metrics %s are now deprecated. Use 'software_quality_maintainability_issues', " +
          "'software_quality_reliability_issues', 'software_quality_security_issues', 'new_software_quality_maintainability_issues', " +
          "'new_software_quality_reliability_issues', 'new_software_quality_security_issues' instead.",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube108())),
        new Change("10.7", "Added new accepted values for the 'metricKeys' param: %s".formatted(MeasuresWsModule.getNewMetricsInSonarQube107())),
        new Change("10.5", String.format("The metrics %s are now deprecated " +
          "without exact replacement. Use 'maintainability_issues', 'reliability_issues' and 'security_issues' instead.",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube105())),
        new Change("10.5", "Added new accepted values for the 'metricKeys' param: 'new_maintainability_issues', 'new_reliability_issues', 'new_security_issues'"),
        new Change("10.4", String.format("The metrics %s are now deprecated " +
          "without exact replacement. Use 'maintainability_issues', 'reliability_issues' and 'security_issues' instead.",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube104())),
        new Change("10.4", "The metrics 'open_issues', 'reopened_issues' and 'confirmed_issues' are now deprecated in the response. Consume 'violations' instead."),
        new Change("10.4", "The use of 'open_issues', 'reopened_issues' and 'confirmed_issues' values in 'metricKeys' param are now deprecated. Use 'violations' instead."),
        new Change("10.4", "The metric 'wont_fix_issues' is now deprecated in the response. Consume 'accepted_issues' instead."),
        new Change("10.4", "The use of 'wont_fix_issues' value in 'metricKeys' param is now deprecated. Use 'accepted_issues' instead."),
        new Change("10.4", "Added new accepted value for the 'metricKeys' param: 'accepted_issues'."),
        new Change("10.0", format("The use of the following metrics in 'metricKeys' parameter is not deprecated anymore: %s",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube93())),
        new Change("9.3", format("The use of the following metrics in 'metrics' parameter is deprecated: %s",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube93())),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setRequired(true)
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setSince("6.6")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setSince("7.1")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);

    action.createParam(PARAM_METRICS)
      .setDescription("Comma-separated list of metric keys")
      .setRequired(true)
      .setExampleValue("ncloc,coverage,new_violations");

    action.createParam(PARAM_FROM)
      .setDescription("Filter measures created after the given date (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    action.createParam(PARAM_TO)
      .setDescription("Filter measures created before the given date (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    action.addPagingParams(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Optional<SearchHistoryResult> searchHistoryResult = Optional.of(request)
      .map(SearchHistoryAction::toWsRequest)
      .map(search());
    SearchHistoryResponse searchHistoryResponse = searchHistoryResult
      .map(result -> new SearchHistoryResponseFactory(result).apply())
      .orElseThrow();

    writeProtobuf(searchHistoryResponse, request, response);
    searchHistoryResult.ifPresent(r -> writeTelemetry(request, r));
  }

  private void writeTelemetry(Request request, SearchHistoryResult searchResult) {
    Map<String, String> headers = request.getHeaders();
    String referer = headers.getOrDefault("referer", "");
    if (referer.contains("project/activity") && List.of(VIEW, SUBVIEW).contains(searchResult.getComponent().qualifier())) {
      toWsRequest(request).metrics.forEach(telemetryRequestedMetricProvider::metricRequested);
      getGraphType(referer).ifPresent(telemetryGraphTypeProvider::incrementCount);
    }
  }

  private static Optional<String> getGraphType(String url) {
    Matcher matcher = GRAPH_REGEX.matcher(url);
    return matcher.find() ? Optional.of(matcher.group(1)) : Optional.of("issues");
  }

  private static SearchHistoryRequest toWsRequest(Request request) {
    return SearchHistoryRequest.builder()
      .setComponent(request.mandatoryParam(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST))
      .setMetrics(request.mandatoryParamAsStrings(PARAM_METRICS))
      .setFrom(request.param(PARAM_FROM))
      .setTo(request.param(PARAM_TO))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .build();
  }

  private Function<SearchHistoryRequest, SearchHistoryResult> search() {
    return request -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        ComponentDto component = searchComponent(request, dbSession);

        SearchHistoryResult result = new SearchHistoryResult(request.page, request.pageSize)
          .setComponent(component)
          .setAnalyses(searchAnalyses(dbSession, request, component))
          .setMetrics(searchMetrics(dbSession, request))
          .setRequestedMetrics(request.getMetrics());
        return result.setMeasures(searchMeasures(dbSession, request, result));
      }
    };
  }

  private ComponentDto searchComponent(SearchHistoryRequest request, DbSession dbSession) {
    ComponentDto component = loadComponent(dbSession, request);
    userSession.checkComponentPermission(ProjectPermission.USER, component);
    if (ComponentScopes.PROJECT.equals(component.scope()) && ComponentQualifiers.APP.equals(component.qualifier())) {
      userSession.checkChildProjectsPermission(ProjectPermission.USER, component);
    }
    return component;
  }

  private List<ProjectMeasureDto> searchMeasures(DbSession dbSession, SearchHistoryRequest request, SearchHistoryResult result) {
    Date from = parseStartingDateOrDateTime(request.getFrom());
    Date to = parseEndingDateOrDateTime(request.getTo());
    PastMeasureQuery dbQuery = new PastMeasureQuery(
      result.getComponent().uuid(),
      result.getMetrics().stream().map(MetricDto::getUuid).toList(),
      from == null ? null : from.getTime(),
      to == null ? null : (to.getTime() + 1_000L));
    return dbClient.projectMeasureDao().selectPastMeasures(dbSession, dbQuery);
  }

  private List<SnapshotDto> searchAnalyses(DbSession dbSession, SearchHistoryRequest request, ComponentDto component) {
    SnapshotQuery dbQuery = new SnapshotQuery()
      .setRootComponentUuid(component.branchUuid())
      .setStatus(STATUS_PROCESSED)
      .setSort(SORT_FIELD.BY_DATE, SORT_ORDER.ASC);
    ofNullable(request.getFrom()).ifPresent(from -> dbQuery.setCreatedAfter(parseStartingDateOrDateTime(from).getTime()));
    ofNullable(request.getTo()).ifPresent(to -> dbQuery.setCreatedBefore(parseEndingDateOrDateTime(to).getTime() + 1_000L));

    return dbClient.snapshotDao().selectAnalysesByQuery(dbSession, dbQuery);
  }

  private List<MetricDto> searchMetrics(DbSession dbSession, SearchHistoryRequest request) {
    List<String> upToDateRequestedMetrics = RemovedMetricConverter.withRemovedMetricAlias(request.getMetrics());
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, upToDateRequestedMetrics);
    if (upToDateRequestedMetrics.size() > metrics.size()) {
      Set<String> requestedMetrics = new HashSet<>(upToDateRequestedMetrics);
      Set<String> foundMetrics = metrics.stream().map(MetricDto::getKey).collect(Collectors.toSet());

      Set<String> unfoundMetrics = Sets.difference(requestedMetrics, foundMetrics).immutableCopy();
      throw new IllegalArgumentException(format("Metrics %s are not found", String.join(", ", unfoundMetrics)));
    }

    return metrics;
  }

  private ComponentDto loadComponent(DbSession dbSession, SearchHistoryRequest request) {
    String componentKey = request.getComponent();
    String branch = request.getBranch();
    String pullRequest = request.getPullRequest();
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  static class SearchHistoryRequest {
    private final String component;
    private final String branch;
    private final String pullRequest;
    private final List<String> metrics;
    private final String from;
    private final String to;
    private final int page;
    private final int pageSize;

    public SearchHistoryRequest(Builder builder) {
      this.component = builder.component;
      this.branch = builder.branch;
      this.pullRequest = builder.pullRequest;
      this.metrics = builder.metrics;
      this.from = builder.from;
      this.to = builder.to;
      this.page = builder.page;
      this.pageSize = builder.pageSize;
    }

    public String getComponent() {
      return component;
    }

    @CheckForNull
    public String getBranch() {
      return branch;
    }

    @CheckForNull
    public String getPullRequest() {
      return pullRequest;
    }

    public List<String> getMetrics() {
      return metrics;
    }

    @CheckForNull
    public String getFrom() {
      return from;
    }

    @CheckForNull
    public String getTo() {
      return to;
    }

    public int getPage() {
      return page;
    }

    public int getPageSize() {
      return pageSize;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  static class Builder {
    private String component;
    private String branch;
    private String pullRequest;
    private List<String> metrics;
    private String from;
    private String to;
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
      // enforce build factory method
    }

    public Builder setComponent(String component) {
      this.component = component;
      return this;
    }

    public Builder setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    public Builder setPullRequest(@Nullable String pullRequest) {
      this.pullRequest = pullRequest;
      return this;
    }

    public Builder setMetrics(List<String> metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder setFrom(@Nullable String from) {
      this.from = from;
      return this;
    }

    public Builder setTo(@Nullable String to) {
      this.to = to;
      return this;
    }

    public Builder setPage(int page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public SearchHistoryRequest build() {
      checkArgument(component != null && !component.isEmpty(), "Component key is required");
      checkArgument(metrics != null && !metrics.isEmpty(), "Metric keys are required");
      checkArgument(pageSize <= MAX_PAGE_SIZE, "Page size (%d) must be lower than or equal to %d", pageSize, MAX_PAGE_SIZE);

      return new SearchHistoryRequest(this);
    }

    private static void checkArgument(boolean condition, String message, Object... args) {
      if (!condition) {
        throw new IllegalArgumentException(format(message, args));
      }
    }
  }
}
