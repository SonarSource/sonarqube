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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.ComponentTreeQuery.Strategy;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureTreeQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.ComponentTreeWsResponse;
import org.sonarqube.ws.client.component.ComponentsWsParameters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;
import static org.sonar.db.metric.RemovedMetricConverter.includeRenamedMetrics;
import static org.sonar.db.metric.RemovedMetricConverter.withRemovedMetricAlias;
import static org.sonar.server.component.ws.MeasuresWsParameters.ACTION_COMPONENT_TREE;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_METRICS;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_PERIOD;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_PERIOD_SORT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_SORT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_SORT_FILTER;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_QUALIFIERS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_STRATEGY;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.measure.ws.ComponentResponseCommon.addMeasureIncludingRenamedMetric;
import static org.sonar.server.measure.ws.ComponentResponseCommon.addMetricToResponseIncludingRenamedMetric;
import static org.sonar.server.measure.ws.MeasureDtoToWsMeasure.updateMeasureBuilder;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createAdditionalFieldsParameter;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriod.snapshotToWsPeriods;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * <p>Navigate through components based on different strategy with specified measures.
 * To limit the number of rows in database, a best value algorithm exists in database.</p>
 * A measure is not stored in database if:
 * <ul>
 * <li>the component is a file (production or test)</li>
 * <li>optimization algorithm is enabled on the metric</li>
 * <li>the measure computed equals the metric best value</li>
 * <li>the period values are all equal to 0</li>
 * </ul>
 * To recreate a best value 2 different cases:
 * <ul>
 * <li>Metric starts with 'new_' (ex: new_violations): the best value measure doesn't have a value and period values are all equal to 0</li>
 * <li>Other metrics: the best value measure has a value of 0 and no period value</li>
 * </ul>
 */
public class ComponentTreeAction implements MeasuresWsAction {
  private static final int MAX_SIZE = 500;
  private static final String NUMBER_OF_KEYS_LIMITED = "Number of metric keys is limited to %s";
  private static final int QUERY_MINIMUM_LENGTH = 3;
  // tree exploration strategies
  static final String ALL_STRATEGY = "all";
  static final String CHILDREN_STRATEGY = "children";
  static final String LEAVES_STRATEGY = "leaves";
  static final Map<String, Strategy> STRATEGIES = Map.of(
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
  private static final int MAX_METRIC_KEYS = 75;
  private static final String COMMA_JOIN_SEPARATOR = ", ";
  private static final Set<String> QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE = Set.of(ComponentQualifiers.FILE, ComponentQualifiers.UNIT_TEST_FILE);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final I18n i18n;
  private final ComponentTypes componentTypes;

  public ComponentTreeAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, I18n i18n,
    ComponentTypes componentTypes) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.i18n = i18n;
    this.componentTypes = componentTypes;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_COMPONENT_TREE)
      .setDescription(format("Navigate through components based on the chosen strategy with specified measures.<br>" +
        "Requires the following permission: 'Browse' on the specified project.<br>" +
        "For applications, it also requires 'Browse' permission on its child projects. <br>" +
        "When limiting search with the %s parameter, directories are not returned.", Param.TEXT_QUERY))
      .setResponseExample(getClass().getResource("component_tree-example.json"))
      .setSince("5.4")
      .setHandler(this)
      .addPagingParams(100, MAX_SIZE)
      .setChangelog(
        new Change("10.8", format(NUMBER_OF_KEYS_LIMITED, 75)),
        new Change("10.8", "Portfolio project metrics now also include: 'contains_ai_code', 'reliability_rating_without_aica', " +
          "'reliability_rating_with_aica', 'software_quality_security_rating_without_aica', 'software_quality_security_rating_with_aica', " +
          "'security_rating_without_aica', 'security_rating_with_aica', 'new_reliability_rating_without_aica', 'new_reliability_rating_with_aica', " +
          "'new_software_quality_reliability_rating_without_aica', 'new_software_quality_reliability_rating_with_aica', 'new_security_rating_without_aica', " +
          "'new_security_rating_with_aica', 'new_software_quality_security_rating_without_aica', 'new_software_quality_security_rating_with_aica', " +
          "'security_review_rating_without_aica', 'security_review_rating_with_aica', 'sqale_rating_without_aica', 'sqale_rating_with_aica', " +
          "'new_software_quality_maintainability_rating_without_aica', 'new_software_quality_maintainability_rating_with_aica', 'ncloc_without_aica', " +
          "'ncloc_with_aica', 'software_quality_reliability_rating_without_aica', 'software_quality_reliability_rating_with_aica', " +
          "'new_maintainability_rating_without_aica', 'new_maintainability_rating_with_aica', 'software_quality_maintainability_rating_without_aica', " +
          "'software_quality_maintainability_rating_with_aica', 'new_security_review_rating_without_aica', 'new_security_review_rating_with_aica', " +
          "'releasability_rating_without_aica', 'releasability_rating_with_aica'"),
        new Change("10.8", String.format("The following metrics are not deprecated anymore: %s",
          MeasuresWsModule.getUndeprecatedMetricsinSonarQube108())),
        new Change("10.8", String.format("Added new accepted values for the 'metricKeys' param: %s",
          MeasuresWsModule.getNewMetricsInSonarQube108())),
        new Change("10.8", String.format("The metrics %s are now deprecated. Use 'software_quality_maintainability_issues', " +
          "'software_quality_reliability_issues', 'software_quality_security_issues', 'new_software_quality_maintainability_issues', " +
          "'new_software_quality_reliability_issues', 'new_software_quality_security_issues' instead.",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube108())),
        new Change("10.7", format(NUMBER_OF_KEYS_LIMITED, 25)),
        new Change("10.7",
          "Added new accepted values for the 'metricKeys' param: %s".formatted(MeasuresWsModule.getNewMetricsInSonarQube107())),
        new Change("10.5", "Added new accepted values for the 'metricKeys' param: 'new_maintainability_issues', 'new_reliability_issues'," +
          " 'new_security_issues'"),
        new Change("10.5", format("The metrics %s are now deprecated " +
          "without exact replacement. Use 'maintainability_issues', 'reliability_issues' and 'security_issues' instead.",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube105())),
        new Change("10.5", "Added new accepted values for the 'metricKeys' param: 'maintainability_issues', 'reliability_issues', " +
          "'security_issues'"),
        new Change("10.4", format("The metrics %s are now deprecated " +
          "without exact replacement. Use 'maintainability_issues', 'reliability_issues' and 'security_issues' instead.",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube104())),
        new Change("10.4", "The metrics 'open_issues', 'reopened_issues' and 'confirmed_issues' are now deprecated in the response. " +
          "Consume 'violations' instead."),
        new Change("10.4", "The use of 'open_issues', 'reopened_issues' and 'confirmed_issues' values in 'metricKeys' param are now " +
          "deprecated. Use 'violations' instead."),
        new Change("10.4", "The metric 'wont_fix_issues' is now deprecated in the response. Consume 'accepted_issues' instead."),
        new Change("10.4", "The use of 'wont_fix_issues' value in 'metricKeys' and 'metricSort' params is now deprecated. Use " +
          "'accepted_issues' instead."),
        new Change("10.4", "Added new accepted value for the 'metricKeys' and 'metricSort' param: 'accepted_issues'."),
        new Change("10.1", format("The use of 'BRC' as value for parameter '%s' is removed",
          ComponentsWsParameters.PARAM_QUALIFIERS)),
        new Change("10.0", format("The use of the following metrics in 'metricKeys' parameter is not deprecated anymore: %s",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube93())),
        new Change("10.0", "the response field periods under measures field is removed."),
        new Change("10.0", "the option `periods` of 'additionalFields' request field is removed."),
        new Change("9.3", format("The use of the following metrics in 'metricKeys' parameter is deprecated: %s",
          MeasuresWsModule.getDeprecatedMetricsInSonarQube93())),
        new Change("8.8", "parameter 'component' is now required"),
        new Change("8.8", "deprecated parameter 'baseComponentId' has been removed"),
        new Change("8.8", "deprecated parameter 'baseComponentKey' has been removed."),
        new Change("8.8", "deprecated response field 'id' has been removed"),
        new Change("8.8", "deprecated response field 'refId' has been removed."),
        new Change("8.1", "the response field periods under measures field is deprecated. Use period instead."),
        new Change("8.1", "the response field periods is deprecated. Use period instead."),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
        new Change("7.2", "field 'bestValue' is added to the response"),
        new Change("6.3", format(NUMBER_OF_KEYS_LIMITED, 15)),
        new Change("6.6", "the response field 'id' is deprecated. Use 'key' instead."),
        new Change("6.6", "the response field 'refId' is deprecated. Use 'refKey' instead."));

    action.createSortParams(SORTS, NAME_SORT, true)
      .setDescription("Comma-separated list of sort fields")
      .setExampleValue(NAME_SORT + "," + PATH_SORT);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that are exactly the same as the supplied string</li>" +
        "</ul>")
      .setMinimumLength(QUERY_MINIMUM_LENGTH)
      .setExampleValue("FILE_NAM");

    action.createParam(PARAM_COMPONENT)
      .setRequired(true)
      .setDescription("Component key. The search is based on this component.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setSince("6.6");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setSince("7.1");

    action.createParam(PARAM_METRIC_SORT)
      .setDescription(
        format("Metric key to sort by. The '%s' parameter must contain the '%s' or '%s' value. It must be part of the '%s' parameter",
          Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT,
          PARAM_METRIC_KEYS))
      .setExampleValue("ncloc");

    action.createParam(PARAM_METRIC_PERIOD_SORT)
      .setDescription(format("Sort measures by leak period or not ?. The '%s' parameter must contain the '%s' value.", Param.SORT,
        METRIC_PERIOD_SORT))
      .setSince("5.5")
      .setPossibleValues(1);

    action.createParam(PARAM_METRIC_SORT_FILTER)
      .setDescription(format("Filter components. Sort must be on a metric. Possible values are: " +
        "<ul>" +
        "<li>%s: return all components</li>" +
        "<li>%s: filter out components that do not have a measure on the sorted metric</li>" +
        "</ul>", ALL_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER))
      .setDefaultValue(ALL_METRIC_SORT_FILTER)
      .setPossibleValues(METRIC_SORT_FILTERS);

    createMetricKeysParameter(action)
      .setDescription("Comma-separated list of metric keys. Types %s are not allowed. For type %s only %s metrics are supported",
        join(COMMA_JOIN_SEPARATOR, UnsupportedMetrics.FORBIDDEN_METRIC_TYPES),
        DATA.name(),
        join(COMMA_JOIN_SEPARATOR, UnsupportedMetrics.PARTIALLY_SUPPORTED_METRICS.get(DATA.name())))
      .setMaxValuesAllowed(MAX_METRIC_KEYS);
    createAdditionalFieldsParameter(action);
    createQualifiersParameter(action, newQualifierParameterContext(i18n, componentTypes));

    action.createParam(PARAM_STRATEGY)
      .setDescription("Strategy to search for base component descendants:" +
        "<ul>" +
        "<li>children: return the children components of the base component. Grandchildren components are not returned</li>" +
        "<li>all: return all the descendants components of the base component. Grandchildren are returned.</li>" +
        "<li>leaves: return all the descendant components (files, in general) which don't have other children. They are the leaves of the" +
        " component tree.</li>" +
        "</ul>")
      .setPossibleValues(STRATEGIES.keySet())
      .setDefaultValue(ALL_STRATEGY);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ComponentTreeWsResponse componentTreeWsResponse = doHandle(toComponentTreeWsRequest(request));
    writeProtobuf(componentTreeWsResponse, request, response);
  }

  private ComponentTreeWsResponse doHandle(ComponentTreeRequest request) {
    ComponentTreeData data = load(request);
    if (data.getComponents() == null) {
      return emptyResponse(data.getBaseComponent(), data.getBranch(), request);
    }

    return buildResponse(
      request,
      data,
      Paging.forPageIndex(
        request.getPage())
        .withPageSize(request.getPageSize())
        .andTotal(data.getComponentCount()),
      request.getMetricKeys());
  }

  private static ComponentTreeWsResponse buildResponse(ComponentTreeRequest request, ComponentTreeData data, Paging paging,
    List<String> requestedMetrics) {
    ComponentTreeWsResponse.Builder response = ComponentTreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    boolean isMainBranch = data.getBranch() == null || data.getBranch().isMain();
    response.setBaseComponent(
      toWsComponent(
        data.getBaseComponent(),
        data.getMeasuresByComponentUuidAndMetric().row(data.getBaseComponent().uuid()),
        data.getReferenceComponentsByUuid(), isMainBranch ? null : request.getBranch(), request.getPullRequest(), requestedMetrics));

    for (ComponentDto componentDto : data.getComponents()) {
      if (componentDto.getCopyComponentUuid() != null) {
        String refBranch = data.getBranchByReferenceUuid().get(componentDto.getCopyComponentUuid());
        response.addComponents(toWsComponent(
          componentDto,
          data.getMeasuresByComponentUuidAndMetric().row(componentDto.uuid()),
          data.getReferenceComponentsByUuid(), refBranch, null, requestedMetrics));
      } else {
        response.addComponents(toWsComponent(
          componentDto,
          data.getMeasuresByComponentUuidAndMetric().row(componentDto.uuid()),
          data.getReferenceComponentsByUuid(), isMainBranch ? null : request.getBranch(), request.getPullRequest(), requestedMetrics));
      }
    }

    if (areMetricsInResponse(request)) {
      for (MetricDto metricDto : data.getMetrics()) {
        addMetricToResponseIncludingRenamedMetric(metric -> response.getMetricsBuilder().addMetrics(metric), requestedMetrics, metricDto);
      }
    }

    List<String> additionalFields = ofNullable(request.getAdditionalFields()).orElse(Collections.emptyList());

    if (additionalFields.contains(ADDITIONAL_PERIOD) && data.getPeriod() != null) {
      response.setPeriod(data.getPeriod());
    }

    return response.build();
  }

  private static boolean areMetricsInResponse(ComponentTreeRequest request) {
    List<String> additionalFields = request.getAdditionalFields();
    return additionalFields != null && additionalFields.contains(ADDITIONAL_METRICS);
  }

  private static ComponentTreeWsResponse emptyResponse(@Nullable ComponentDto baseComponent, @Nullable BranchDto branch,
    ComponentTreeRequest request) {
    ComponentTreeWsResponse.Builder response = ComponentTreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setTotal(0);
    if (baseComponent != null) {
      boolean isMainBranch = branch == null || branch.isMain();
      response.setBaseComponent(componentDtoToWsComponent(baseComponent, isMainBranch ? null : request.getBranch(),
        request.getPullRequest()));
    }
    return response.build();
  }

  private static ComponentTreeRequest toComponentTreeWsRequest(Request request) {
    List<String> metricKeys = request.mandatoryParamAsStrings(PARAM_METRIC_KEYS);
    checkArgument(metricKeys.size() <= MAX_METRIC_KEYS, "Number of metrics keys is limited to %s, got %s", MAX_METRIC_KEYS,
      metricKeys.size());
    ComponentTreeRequest componentTreeRequest = new ComponentTreeRequest()
      .setComponent(request.mandatoryParam(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST))
      .setMetricKeys(metricKeys)
      .setStrategy(request.mandatoryParam(PARAM_STRATEGY))
      .setQualifiers(request.paramAsStrings(PARAM_QUALIFIERS))
      .setAdditionalFields(request.paramAsStrings(PARAM_ADDITIONAL_FIELDS))
      .setSort(request.paramAsStrings(Param.SORT))
      .setAsc(request.paramAsBoolean(Param.ASCENDING))
      .setMetricSort(includeRenamedMetrics(request.param(PARAM_METRIC_SORT)))
      .setMetricSortFilter(request.mandatoryParam(PARAM_METRIC_SORT_FILTER))
      .setMetricPeriodSort(request.paramAsInt(PARAM_METRIC_PERIOD_SORT))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setQuery(request.param(Param.TEXT_QUERY));
    String metricSortValue = componentTreeRequest.getMetricSort();
    checkRequest(!componentTreeRequest.getMetricKeys().isEmpty(), "The '%s' parameter must contain at least one metric key",
      PARAM_METRIC_KEYS);
    List<String> sorts = ofNullable(componentTreeRequest.getSort()).orElse(emptyList());
    checkRequest(metricSortValue == null ^ sorts.contains(METRIC_SORT) ^ sorts.contains(METRIC_PERIOD_SORT),
      "To sort by a metric, the '%s' parameter must contain '%s' or '%s', and a metric key must be provided in the '%s' parameter",
      Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT, PARAM_METRIC_SORT);
    checkRequest(metricSortValue == null ^ componentTreeRequest.getMetricKeys().contains(metricSortValue),
      "To sort by the '%s' metric, it must be in the list of metric keys in the '%s' parameter", metricSortValue, PARAM_METRIC_KEYS);
    checkRequest(componentTreeRequest.getMetricPeriodSort() == null ^ sorts.contains(METRIC_PERIOD_SORT),
      "To sort by a metric period, the '%s' parameter must contain '%s' and the '%s' must be provided.", Param.SORT, METRIC_PERIOD_SORT,
      PARAM_METRIC_PERIOD_SORT);
    checkRequest(ALL_METRIC_SORT_FILTER.equals(componentTreeRequest.getMetricSortFilter()) || metricSortValue != null,
      "To filter components based on the sort metric, the '%s' parameter must contain '%s' or '%s' and the '%s' parameter must be provided",
      Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT, PARAM_METRIC_SORT);
    return componentTreeRequest;
  }

  private static Measures.Component.Builder toWsComponent(ComponentDto component, Map<MetricDto, ComponentTreeData.Measure> measures,
    Map<String, ComponentDto> referenceComponentsByUuid, @Nullable String branch, @Nullable String pullRequest,
    List<String> requestedMetrics) {
    Measures.Component.Builder wsComponent = componentDtoToWsComponent(component, branch, pullRequest);
    ComponentDto referenceComponent = referenceComponentsByUuid.get(component.getCopyComponentUuid());
    if (referenceComponent != null) {
      wsComponent.setRefKey(referenceComponent.getKey());
      String displayQualifier = getDisplayQualifier(component, referenceComponent);
      wsComponent.setQualifier(displayQualifier);
    }
    Measures.Measure.Builder measureBuilder = Measures.Measure.newBuilder();
    for (Map.Entry<MetricDto, ComponentTreeData.Measure> entry : measures.entrySet()) {
      ComponentTreeData.Measure measure = entry.getValue();
      boolean onNewCode = entry.getKey().getKey().startsWith("new_");
      updateMeasureBuilder(measureBuilder, entry.getKey(), measure.getValue(), measure.getData(), onNewCode);
      addMeasureIncludingRenamedMetric(requestedMetrics, wsComponent, measureBuilder);
      measureBuilder.clear();
    }
    return wsComponent;
  }

  // https://jira.sonarsource.com/browse/SONAR-13703 - for apps that were added as a local reference to a portfolio, we want to
  // show them as apps, not sub-portfolios
  private static String getDisplayQualifier(ComponentDto component, ComponentDto referenceComponent) {
    String qualifier = component.qualifier();
    if (qualifier.equals(ComponentQualifiers.SUBVIEW) && referenceComponent.qualifier().equals(ComponentQualifiers.APP)) {
      return ComponentQualifiers.APP;
    }
    return qualifier;
  }

  private ComponentTreeData load(ComponentTreeRequest wsRequest) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto baseComponent = loadComponent(dbSession, wsRequest);
      checkPermissions(baseComponent);
      // portfolios don't have branches
      BranchDto branchDto = dbClient.branchDao().selectByUuid(dbSession, baseComponent.branchUuid()).orElse(null);

      Optional<SnapshotDto> baseSnapshot = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession,
        baseComponent.branchUuid());
      if (baseSnapshot.isEmpty()) {
        return ComponentTreeData.builder()
          .setBranch(branchDto)
          .setBaseComponent(baseComponent)
          .build();
      }

      ComponentTreeQuery componentTreeQuery = toComponentTreeQuery(wsRequest, baseComponent);
      List<ComponentDto> components = searchComponents(dbSession, componentTreeQuery);

      List<MetricDto> metrics = searchMetrics(dbSession,
        new HashSet<>(withRemovedMetricAlias(ofNullable(wsRequest.getMetricKeys()).orElse(List.of()))));
      Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric = searchMeasuresByComponentUuidAndMetric(dbSession, baseComponent, componentTreeQuery,
        components, metrics);

      components = filterComponents(components, measuresByComponentUuidAndMetric, metrics, wsRequest);
      components = filterAuthorizedComponents(components);
      components = sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);

      int componentCount = components.size();
      components = paginateComponents(components, wsRequest);

      Map<String, ComponentDto> referencesByUuid = searchReferenceComponentsById(dbSession, components);
      Map<String, String> branchByReferenceUuid = searchReferenceBranchKeys(dbSession, referencesByUuid.keySet());

      return ComponentTreeData.builder()
        .setBaseComponent(baseComponent)
        .setBranch(branchDto)
        .setComponentsFromDb(components)
        .setComponentCount(componentCount)
        .setBranchByReferenceUuid(branchByReferenceUuid)
        .setMeasuresByComponentUuidAndMetric(measuresByComponentUuidAndMetric)
        .setMetrics(metrics)
        .setPeriod(snapshotToWsPeriods(baseSnapshot.get()).orElse(null))
        .setReferenceComponentsByUuid(referencesByUuid)
        .build();
    }
  }

  private Map<String, String> searchReferenceBranchKeys(DbSession dbSession, Set<String> referenceUuids) {
    return dbClient.branchDao().selectByUuids(dbSession, referenceUuids).stream()
      .filter(b -> !b.isMain())
      .collect(Collectors.toMap(BranchDto::getUuid, BranchDto::getBranchKey));
  }

  private ComponentDto loadComponent(DbSession dbSession, ComponentTreeRequest request) {
    String componentKey = request.getComponent();
    String branch = request.getBranch();
    String pullRequest = request.getPullRequest();
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  private Map<String, ComponentDto> searchReferenceComponentsById(DbSession dbSession, List<ComponentDto> components) {
    List<String> referenceComponentUUids = components.stream()
      .map(ComponentDto::getCopyComponentUuid)
      .filter(Objects::nonNull)
      .toList();
    if (referenceComponentUUids.isEmpty()) {
      return emptyMap();
    }

    return FluentIterable.from(dbClient.componentDao().selectByUuids(dbSession, referenceComponentUUids))
      .uniqueIndex(ComponentDto::uuid);
  }

  private List<ComponentDto> searchComponents(DbSession dbSession, ComponentTreeQuery componentTreeQuery) {
    Collection<String> qualifiers = componentTreeQuery.getQualifiers();
    if (qualifiers != null && qualifiers.isEmpty()) {
      return Collections.emptyList();
    }
    return dbClient.componentDao().selectDescendants(dbSession, componentTreeQuery);
  }

  private List<MetricDto> searchMetrics(DbSession dbSession, Set<String> metricKeys) {
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    if (metrics.size() < metricKeys.stream().filter(key -> !key.equals("contains_ai_code")).count()) {
      List<String> foundMetricKeys = Lists.transform(metrics, MetricDto::getKey);
      Set<String> missingMetricKeys = Sets.difference(
        new LinkedHashSet<>(metricKeys),
        new LinkedHashSet<>(foundMetricKeys));

      throw new NotFoundException(format("The following metric keys are not found: %s", join(COMMA_JOIN_SEPARATOR,
        missingMetricKeys)));
    }
    String forbiddenMetrics = metrics.stream()
      .filter(UnsupportedMetrics.INSTANCE)
      .map(MetricDto::getKey)
      .sorted()
      .collect(Collectors.joining(COMMA_JOIN_SEPARATOR));
    checkArgument(forbiddenMetrics.isEmpty(), "Metrics %s can't be requested in this web service. Please use api/measures/component",
      forbiddenMetrics);
    return metrics;
  }

  private Table<String, MetricDto, ComponentTreeData.Measure> searchMeasuresByComponentUuidAndMetric(DbSession dbSession,
    ComponentDto baseComponent,
    ComponentTreeQuery componentTreeQuery, List<ComponentDto> components, List<MetricDto> metrics) {

    Map<String, MetricDto> metricsByKeys = Maps.uniqueIndex(metrics, MetricDto::getKey);
    MeasureTreeQuery measureQuery = MeasureTreeQuery.builder()
      .setStrategy(MeasureTreeQuery.Strategy.valueOf(componentTreeQuery.getStrategy().name()))
      .setNameOrKeyQuery(componentTreeQuery.getNameOrKeyQuery())
      .setQualifiers(componentTreeQuery.getQualifiers())
      .build();

    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric = HashBasedTable.create(components.size(),
      metrics.size());
    dbClient.measureDao().selectTreeByQuery(dbSession, baseComponent, measureQuery, result -> {
      MeasureDto measureDto = result.getResultObject();
      measureDto.getMetricValues().forEach((metricKey, value) -> {
        MetricDto metric = metricsByKeys.get(metricKey);
        if (metric != null) {
          measuresByComponentUuidAndMetric.put(
            measureDto.getComponentUuid(),
            metric,
            ComponentTreeData.Measure.createFromMetricValue(metric, value));
        }
      });
    });

    Set<MetricDto> baseComponentMetricDtos = measuresByComponentUuidAndMetric.row(baseComponent.uuid()).keySet();

    addBestValuesToMeasures(measuresByComponentUuidAndMetric, components, baseComponentMetricDtos, metrics);

    return measuresByComponentUuidAndMetric;
  }

  /**
   * Conditions for best value measure:
   * <ul>
   * <li>component is a production file or test file</li>
   * <li>metric is optimized for best value</li>
   * </ul>
   */
  private static void addBestValuesToMeasures(Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric,
    List<ComponentDto> components,
    Set<MetricDto> baseComponentMetricDtos,
    List<MetricDto> metrics) {
    List<MetricDtoWithBestValue> metricDtosWithBestValueMeasure = metrics.stream()
      .filter(baseComponentMetricDtos::contains)
      .filter(MetricDtoFunctions.isOptimizedForBestValue())
      .map(new MetricDtoToMetricDtoWithBestValue())
      .toList();
    if (metricDtosWithBestValueMeasure.isEmpty()) {
      return;
    }

    Stream<ComponentDto> componentsEligibleForBestValue = components.stream().filter(ComponentTreeAction::isFileComponent);
    componentsEligibleForBestValue.forEach(component -> {
      for (MetricDtoWithBestValue metricWithBestValue : metricDtosWithBestValueMeasure) {
        if (measuresByComponentUuidAndMetric.get(component.uuid(), metricWithBestValue.getMetric()) == null) {
          measuresByComponentUuidAndMetric.put(component.uuid(), metricWithBestValue.getMetric(),
            ComponentTreeData.Measure.createFromMetricValue(metricWithBestValue.getMetric(), metricWithBestValue.getBestValue()));
        }
      }
    });
  }

  private static List<ComponentDto> filterComponents(List<ComponentDto> components,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric, List<MetricDto> metrics,
    ComponentTreeRequest wsRequest) {
    if (!componentWithMeasuresOnly(wsRequest)) {
      return components;
    }

    String metricKeyToSort = wsRequest.getMetricSort();
    Optional<MetricDto> metricToSort = metrics.stream().filter(m -> metricKeyToSort.equals(m.getKey())).findFirst();
    checkState(metricToSort.isPresent(), "Metric '%s' not found", metricKeyToSort, wsRequest.getMetricKeys());

    return components
      .stream()
      .filter(new HasMeasure(measuresByComponentUuidAndMetric, metricToSort.get()))
      .toList();
  }

  private List<ComponentDto> filterAuthorizedComponents(List<ComponentDto> components) {
    return userSession.keepAuthorizedComponents(ProjectPermission.USER, components);
  }

  private static boolean componentWithMeasuresOnly(ComponentTreeRequest wsRequest) {
    return WITH_MEASURES_ONLY_METRIC_SORT_FILTER.equals(wsRequest.getMetricSortFilter());
  }

  private static List<ComponentDto> sortComponents(List<ComponentDto> components, ComponentTreeRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    return ComponentTreeSort.sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);
  }

  private static List<ComponentDto> paginateComponents(List<ComponentDto> components, ComponentTreeRequest wsRequest) {
    return components.stream()
      .skip(offset(wsRequest.getPage(), wsRequest.getPageSize()))
      .limit(wsRequest.getPageSize())
      .toList();
  }

  @CheckForNull
  private List<String> childrenQualifiers(ComponentTreeRequest request, String baseQualifier) {
    List<String> requestQualifiers = request.getQualifiers();
    List<String> childrenQualifiers = null;
    if (LEAVES_STRATEGY.equals(request.getStrategy())) {
      childrenQualifiers = componentTypes.getLeavesQualifiers(baseQualifier);
    }

    if (requestQualifiers == null) {
      return childrenQualifiers;
    }

    if (childrenQualifiers == null) {
      return requestQualifiers;
    }

    Sets.SetView<String> qualifiersIntersection = Sets.intersection(new HashSet<>(childrenQualifiers),
      new HashSet<Object>(requestQualifiers));

    return new ArrayList<>(qualifiersIntersection);
  }

  private ComponentTreeQuery toComponentTreeQuery(ComponentTreeRequest wsRequest, ComponentDto baseComponent) {
    List<String> childrenQualifiers = childrenQualifiers(wsRequest, baseComponent.qualifier());

    ComponentTreeQuery.Builder componentTreeQueryBuilder = ComponentTreeQuery.builder()
      .setBaseUuid(baseComponent.uuid())
      .setStrategy(STRATEGIES.get(wsRequest.getStrategy()));

    if (wsRequest.getQuery() != null) {
      componentTreeQueryBuilder.setNameOrKeyQuery(wsRequest.getQuery());
    }
    if (childrenQualifiers != null) {
      componentTreeQueryBuilder.setQualifiers(childrenQualifiers);
    }
    return componentTreeQueryBuilder.build();
  }

  private void checkPermissions(ComponentDto baseComponent) {
    userSession.checkComponentPermission(ProjectPermission.USER, baseComponent);

    if (ComponentScopes.PROJECT.equals(baseComponent.scope()) && ComponentQualifiers.APP.equals(baseComponent.qualifier())) {
      userSession.checkChildProjectsPermission(ProjectPermission.USER, baseComponent);
    }
  }

  public static boolean isFileComponent(@Nonnull ComponentDto input) {
    return QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE.contains(input.qualifier());
  }

  private static class MetricDtoToMetricDtoWithBestValue implements Function<MetricDto, MetricDtoWithBestValue> {
    @Override
    public MetricDtoWithBestValue apply(@Nonnull MetricDto input) {
      return new MetricDtoWithBestValue(input);
    }
  }

  private enum UnsupportedMetrics implements Predicate<MetricDto> {
    INSTANCE;

    static final Set<String> FORBIDDEN_METRIC_TYPES = Set.of(DISTRIB.name());
    static final Map<String, Set<String>> PARTIALLY_SUPPORTED_METRICS = Map.of(
      DATA.name(),
      DataSupportedMetrics.IMPACTS_SUPPORTED_METRICS);

    @Override
    public boolean test(@Nonnull MetricDto input) {
      if (FORBIDDEN_METRIC_TYPES.contains(input.getValueType())) {
        return true;
      }
      Set<String> partialSupport = PARTIALLY_SUPPORTED_METRICS.get(input.getValueType());
      if (partialSupport == null) {
        return false;
      } else {
        return !partialSupport.contains(input.getKey());
      }
    }
  }

}
