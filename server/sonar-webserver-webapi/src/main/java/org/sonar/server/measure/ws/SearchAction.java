/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.RemovedMetricConverter;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.Measures.SearchWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.SUBVIEW;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.db.metric.RemovedMetricConverter.DEPRECATED_METRIC_REPLACEMENT;
import static org.sonar.db.metric.RemovedMetricConverter.REMOVED_METRIC;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PROJECT_KEYS;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.measure.ws.MeasureDtoToWsMeasure.updateMeasureBuilder;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements MeasuresWsAction {

  private static final int MAX_NB_PROJECTS = 100;
  private static final Set<String> ALLOWED_QUALIFIERS = ImmutableSet.of(PROJECT, APP, VIEW, SUBVIEW);

  private final UserSession userSession;
  private final DbClient dbClient;

  public SearchAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search")
      .setInternal(true)
      .setDescription("Search for project measures ordered by project names.<br>" +
        "At most %d projects can be provided.<br>" +
        "Returns the projects with the 'Browse' permission.",
        MAX_NB_PROJECTS)
      .setSince("6.2")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setHandler(this)
      .setChangelog(
        new Change("10.4", "The metrics 'open_issues', 'reopened_issues' and 'confirmed_issues' are now deprecated in the response. Consume 'violations' instead."),
        new Change("10.4", "The use of 'open_issues', 'reopened_issues' and 'confirmed_issues' values in 'metricKeys' param are now deprecated. Use 'violations' instead."),
        new Change("10.4", "The metric 'wont_fix_issues' is now deprecated in the response. Consume 'accepted_issues' instead."),
        new Change("10.4", "The use of 'wont_fix_issues' value in 'metricKeys' param is now deprecated. Use 'accepted_issues' instead."),
        new Change("10.4", "Added new accepted value for the 'metricKeys' param: 'accepted_issues'."),
        new Change("10.0", format("The use of the following metrics in 'metricKeys' parameter is not deprecated anymore: %s",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube93())),
        new Change("9.3", format("The use of the following metrics in 'metricKeys' parameter is deprecated: %s",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube93())));

    createMetricKeysParameter(action);

    action.createParam(PARAM_PROJECT_KEYS)
      .setDescription("Comma-separated list of project, view or sub-view keys")
      .setExampleValue(String.join(",", KEY_PROJECT_EXAMPLE_001, KEY_PROJECT_EXAMPLE_002))
      .setRequired(true);
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchWsResponse response = new ResponseBuilder(httpRequest, dbSession).build();
      writeProtobuf(response, httpRequest, httpResponse);
    }
  }

  private class ResponseBuilder {
    private final DbSession dbSession;
    private final Request httpRequest;
    private SearchRequest request;
    private List<ComponentDto> projects;
    private List<MetricDto> metrics;
    private List<LiveMeasureDto> measures;

    ResponseBuilder(Request httpRequest, DbSession dbSession) {
      this.dbSession = dbSession;
      this.httpRequest = httpRequest;
    }

    SearchWsResponse build() {
      this.request = createRequest();
      this.projects = searchProjects();
      this.metrics = searchMetrics();
      this.measures = searchMeasures();
      return buildResponse();
    }

    private SearchRequest createRequest() {
      request = SearchRequest.builder()
        .setMetricKeys(httpRequest.mandatoryParamAsStrings(PARAM_METRIC_KEYS))
        .setProjectKeys(httpRequest.paramAsStrings(PARAM_PROJECT_KEYS))
        .build();

      return request;
    }

    private List<ComponentDto> searchProjects() {
      List<ComponentDto> componentDtos = searchByProjectKeys(dbSession, request.getProjectKeys());
      checkArgument(ALLOWED_QUALIFIERS.containsAll(componentDtos.stream().map(ComponentDto::qualifier).collect(Collectors.toSet())),
        "Only component of qualifiers %s are allowed", ALLOWED_QUALIFIERS);
      return getAuthorizedProjects(componentDtos);
    }

    private List<ComponentDto> searchByProjectKeys(DbSession dbSession, List<String> projectKeys) {
      return dbClient.componentDao().selectByKeys(dbSession, projectKeys);
    }

    private List<ComponentDto> getAuthorizedProjects(List<ComponentDto> componentDtos) {
      return userSession.keepAuthorizedComponents(UserRole.USER, componentDtos);
    }

    private List<MetricDto> searchMetrics() {
      Collection<String> metricKeysParamValue = RemovedMetricConverter.withRemovedMetricAlias(request.getMetricKeys());
      List<MetricDto> dbMetrics = dbClient.metricDao().selectByKeys(dbSession, metricKeysParamValue);
      List<String> metricKeys = dbMetrics.stream().map(MetricDto::getKey).toList();
      checkRequest(metricKeysParamValue.size() == dbMetrics.size(), "The following metrics are not found: %s",
        String.join(", ", difference(metricKeysParamValue, metricKeys)));
      return dbMetrics;
    }

    private List<String> difference(Collection<String> expected, Collection<String> actual) {
      Set<String> actualSet = new HashSet<>(actual);

      return expected.stream()
        .filter(value -> !actualSet.contains(value))
        .sorted(String::compareTo)
        .toList();
    }

    private List<LiveMeasureDto> searchMeasures() {
      return dbClient.liveMeasureDao().selectByComponentUuidsAndMetricUuids(dbSession,
        projects.stream().map(ComponentDto::uuid).toList(),
        metrics.stream().map(MetricDto::getUuid).toList());
    }

    private SearchWsResponse buildResponse() {
      List<Measure> wsMeasures = buildWsMeasures();
      return SearchWsResponse.newBuilder()
        .addAllMeasures(wsMeasures)
        .build();
    }

    private List<Measure> buildWsMeasures() {
      Map<String, ComponentDto> componentsByUuid = projects.stream().collect(toMap(ComponentDto::uuid, Function.identity()));
      Map<String, String> componentNamesByKey = projects.stream().collect(toMap(ComponentDto::getKey, ComponentDto::name));
      Map<String, MetricDto> metricsByUuid = metrics.stream().collect(toMap(MetricDto::getUuid, identity()));

      Function<LiveMeasureDto, MetricDto> dbMeasureToDbMetric = dbMeasure -> metricsByUuid.get(dbMeasure.getMetricUuid());
      Function<Measure, String> byMetricKey = Measure::getMetric;
      Function<Measure, String> byComponentName = wsMeasure -> componentNamesByKey.get(wsMeasure.getComponent());

      Measure.Builder measureBuilder = Measure.newBuilder();
      List<Measure> allMeasures = new ArrayList<>();
      for (LiveMeasureDto measure : measures) {
        updateMeasureBuilder(measureBuilder, dbMeasureToDbMetric.apply(measure), measure);
        measureBuilder.setComponent(componentsByUuid.get(measure.getComponentUuid()).getKey());
        Measure measureMsg = measureBuilder.build();
        addMeasureIncludingRenamedMetric(measureMsg, allMeasures, measureBuilder);

        measureBuilder.clear();
      }
      return allMeasures.stream()
        .sorted(comparing(byMetricKey).thenComparing(byComponentName))
        .toList();
    }

    private void addMeasureIncludingRenamedMetric(Measure measureMsg, List<Measure> allMeasures, Measure.Builder measureBuilder) {
      if (measureBuilder.getMetric().equals(DEPRECATED_METRIC_REPLACEMENT)) {
        if (request.getMetricKeys().contains(DEPRECATED_METRIC_REPLACEMENT)) {
          allMeasures.add(measureMsg);
        }
        if (request.getMetricKeys().contains(REMOVED_METRIC)) {
          allMeasures.add(measureBuilder.setMetric(REMOVED_METRIC).build());
        }
      } else {
        allMeasures.add(measureMsg);
      }
    }
  }

  private static class SearchRequest {

    private final List<String> metricKeys;
    private final List<String> projectKeys;

    public SearchRequest(Builder builder) {
      metricKeys = builder.metricKeys;
      projectKeys = builder.projectKeys;
    }

    public List<String> getMetricKeys() {
      return metricKeys;
    }

    public List<String> getProjectKeys() {
      return projectKeys;
    }

    public static Builder builder() {
      return new Builder();
    }

  }

  private static class Builder {
    private List<String> metricKeys;
    private List<String> projectKeys;

    private Builder() {
      // enforce method constructor
    }

    public Builder setMetricKeys(List<String> metricKeys) {
      this.metricKeys = metricKeys;
      return this;
    }

    public Builder setProjectKeys(List<String> projectKeys) {
      this.projectKeys = projectKeys;
      return this;
    }

    public SearchAction.SearchRequest build() {
      checkArgument(metricKeys != null && !metricKeys.isEmpty(), "Metric keys must be provided");
      checkArgument(projectKeys != null && !projectKeys.isEmpty(), "Project keys must be provided");
      int nbComponents = projectKeys.size();
      checkArgument(nbComponents <= MAX_NB_PROJECTS,
        "%s projects provided, more than maximum authorized (%s)", nbComponents, MAX_NB_PROJECTS);
      return new SearchAction.SearchRequest(this);
    }
  }
}
