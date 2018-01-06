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
package org.sonar.server.measure.ws;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.ComponentTreeQuery.Strategy;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.measure.MeasureTreeQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.ComponentTreeWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;
import static org.sonar.server.component.ComponentFinder.ParamNames.BASE_COMPONENT_ID_AND_KEY;
import static org.sonar.server.component.ComponentFinder.ParamNames.DEVELOPER_ID_AND_KEY;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.measure.ws.MeasureDtoToWsMeasure.updateMeasureBuilder;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createAdditionalFieldsParameter;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createDeveloperParameters;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.measure.ws.MetricDtoToWsMetric.metricDtoToWsMetric;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriods.snapshotToWsPeriods;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonar.server.component.ws.MeasuresWsParameters.ACTION_COMPONENT_TREE;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_METRICS;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_PERIODS;
import static org.sonar.server.component.ws.MeasuresWsParameters.DEPRECATED_PARAM_BASE_COMPONENT_ID;
import static org.sonar.server.component.ws.MeasuresWsParameters.DEPRECATED_PARAM_BASE_COMPONENT_KEY;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_DEVELOPER_KEY;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_PERIOD_SORT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_SORT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_SORT_FILTER;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_QUALIFIERS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_STRATEGY;

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
  static final Set<String> FORBIDDEN_METRIC_TYPES = ImmutableSet.of(DISTRIB.name(), DATA.name());
  private static final int MAX_METRIC_KEYS = 15;
  private static final Joiner COMMA_JOINER = Joiner.on(", ");
  private static final Set<String> QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE = ImmutableSet.of(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final I18n i18n;
  private final ResourceTypes resourceTypes;

  public ComponentTreeAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, I18n i18n,
    ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.i18n = i18n;
    this.resourceTypes = resourceTypes;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_COMPONENT_TREE)
      .setDescription(format("Navigate through components based on the chosen strategy with specified measures. The %s or the %s parameter must be provided.<br>" +
        "Requires the following permission: 'Browse' on the specified project.<br>" +
        "When limiting search with the %s parameter, directories are not returned.",
        DEPRECATED_PARAM_BASE_COMPONENT_ID, PARAM_COMPONENT, Param.TEXT_QUERY))
      .setResponseExample(getClass().getResource("component_tree-example.json"))
      .setSince("5.4")
      .setHandler(this)
      .addPagingParams(100, MAX_SIZE)
      .setChangelog(
        new Change("6.3", format("Number of metric keys is limited to %s", MAX_METRIC_KEYS)),
        new Change("6.6", "the response field id is deprecated. Use key instead."),
        new Change("6.6", "the response field refId is deprecated. Use refKey instead."));

    action.createSortParams(SORTS, NAME_SORT, true)
      .setDescription("Comma-separated list of sort fields")
      .setExampleValue(NAME_SORT + "," + PATH_SORT);

    action.createParam(Param.TEXT_QUERY)
      .setDescription(format("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that are exactly the same as the supplied string</li>" +
        "</ul>"))
      .setMinimumLength(QUERY_MINIMUM_LENGTH)
      .setExampleValue("FILE_NAM");

    action.createParam(DEPRECATED_PARAM_BASE_COMPONENT_ID)
      .setDescription("Base component id. The search is based on this component.")
      .setExampleValue(UUID_EXAMPLE_02)
      .setDeprecatedSince("6.6");

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key. The search is based on this component.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setDeprecatedKey(DEPRECATED_PARAM_BASE_COMPONENT_KEY, "6.6");

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true)
      .setSince("6.6");

    action.createParam(PARAM_METRIC_SORT)
      .setDescription(
        format("Metric key to sort by. The '%s' parameter must contain the '%s' or '%s' value. It must be part of the '%s' parameter", Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT,
          PARAM_METRIC_KEYS))
      .setExampleValue("ncloc");

    action.createParam(PARAM_METRIC_PERIOD_SORT)
      .setDescription(format("Sort measures by leak period or not ?. The '%s' parameter must contain the '%s' value.", Param.SORT, METRIC_PERIOD_SORT))
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
      .setDescription("Comma-separated list of metric keys. Types %s are not allowed.", COMMA_JOINER.join(FORBIDDEN_METRIC_TYPES))
      .setMaxValuesAllowed(MAX_METRIC_KEYS);
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

  private ComponentTreeWsResponse doHandle(ComponentTreeRequest request) {
    if (request.getDeveloperId() != null || request.getDeveloperKey() != null) {
      return emptyResponse(null, request);
    }

    ComponentTreeData data = load(request);
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

  private static ComponentTreeWsResponse buildResponse(ComponentTreeRequest request, ComponentTreeData data, Paging paging) {
    ComponentTreeWsResponse.Builder response = ComponentTreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    response.setBaseComponent(
      toWsComponent(
        data.getBaseComponent(),
        data.getMeasuresByComponentUuidAndMetric().row(data.getBaseComponent().uuid()),
        data.getReferenceComponentsByUuid()));

    for (ComponentDto componentDto : data.getComponents()) {
      response.addComponents(toWsComponent(
        componentDto,
        data.getMeasuresByComponentUuidAndMetric().row(componentDto.uuid()),
        data.getReferenceComponentsByUuid()));
    }

    if (areMetricsInResponse(request)) {
      Measures.Metrics.Builder metricsBuilder = response.getMetricsBuilder();
      for (MetricDto metricDto : data.getMetrics()) {
        metricsBuilder.addMetrics(metricDtoToWsMetric(metricDto));
      }
    }

    if (arePeriodsInResponse(request)) {
      response.getPeriodsBuilder().addAllPeriods(data.getPeriods());
    }

    return response.build();
  }

  private static boolean areMetricsInResponse(ComponentTreeRequest request) {
    List<String> additionalFields = request.getAdditionalFields();
    return additionalFields != null && additionalFields.contains(ADDITIONAL_METRICS);
  }

  private static boolean arePeriodsInResponse(ComponentTreeRequest request) {
    List<String> additionalFields = request.getAdditionalFields();
    return additionalFields != null && additionalFields.contains(ADDITIONAL_PERIODS);
  }

  private static ComponentTreeWsResponse emptyResponse(@Nullable ComponentDto baseComponent, ComponentTreeRequest request) {
    ComponentTreeWsResponse.Builder response = ComponentTreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setTotal(0);
    if (baseComponent != null) {
      response.setBaseComponent(componentDtoToWsComponent(baseComponent));
    }
    return response.build();
  }

  private static ComponentTreeRequest toComponentTreeWsRequest(Request request) {
    List<String> metricKeys = request.mandatoryParamAsStrings(PARAM_METRIC_KEYS);
    checkArgument(metricKeys.size() <= MAX_METRIC_KEYS, "Number of metrics keys is limited to %s, got %s", MAX_METRIC_KEYS, metricKeys.size());
    ComponentTreeRequest componentTreeRequest = new ComponentTreeRequest()
      .setBaseComponentId(request.param(DEPRECATED_PARAM_BASE_COMPONENT_ID))
      .setComponent(request.param(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setMetricKeys(metricKeys)
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
    String metricSortValue = componentTreeRequest.getMetricSort();
    checkRequest(!componentTreeRequest.getMetricKeys().isEmpty(), "The '%s' parameter must contain at least one metric key", PARAM_METRIC_KEYS);
    List<String> sorts = Optional.ofNullable(componentTreeRequest.getSort()).orElse(emptyList());
    checkRequest(metricSortValue == null ^ sorts.contains(METRIC_SORT) ^ sorts.contains(METRIC_PERIOD_SORT),
      "To sort by a metric, the '%s' parameter must contain '%s' or '%s', and a metric key must be provided in the '%s' parameter",
      Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT, PARAM_METRIC_SORT);
    checkRequest(metricSortValue == null ^ componentTreeRequest.getMetricKeys().contains(metricSortValue),
      "To sort by the '%s' metric, it must be in the list of metric keys in the '%s' parameter", metricSortValue, PARAM_METRIC_KEYS);
    checkRequest(componentTreeRequest.getMetricPeriodSort() == null ^ sorts.contains(METRIC_PERIOD_SORT),
      "To sort by a metric period, the '%s' parameter must contain '%s' and the '%s' must be provided.", Param.SORT, METRIC_PERIOD_SORT, PARAM_METRIC_PERIOD_SORT);
    checkRequest(ALL_METRIC_SORT_FILTER.equals(componentTreeRequest.getMetricSortFilter()) || metricSortValue != null,
      "To filter components based on the sort metric, the '%s' parameter must contain '%s' or '%s' and the '%s' parameter must be provided",
      Param.SORT, METRIC_SORT, METRIC_PERIOD_SORT, PARAM_METRIC_SORT);
    return componentTreeRequest;
  }

  private static Measures.Component.Builder toWsComponent(ComponentDto component, Map<MetricDto, ComponentTreeData.Measure> measures,
                                                          Map<String, ComponentDto> referenceComponentsByUuid) {
    Measures.Component.Builder wsComponent = componentDtoToWsComponent(component);
    ComponentDto referenceComponent = referenceComponentsByUuid.get(component.getCopyResourceUuid());
    if (referenceComponent != null) {
      wsComponent.setRefId(referenceComponent.uuid());
      wsComponent.setRefKey(referenceComponent.getDbKey());
    }
    Measures.Measure.Builder measureBuilder = Measures.Measure.newBuilder();
    for (Map.Entry<MetricDto, ComponentTreeData.Measure> entry : measures.entrySet()) {
      ComponentTreeData.Measure measure = entry.getValue();
      updateMeasureBuilder(measureBuilder, entry.getKey(), measure.getValue(), measure.getData(), measure.getVariation());
      wsComponent.addMeasures(measureBuilder);
      measureBuilder.clear();
    }
    return wsComponent;
  }

  private ComponentTreeData load(ComponentTreeRequest wsRequest) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto baseComponent = loadComponent(dbSession, wsRequest);
      checkPermissions(baseComponent);
      Optional<SnapshotDto> baseSnapshot = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, baseComponent.projectUuid());
      if (!baseSnapshot.isPresent()) {
        return ComponentTreeData.builder()
                .setBaseComponent(baseComponent)
                .build();
      }

      ComponentTreeQuery componentTreeQuery = toComponentTreeQuery(wsRequest, baseComponent);
      List<ComponentDto> components = searchComponents(dbSession, componentTreeQuery);
      List<MetricDto> metrics = searchMetrics(dbSession, wsRequest);
      Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric = searchMeasuresByComponentUuidAndMetric(dbSession, baseComponent, componentTreeQuery,
              components,
              metrics);

      components = filterComponents(components, measuresByComponentUuidAndMetric, metrics, wsRequest);
      components = sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);

      int componentCount = components.size();
      components = paginateComponents(components, wsRequest);

      return ComponentTreeData.builder()
              .setBaseComponent(baseComponent)
              .setComponentsFromDb(components)
              .setComponentCount(componentCount)
              .setMeasuresByComponentUuidAndMetric(measuresByComponentUuidAndMetric)
              .setMetrics(metrics)
              .setPeriods(snapshotToWsPeriods(baseSnapshot.get()))
              .setReferenceComponentsByUuid(searchReferenceComponentsById(dbSession, components))
              .build();
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, ComponentTreeRequest request) {
    String componentKey = request.getComponent();
    String componentId = request.getBaseComponentId();
    String branch = request.getBranch();
    checkArgument(componentId == null || branch == null, "'%s' and '%s' parameters cannot be used at the same time", DEPRECATED_PARAM_BASE_COMPONENT_ID, PARAM_BRANCH);
    return branch == null
            ? componentFinder.getByUuidOrKey(dbSession, componentId, componentKey, BASE_COMPONENT_ID_AND_KEY)
            : componentFinder.getByKeyAndBranch(dbSession, componentKey, branch);
  }

  private Map<String, ComponentDto> searchReferenceComponentsById(DbSession dbSession, List<ComponentDto> components) {
    List<String> referenceComponentUUids = components.stream()
            .map(ComponentDto::getCopyResourceUuid)
            .filter(Objects::nonNull)
            .collect(MoreCollectors.toList(components.size()));
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

  private List<MetricDto> searchMetrics(DbSession dbSession, ComponentTreeRequest request) {
    List<String> metricKeys = requireNonNull(request.getMetricKeys());
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    if (metrics.size() < metricKeys.size()) {
      List<String> foundMetricKeys = Lists.transform(metrics, MetricDto::getKey);
      Set<String> missingMetricKeys = Sets.difference(
              new LinkedHashSet<>(metricKeys),
              new LinkedHashSet<>(foundMetricKeys));

      throw new NotFoundException(format("The following metric keys are not found: %s", COMMA_JOINER.join(missingMetricKeys)));
    }
    String forbiddenMetrics = metrics.stream()
            .filter(metric -> ComponentTreeAction.FORBIDDEN_METRIC_TYPES.contains(metric.getValueType()))
            .map(MetricDto::getKey)
            .sorted()
            .collect(MoreCollectors.join(COMMA_JOINER));
    checkArgument(forbiddenMetrics.isEmpty(), "Metrics %s can't be requested in this web service. Please use api/measures/component", forbiddenMetrics);
    return metrics;
  }

  private Table<String, MetricDto, ComponentTreeData.Measure> searchMeasuresByComponentUuidAndMetric(DbSession dbSession, ComponentDto baseComponent,
    ComponentTreeQuery componentTreeQuery, List<ComponentDto> components, List<MetricDto> metrics) {

    Map<Integer, MetricDto> metricsById = Maps.uniqueIndex(metrics, MetricDto::getId);
    MeasureTreeQuery measureQuery = MeasureTreeQuery.builder()
            .setStrategy(MeasureTreeQuery.Strategy.valueOf(componentTreeQuery.getStrategy().name()))
            .setNameOrKeyQuery(componentTreeQuery.getNameOrKeyQuery())
            .setQualifiers(componentTreeQuery.getQualifiers())
            .setMetricIds(new ArrayList<>(metricsById.keySet()))
            .build();

    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric = HashBasedTable.create(components.size(), metrics.size());
    dbClient.liveMeasureDao().selectTreeByQuery(dbSession, baseComponent, measureQuery, result -> {
      LiveMeasureDto measureDto = result.getResultObject();
      measuresByComponentUuidAndMetric.put(
              measureDto.getComponentUuid(),
              metricsById.get(measureDto.getMetricId()),
              ComponentTreeData.Measure.createFromMeasureDto(measureDto));
    });

    addBestValuesToMeasures(measuresByComponentUuidAndMetric, components, metrics);

    return measuresByComponentUuidAndMetric;
  }

  /**
   * Conditions for best value measure:
   * <ul>
   * <li>component is a production file or test file</li>
   * <li>metric is optimized for best value</li>
   * </ul>
   */
  private static void addBestValuesToMeasures(Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric, List<ComponentDto> components,
                                              List<MetricDto> metrics) {
    List<MetricDtoWithBestValue> metricDtosWithBestValueMeasure = metrics.stream()
            .filter(MetricDtoFunctions.isOptimizedForBestValue())
            .map(new MetricDtoToMetricDtoWithBestValue())
            .collect(MoreCollectors.toList(metrics.size()));
    if (metricDtosWithBestValueMeasure.isEmpty()) {
      return;
    }

    Stream<ComponentDto> componentsEligibleForBestValue = components.stream().filter(ComponentTreeAction::isFileComponent);
    componentsEligibleForBestValue.forEach(component -> {
      for (MetricDtoWithBestValue metricWithBestValue : metricDtosWithBestValueMeasure) {
        if (measuresByComponentUuidAndMetric.get(component.uuid(), metricWithBestValue.getMetric()) == null) {
          measuresByComponentUuidAndMetric.put(component.uuid(), metricWithBestValue.getMetric(),
                  ComponentTreeData.Measure.createFromMeasureDto(metricWithBestValue.getBestValue()));
        }
      }
    });
  }

  private static List<ComponentDto> filterComponents(List<ComponentDto> components,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric, List<MetricDto> metrics, ComponentTreeRequest wsRequest) {
    if (!componentWithMeasuresOnly(wsRequest)) {
      return components;
    }

    String metricKeyToSort = wsRequest.getMetricSort();
    Optional<MetricDto> metricToSort = metrics.stream().filter(m -> metricKeyToSort.equals(m.getKey())).findFirst();
    checkState(metricToSort.isPresent(), "Metric '%s' not found", metricKeyToSort, wsRequest.getMetricKeys());

    return components
            .stream()
            .filter(new HasMeasure(measuresByComponentUuidAndMetric, metricToSort.get(), wsRequest.getMetricPeriodSort()))
            .collect(MoreCollectors.toList(components.size()));
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
            .collect(MoreCollectors.toList(wsRequest.getPageSize()));
  }

  @CheckForNull
  private List<String> childrenQualifiers(ComponentTreeRequest request, String baseQualifier) {
    List<String> requestQualifiers = request.getQualifiers();
    List<String> childrenQualifiers = null;
    if (LEAVES_STRATEGY.equals(request.getStrategy())) {
      childrenQualifiers = resourceTypes.getLeavesQualifiers(baseQualifier);
    }

    if (requestQualifiers == null) {
      return childrenQualifiers;
    }

    if (childrenQualifiers == null) {
      return requestQualifiers;
    }

    Sets.SetView<String> qualifiersIntersection = Sets.intersection(new HashSet<>(childrenQualifiers), new HashSet<Object>(requestQualifiers));

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
    userSession.checkComponentPermission(UserRole.USER, baseComponent);
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
}
