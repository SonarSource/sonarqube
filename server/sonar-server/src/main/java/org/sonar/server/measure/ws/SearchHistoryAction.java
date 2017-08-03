/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.db.component.SnapshotQuery.SORT_FIELD;
import org.sonar.db.component.SnapshotQuery.SORT_ORDER;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.PastMeasureQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.WsMeasures.SearchHistoryResponse;
import org.sonarqube.ws.client.measure.SearchHistoryRequest;

import static java.lang.String.format;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ACTION_SEARCH_HISTORY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRICS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_TO;
import static org.sonarqube.ws.client.measure.SearchHistoryRequest.DEFAULT_PAGE_SIZE;
import static org.sonarqube.ws.client.measure.SearchHistoryRequest.MAX_PAGE_SIZE;

public class SearchHistoryAction implements MeasuresWsAction {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public SearchHistoryAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  private static SearchHistoryRequest toWsRequest(Request request) {
    return SearchHistoryRequest.builder()
      .setComponent(request.mandatoryParam(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setMetrics(request.mandatoryParamAsStrings(PARAM_METRICS))
      .setFrom(request.param(PARAM_FROM))
      .setTo(request.param(PARAM_TO))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .build();
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH_HISTORY)
      .setDescription("Search measures history of a component.<br>" +
        "Measures are ordered chronologically.<br>" +
        "Pagination applies to the number of measures for each metric.<br>" +
        "Requires the following permission: 'Browse' on the specified component")
      .setResponseExample(getClass().getResource("search_history-example.json"))
      .setSince("6.3")
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setRequired(true)
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setSince("6.6")
      .setInternal(true)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action.createParam(PARAM_METRICS)
      .setDescription("Comma-separated list of metric keys")
      .setRequired(true)
      .setExampleValue("ncloc,coverage,new_violations");

    action.createParam(PARAM_FROM)
      .setDescription("Filter measures created after the given date (inclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01T13:00:00+0100");

    action.createParam(PARAM_TO)
      .setDescription("Filter measures created before the given date (inclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01");

    action.addPagingParams(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchHistoryResponse searchHistoryResponse = Stream.of(request)
      .map(SearchHistoryAction::toWsRequest)
      .map(search())
      .map(result -> new SearchHistoryResponseFactory(result).apply())
      .collect(MoreCollectors.toOneElement());

    writeProtobuf(searchHistoryResponse, request, response);
  }

  private Function<SearchHistoryRequest, SearchHistoryResult> search() {
    return request -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        ComponentDto component = searchComponent(request, dbSession);

        SearchHistoryResult result = new SearchHistoryResult(request)
          .setComponent(component)
          .setAnalyses(searchAnalyses(dbSession, request, component))
          .setMetrics(searchMetrics(dbSession, request));
        return result.setMeasures(searchMeasures(dbSession, request, result));
      }
    };
  }

  private ComponentDto searchComponent(SearchHistoryRequest request, DbSession dbSession) {
    ComponentDto component = loadComponent(dbSession, request);
    userSession.checkComponentPermission(UserRole.USER, component);
    return component;
  }

  private List<MeasureDto> searchMeasures(DbSession dbSession, SearchHistoryRequest request, SearchHistoryResult result) {
    Date from = parseStartingDateOrDateTime(request.getFrom());
    Date to = parseEndingDateOrDateTime(request.getTo());
    PastMeasureQuery dbQuery = new PastMeasureQuery(
      result.getComponent().uuid(),
      result.getMetrics().stream().map(MetricDto::getId).collect(MoreCollectors.toList()),
      from == null ? null : from.getTime(),
      to == null ? null : (to.getTime() + 1_000L));
    return dbClient.measureDao().selectPastMeasures(dbSession, dbQuery);
  }

  private List<SnapshotDto> searchAnalyses(DbSession dbSession, SearchHistoryRequest request, ComponentDto component) {
    SnapshotQuery dbQuery = new SnapshotQuery()
      .setComponentUuid(component.projectUuid())
      .setStatus(STATUS_PROCESSED)
      .setSort(SORT_FIELD.BY_DATE, SORT_ORDER.ASC);
    setNullable(request.getFrom(), from -> dbQuery.setCreatedAfter(parseStartingDateOrDateTime(from).getTime()));
    setNullable(request.getTo(), to -> dbQuery.setCreatedBefore(parseEndingDateOrDateTime(to).getTime() + 1_000L));

    return dbClient.snapshotDao().selectAnalysesByQuery(dbSession, dbQuery);
  }

  private List<MetricDto> searchMetrics(DbSession dbSession, SearchHistoryRequest request) {
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, request.getMetrics());
    if (request.getMetrics().size() > metrics.size()) {
      Set<String> requestedMetrics = request.getMetrics().stream().collect(MoreCollectors.toSet());
      Set<String> foundMetrics = metrics.stream().map(MetricDto::getKey).collect(MoreCollectors.toSet());

      Set<String> unfoundMetrics = Sets.difference(requestedMetrics, foundMetrics).immutableCopy();
      throw new IllegalArgumentException(format("Metrics %s are not found", String.join(", ", unfoundMetrics)));
    }

    return metrics;
  }

  private ComponentDto loadComponent(DbSession dbSession, SearchHistoryRequest request) {
    String componentKey = request.getComponent();
    String branch = request.getBranch();
    if (branch != null) {
      return componentFinder.getByKeyAndBranch(dbSession, componentKey, branch);
    }
    return componentFinder.getByKey(dbSession, componentKey);
  }

}
