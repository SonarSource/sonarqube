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
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.SearchWsResponse;
import org.sonarqube.ws.client.measure.SearchRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_03;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.dbToWsComponent;
import static org.sonar.server.measure.ws.MeasureDtoToWsMeasure.measureDtoToWsMeasure;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_002;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;

public class SearchAction implements MeasuresWsAction {
  private static final int MAX_NB_COMPONENTS = 100;
  static final String PARAM_COMPONENT_IDS = "componentIds";
  static final String PARAM_COMPONENT_KEYS = "componentKeys";

  private final DbClient dbClient;

  public SearchAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search")
      .setInternal(true)
      .setDescription("Search for component measures ordered by component names.<br>" +
        "At most %d components can be provided.<br>" +
        "Either '%s' or '%s' must be provided, not both.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        " <li>'Administer System'</li>" +
        " <li>'Administer' rights on the provided components</li>" +
        " <li>'Browse' on the provided components</li>" +
        "</ul>",
        MAX_NB_COMPONENTS, PARAM_COMPONENT_IDS, PARAM_COMPONENT_KEYS)
      .setSince("6.1")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setHandler(this);

    createMetricKeysParameter(action);

    action.createParam(PARAM_COMPONENT_IDS)
      .setDescription("Comma-separated list of component ids")
      .setExampleValue(String.join(",", UUID_EXAMPLE_01, UUID_EXAMPLE_02, UUID_EXAMPLE_03));

    action.createParam(PARAM_COMPONENT_KEYS)
      .setDescription("Comma-separated list of component keys")
      .setExampleValue(String.join(",", KEY_PROJECT_EXAMPLE_001, KEY_FILE_EXAMPLE_001, KEY_PROJECT_EXAMPLE_002, KEY_FILE_EXAMPLE_002));

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

    ResponseBuilder(Request httpRequest, DbSession dbSession) {
      this.dbSession = dbSession;
      this.httpRequest = httpRequest;
    }

    SearchWsResponse build() {
      this.request = setRequest();
      this.components = searchComponents();
      this.metrics = searchMetrics();
      this.measures = searchMeasures();

      return buildResponse();
    }

    private SearchRequest setRequest() {
      request = SearchRequest.builder()
        .setMetricKeys(httpRequest.mandatoryParamAsStrings(PARAM_METRIC_KEYS))
        .setComponentIds(httpRequest.paramAsStrings(PARAM_COMPONENT_IDS))
        .setComponentKeys(httpRequest.paramAsStrings(PARAM_COMPONENT_KEYS))
        .build();

      this.components = searchComponents();
      this.metrics = searchMetrics();

      return request;
    }

    private List<MetricDto> searchMetrics() {
      requireNonNull(request);
      return dbClient.metricDao().selectByKeys(dbSession, request.getMetricKeys());
    }

    private List<ComponentDto> searchComponents() {
      requireNonNull(request);
      if (request.hasComponentIds()) {
        List<ComponentDto> componentsByUuid = searchByComponentUuids(dbSession, request.getComponentIds());
        checkArgument(componentsByUuid.size() == request.getComponentIds().size(), "Some components are not found in: '%s'", String.join(", ", request.getComponentIds()));
        return componentsByUuid;
      } else {
        List<ComponentDto> componentsByKey = searchByComponentKeys(dbSession, request.getComponentKeys());
        checkArgument(componentsByKey.size() == request.getComponentKeys().size(), "Some components are not found in: '%s'", String.join(", ", request.getComponentKeys()));
        return componentsByKey;
      }
    }

    private List<ComponentDto> searchByComponentUuids(DbSession dbSession, List<String> componentUuids) {
      return dbClient.componentDao().selectByUuids(dbSession, componentUuids);
    }

    private List<ComponentDto> searchByComponentKeys(DbSession dbSession, List<String> componentKeys) {
      return dbClient.componentDao().selectByKeys(dbSession, componentKeys);
    }

    private List<MeasureDto> searchMeasures() {
      requireNonNull(components);
      requireNonNull(metrics);

      return dbClient.measureDao().selectByQuery(dbSession, MeasureQuery.builder()
        .setComponentUuids(components.stream().map(ComponentDto::uuid).collect(Collectors.toList()))
        .setMetricIds(metrics.stream().map(MetricDto::getId).collect(Collectors.toList()))
        .build());
    }

    private SearchWsResponse buildResponse() {
      requireNonNull(metrics);
      requireNonNull(measures);
      requireNonNull(components);

      Map<Integer, MetricDto> metricById = metrics.stream().collect(Collectors.toMap(MetricDto::getId, identity()));

      ImmutableMultimap<String, WsMeasures.Measure> wsMeasuresByComponentUuid = measures.stream()
        .collect(Collectors.toMap(identity(), MeasureDto::getComponentUuid))
        .entrySet().stream()
        .map(entry -> immutableEntry(
          measureDtoToWsMeasure(metricById.get(entry.getKey().getMetricId()), entry.getKey()),
          entry.getValue()))
        .sorted((e1, e2) -> e1.getKey().getMetric().compareTo(e2.getKey().getMetric()))
        .collect(Collector.of(
          ImmutableMultimap::<String, WsMeasures.Measure>builder,
          (result, entry) -> result.put(entry.getValue(), entry.getKey()),
          (result1, result2) -> {
            throw new IllegalStateException("Parallel execution forbidden during WS measures");
          },
          ImmutableMultimap.Builder::build));

      return components.stream()
        .map(dbComponent -> dbToWsComponent(dbComponent, wsMeasuresByComponentUuid.get(dbComponent.uuid())))
        .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
        .collect(Collector.of(
          SearchWsResponse::newBuilder,
          SearchWsResponse.Builder::addComponents,
          (result1, result2) -> {
            throw new IllegalStateException("Parallel execution forbidden while build SearchWsResponse");
          },
          SearchWsResponse.Builder::build));
    }

  }
}
