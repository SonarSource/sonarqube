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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Map;
import java.util.Set;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery.Strategy;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.ComponentTreeWsResponse;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createAdditionalFieldsParameter;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createDeveloperParameters;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.measure.ws.MetricDtoToWsMetric.metricDtoToWsMetric;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ACTION_COMPONENT_TREE;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ADDITIONAL_METRICS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ADDITIONAL_PERIODS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BASE_COMPONENT_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_PERIOD_SORT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_SORT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_SORT_FILTER;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_STRATEGY;

/**
 * <p>Navigate through components based on different strategy with specified measures.
 * To limit the number of rows in database, a best value algorithm exists in database.</p>
 * A measure is not stored in database if:
 * <ul>
 *   <li>the component is a file (production or test)</li>
 *   <li>optimization algorithm is enabled on the metric</li>
 *   <li>the measure computed equals the metric best value</li>
 *   <li>the period values are all equal to 0</li>
 * </ul>
 * To recreate a best value 2 different cases:
 * <ul>
 *   <li>Metric starts with 'new_' (ex: new_violations): the best value measure doesn't have a value and period values are all equal to 0</li>
 *   <li>Other metrics: the best value measure has a value of 0 and no period value</li>
 * </ul>
 */
public class ComponentTreeAction implements MeasuresWsAction {
  private static final int MAX_SIZE = 500;
  private static final int QUERY_MINIMUM_LENGTH = 3;
  // tree exploration strategies
  static final String ALL_STRATEGY = "all";
  static final String CHILDREN_STRATEGY = "children";
  static final String LEAVES_STRATEGY = "leaves";
  static final Map<String, Strategy> STRATEGIES = ImmutableMap.of(
    ALL_STRATEGY, LEAVES,
    CHILDREN_STRATEGY, CHILDREN,
    LEAVES_STRATEGY, LEAVES);
  // sort
  static final String NAME_SORT = "name";
  static final String PATH_SORT = "path";
  static final String QUALIFIER_SORT = "qualifier";
  static final String METRIC_SORT = "metric";
  static final String METRIC_PERIOD_SORT = "metricPeriod";
  static final Set<String> SORTS = ImmutableSortedSet.of(NAME_SORT, PATH_SORT, QUALIFIER_SORT, METRIC_SORT, METRIC_PERIOD_SORT);
  static final String ALL_METRIC_SORT_FILTER = "all";
  static final String WITH_MEASURES_ONLY_METRIC_SORT_FILTER = "withMeasuresOnly";
  static final Set<String> METRIC_SORT_FILTERS = ImmutableSortedSet.of(ALL_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER);

  private final ComponentTreeDataLoader dataLoader;
  private final I18n i18n;
  private final ResourceTypes resourceTypes;

  public ComponentTreeAction(ComponentTreeDataLoader dataLoader, I18n i18n,
    ResourceTypes resourceTypes) {
    this.dataLoader = dataLoader;
    this.i18n = i18n;
    this.resourceTypes = resourceTypes;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_COMPONENT_TREE)
      .setDescription(format("Navigate through components based on the chosen strategy with specified measures. The %s or the %s parameter must be provided.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer System'</li>" +
        "  <li>'Administer' rights on the specified project</li>" +
        "  <li>'Browse' on the specified project</li>" +
        "</ul>" +
        "When limiting search with the %s parameter, directories are not returned.",
        PARAM_BASE_COMPONENT_ID, PARAM_BASE_COMPONENT_KEY, Param.TEXT_QUERY))
      .setResponseExample(getClass().getResource("component_tree-example.json"))
      .setSince("5.4")
      .setHandler(this)
      .addPagingParams(100, MAX_SIZE);

    action.createSortParams(SORTS, NAME_SORT, true)
      .setDescription("Comma-separated list of sort fields")
      .setExampleValue(NAME_SORT + "," + PATH_SORT);

    action.createParam(Param.TEXT_QUERY)
      .setDescription(format("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that are exactly the same as the supplied string</li>" +
        "</ul>" +
        "Must have at least %d characters.", QUERY_MINIMUM_LENGTH))
      .setExampleValue("FILE_NAM");

    action.createParam(PARAM_BASE_COMPONENT_ID)
      .setDescription("Base component id. The search is based on this component.")
      .setExampleValue(UUID_EXAMPLE_02);

    action.createParam(PARAM_BASE_COMPONENT_KEY)
      .setDescription("Base component key. The search is based on this component.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_METRIC_SORT)
      .setDescription(
        format("Metric key to sort by. The '%s' parameter must contain the '%s' or '%s' value. It must be part of the '%s' parameter", Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT,
          PARAM_METRIC_KEYS))
      .setExampleValue("ncloc");

    action.createParam(PARAM_METRIC_PERIOD_SORT)
      .setDescription(format("Measure period to sort by. The '%s' parameter must contain the '%s' value.", Param.SORT, METRIC_PERIOD_SORT))
      .setSince("5.5")
      .setPossibleValues(1, 2, 3, 4, 5);

    action.createParam(PARAM_METRIC_SORT_FILTER)
      .setDescription(format("Filter components. Sort must be on a metric. Possible values are: " +
        "<ul>" +
        "<li>%s: return all components</li>" +
        "<li>%s: filter out components that do not have a measure on the sorted metric</li>" +
        "</ul>", ALL_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER))
      .setDefaultValue(ALL_METRIC_SORT_FILTER)
      .setPossibleValues(METRIC_SORT_FILTERS);

    createMetricKeysParameter(action);
    createAdditionalFieldsParameter(action);
    createDeveloperParameters(action);
    createQualifiersParameter(action, newQualifierParameterContext(i18n, resourceTypes));

    action.createParam(PARAM_STRATEGY)
      .setDescription("Strategy to search for base component descendants:" +
        "<ul>" +
        "<li>children: return the children components of the base component. Grandchildren components are not returned</li>" +
        "<li>all: return all the descendants components of the base component. Grandchildren are returned.</li>" +
        "<li>leaves: return all the descendant components (files, in general) which don't have other children. They are the leaves of the component tree.</li>" +
        "</ul>")
      .setPossibleValues(STRATEGIES.keySet())
      .setDefaultValue(ALL_STRATEGY);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ComponentTreeWsResponse componentTreeWsResponse = doHandle(toComponentTreeWsRequest(request));
    writeProtobuf(componentTreeWsResponse, request, response);
  }

  private ComponentTreeWsResponse doHandle(ComponentTreeWsRequest request) {
    ComponentTreeData data = dataLoader.load(request);
    if (data.getComponents() == null) {
      return emptyResponse(data.getBaseComponent(), request);
    }

    return buildResponse(
      request,
      data,
      Paging.forPageIndex(
        request.getPage())
        .withPageSize(request.getPageSize())
        .andTotal(data.getComponentCount()));
  }

  private static ComponentTreeWsResponse buildResponse(ComponentTreeWsRequest request, ComponentTreeData data, Paging paging) {
    ComponentTreeWsResponse.Builder response = ComponentTreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    response.setBaseComponent(
      componentDtoToWsComponent(
        data.getBaseComponent(),
        data.getMeasuresByComponentUuidAndMetric().row(data.getBaseComponent().uuid()),
        data.getReferenceComponentsByUuid()));

    for (ComponentDto componentDto : data.getComponents()) {
      response.addComponents(componentDtoToWsComponent(
        componentDto,
        data.getMeasuresByComponentUuidAndMetric().row(componentDto.uuid()),
        data.getReferenceComponentsByUuid()));
    }

    if (areMetricsInResponse(request)) {
      WsMeasures.Metrics.Builder metricsBuilder = response.getMetricsBuilder();
      for (MetricDto metricDto : data.getMetrics()) {
        metricsBuilder.addMetrics(metricDtoToWsMetric(metricDto));
      }
    }

    if (arePeriodsInResponse(request)) {
      response.getPeriodsBuilder().addAllPeriods(data.getPeriods());
    }

    return response.build();
  }

  private static boolean areMetricsInResponse(ComponentTreeWsRequest request) {
    return request.getAdditionalFields() != null && request.getAdditionalFields().contains(ADDITIONAL_METRICS);
  }

  private static boolean arePeriodsInResponse(ComponentTreeWsRequest request) {
    return request.getAdditionalFields() != null && request.getAdditionalFields().contains(ADDITIONAL_PERIODS);
  }

  private static ComponentTreeWsResponse emptyResponse(ComponentDto baseComponent, ComponentTreeWsRequest request) {
    ComponentTreeWsResponse.Builder response = ComponentTreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setTotal(0);
    response.setBaseComponent(componentDtoToWsComponent(baseComponent));
    return response.build();
  }

  private static ComponentTreeWsRequest toComponentTreeWsRequest(Request request) {
    ComponentTreeWsRequest componentTreeWsRequest = new ComponentTreeWsRequest()
      .setBaseComponentId(request.param(PARAM_BASE_COMPONENT_ID))
      .setBaseComponentKey(request.param(PARAM_BASE_COMPONENT_KEY))
      .setMetricKeys(request.mandatoryParamAsStrings(PARAM_METRIC_KEYS))
      .setStrategy(request.mandatoryParam(PARAM_STRATEGY))
      .setQualifiers(request.paramAsStrings(PARAM_QUALIFIERS))
      .setAdditionalFields(request.paramAsStrings(PARAM_ADDITIONAL_FIELDS))
      .setSort(request.paramAsStrings(Param.SORT))
      .setAsc(request.paramAsBoolean(Param.ASCENDING))
      .setMetricSort(request.param(PARAM_METRIC_SORT))
      .setMetricSortFilter(request.mandatoryParam(PARAM_METRIC_SORT_FILTER))
      .setMetricPeriodSort(request.paramAsInt(PARAM_METRIC_PERIOD_SORT))
      .setDeveloperId(request.param(PARAM_DEVELOPER_ID))
      .setDeveloperKey(request.param(PARAM_DEVELOPER_KEY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setQuery(request.param(Param.TEXT_QUERY));
    checkRequest(componentTreeWsRequest.getPageSize() <= MAX_SIZE, "The '%s' parameter must be less than %d", Param.PAGE_SIZE, MAX_SIZE);
    String searchQuery = componentTreeWsRequest.getQuery();
    checkRequest(searchQuery == null || searchQuery.length() >= QUERY_MINIMUM_LENGTH,
      "The '%s' parameter must have at least %d characters", Param.TEXT_QUERY, QUERY_MINIMUM_LENGTH);
    String metricSortValue = componentTreeWsRequest.getMetricSort();
    checkRequest(!componentTreeWsRequest.getMetricKeys().isEmpty(), "The '%s' parameter must contain at least one metric key", PARAM_METRIC_KEYS);
    checkRequest(metricSortValue == null ^ componentTreeWsRequest.getSort().contains(METRIC_SORT)
      ^ componentTreeWsRequest.getSort().contains(METRIC_PERIOD_SORT),
      "To sort by a metric, the '%s' parameter must contain '%s' or '%s', and a metric key must be provided in the '%s' parameter",
      Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT, PARAM_METRIC_SORT);
    checkRequest(metricSortValue == null ^ componentTreeWsRequest.getMetricKeys().contains(metricSortValue),
      "To sort by the '%s' metric, it must be in the list of metric keys in the '%s' parameter", metricSortValue, PARAM_METRIC_KEYS);
    checkRequest(componentTreeWsRequest.getMetricPeriodSort() == null ^ componentTreeWsRequest.getSort().contains(METRIC_PERIOD_SORT),
      "To sort by a metric period, the '%s' parameter must contain '%s' and the '%s' must be provided.", Param.SORT, METRIC_PERIOD_SORT, PARAM_METRIC_PERIOD_SORT);
    checkRequest(ALL_METRIC_SORT_FILTER.equals(componentTreeWsRequest.getMetricSortFilter()) || metricSortValue != null,
      "To filter components based on the sort metric, the '%s' parameter must contain '%s' or '%s' and the '%s' parameter must be provided",
      Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT, PARAM_METRIC_SORT);
    return componentTreeWsRequest;
  }
}
