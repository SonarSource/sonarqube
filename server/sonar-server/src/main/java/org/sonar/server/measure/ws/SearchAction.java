/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.WsMeasures.SearchWsResponse;
import org.sonarqube.ws.WsMeasures.SearchWsResponse.Component;
import org.sonarqube.ws.client.measure.SearchRequest;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.core.util.stream.Collectors.toList;
import static org.sonar.core.util.stream.Collectors.toSet;
import static org.sonar.core.util.stream.Collectors.uniqueIndex;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.dbToWsComponent;
import static org.sonar.server.measure.ws.MeasureDtoToWsMeasure.dbToWsMeasure;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.measure.ws.MetricDtoWithBestValue.buildBestMeasure;
import static org.sonar.server.measure.ws.MetricDtoWithBestValue.isEligibleForBestValue;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriods.snapshotToWsPeriods;
import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_002;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;

public class SearchAction implements MeasuresWsAction {

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
      .setDescription("Search for component measures ordered by component names.<br>" +
        "At most %d components can be provided.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        " <li>'Administer System'</li>" +
        " <li>'Administer' rights on the provided components</li>" +
        " <li>'Browse' on the provided components</li>" +
        "</ul>",
        SearchRequest.MAX_NB_COMPONENTS)
      .setSince("6.2")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setHandler(this);

    createMetricKeysParameter(action);

    action.createParam(PARAM_COMPONENT_KEYS)
      .setDescription("Comma-separated list of component keys")
      .setExampleValue(String.join(",", KEY_PROJECT_EXAMPLE_001, KEY_FILE_EXAMPLE_001, KEY_PROJECT_EXAMPLE_002, KEY_FILE_EXAMPLE_002))
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
    private List<ComponentDto> components;
    private List<MetricDto> metrics;
    private List<MeasureDto> measures;
    private List<SnapshotDto> snapshots;

    ResponseBuilder(Request httpRequest, DbSession dbSession) {
      this.dbSession = dbSession;
      this.httpRequest = httpRequest;
    }

    SearchWsResponse build() {
      this.request = createRequest();
      this.components = searchComponents();
      this.metrics = searchMetrics();
      this.measures = searchMeasures();
      this.snapshots = searchSnapshots();

      return buildResponse();
    }

    private List<SnapshotDto> searchSnapshots() {
      requireNonNull(components);

      Set<String> projectUuids = components.stream().map(ComponentDto::projectUuid).collect(toSet());
      return dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, projectUuids);
    }

    private SearchRequest createRequest() {
      request = SearchRequest.builder()
        .setMetricKeys(httpRequest.mandatoryParamAsStrings(PARAM_METRIC_KEYS))
        .setComponentKeys(httpRequest.paramAsStrings(PARAM_COMPONENT_KEYS))
        .build();

      return request;
    }

    private List<MetricDto> searchMetrics() {
      requireNonNull(request);
      List<MetricDto> dbMetrics = dbClient.metricDao().selectByKeys(dbSession, request.getMetricKeys());
      List<String> metricKeys = dbMetrics.stream().map(MetricDto::getKey).collect(toList());
      checkRequest(request.getMetricKeys().size() == dbMetrics.size(), "The following metrics are not found: %s",
        String.join(", ", difference(request.getMetricKeys(), metricKeys)));
      return dbMetrics;
    }

    private List<ComponentDto> searchComponents() {
      requireNonNull(request);
      return getAuthorizedComponents(searchByComponentKeys(dbSession, request.getComponentKeys()));
    }

    private List<ComponentDto> getAuthorizedComponents(List<ComponentDto> componentDtos) {
      List<String> projectUuids = componentDtos.stream().map(ComponentDto::getRootUuid).collect(Collectors.toList());
      List<ComponentDto> projects = dbClient.componentDao().selectByUuids(dbSession, projectUuids);
      Map<String, Long> projectIdsByUuids = projects.stream().collect(uniqueIndex(ComponentDto::uuid, ComponentDto::getId));
      Collection<Long> authorizedProjectIds = dbClient.authorizationDao().keepAuthorizedProjectIds(dbSession,
        projects.stream().map(ComponentDto::getId).collect(toList()),
        userSession.getUserId(), UserRole.USER);
      return componentDtos.stream()
        .filter(c -> authorizedProjectIds.contains(projectIdsByUuids.get(c.projectUuid())))
        .collect(Collectors.toList());
    }

    private List<ComponentDto> searchByComponentKeys(DbSession dbSession, List<String> componentKeys) {
      return dbClient.componentDao().selectByKeys(dbSession, componentKeys);
    }

    private List<MeasureDto> searchMeasures() {
      requireNonNull(components);
      requireNonNull(metrics);

      return dbClient.measureDao().selectByQuery(dbSession, MeasureQuery.builder()
        .setComponentUuids(components.stream().map(ComponentDto::uuid).collect(toList()))
        .setMetricIds(metrics.stream().map(MetricDto::getId).collect(toList()))
        .build());
    }

    private List<String> difference(Collection<String> expected, Collection<String> actual) {
      Set<String> actualSet = new HashSet<>(actual);

      return expected.stream()
        .filter(value -> !actualSet.contains(value))
        .sorted(String::compareTo)
        .collect(toList());
    }

    private SearchWsResponse buildResponse() {
      requireNonNull(metrics);
      requireNonNull(measures);
      requireNonNull(components);
      requireNonNull(snapshots);

      List<Measure> wsMeasures = buildWsMeasures();
      List<Component> wsComponents = buildWsComponents();

      return SearchWsResponse.newBuilder()
        .addAllMeasures(wsMeasures)
        .addAllComponents(wsComponents)
        .build();
    }

    private List<Component> buildWsComponents() {
      return components.stream()
        .map(dbToWsComponent())
        .sorted(comparing(Component::getName))
        .collect(toList());
    }

    private List<Measure> buildWsMeasures() {
      Map<String, String> componentNamesByUuid = components.stream().collect(toMap(ComponentDto::uuid, ComponentDto::name));
      Map<Integer, MetricDto> metricsById = metrics.stream().collect(toMap(MetricDto::getId, identity()));

      Function<MeasureDto, MetricDto> dbMeasureToDbMetric = dbMeasure -> metricsById.get(dbMeasure.getMetricId());
      Function<Measure, String> byMetricKey = Measure::getMetric;
      Function<Measure, String> byComponentName = wsMeasure -> componentNamesByUuid.get(wsMeasure.getComponent());

      return Stream
        .concat(measures.stream(), buildBestMeasures().stream())
        .map(dbMeasure -> dbToWsMeasure(dbMeasure, dbMeasureToDbMetric.apply(dbMeasure)))
        .sorted(comparing(byMetricKey).thenComparing(byComponentName))
        .collect(toList());
    }

    private List<MeasureDto> buildBestMeasures() {
      Set<MetricDto> metricsWithBestValue = metrics.stream()
        .filter(metric -> metric.isOptimizedBestValue() && metric.getBestValue() != null)
        .collect(toSet());

      Multimap<String, WsMeasures.Period> wsPeriodsByProjectUuid = snapshots.stream().collect(Collector.of(
        ImmutableMultimap::<String, WsMeasures.Period>builder,
        (result, snapshot) -> result.putAll(snapshot.getComponentUuid(), snapshotToWsPeriods(snapshot)),
        (result1, result2) -> {
          throw new IllegalStateException("Parallel execution forbidden");
        },
        ImmutableMultimap.Builder::build));

      Table<String, Integer, MeasureDto> measuresByComponentUuidAndMetricId = measures.stream().collect(Collector.of(
        ImmutableTable::<String, Integer, MeasureDto>builder,
        (result, measure) -> result.put(measure.getComponentUuid(), measure.getMetricId(), measure),
        (result1, result2) -> {
          throw new IllegalStateException("Parallel execution forbidden");
        },
        ImmutableTable.Builder::build));

      Function<ComponentDto, Predicate<MetricDto>> doesNotHaveAMeasureInDb = component -> metric -> !measuresByComponentUuidAndMetricId.contains(component.uuid(), metric.getId());
      return components.stream()
        .filter(isEligibleForBestValue())
        .flatMap(component -> metricsWithBestValue.stream()
          .filter(doesNotHaveAMeasureInDb.apply(component))
          .map(buildBestMeasure(component, wsPeriodsByProjectUuid.get(component.projectUuid()))))
        .collect(toList());
    }
  }
}
