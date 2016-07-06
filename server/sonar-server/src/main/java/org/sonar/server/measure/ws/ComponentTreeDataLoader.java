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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.server.component.ComponentFinder.ParamNames.BASE_COMPONENT_ID_AND_KEY;
import static org.sonar.server.component.ComponentFinder.ParamNames.DEVELOPER_ID_AND_KEY;
import static org.sonar.server.measure.ws.ComponentTreeAction.ALL_STRATEGY;
import static org.sonar.server.measure.ws.ComponentTreeAction.CHILDREN_STRATEGY;
import static org.sonar.server.measure.ws.ComponentTreeAction.LEAVES_STRATEGY;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_PERIOD_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.NAME_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.WITH_MEASURES_ONLY_METRIC_SORT_FILTER;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriods.snapshotToWsPeriods;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class ComponentTreeDataLoader {
  private static final Set<String> QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE = newHashSet(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final ResourceTypes resourceTypes;

  public ComponentTreeDataLoader(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.resourceTypes = resourceTypes;
  }

  ComponentTreeData load(ComponentTreeWsRequest wsRequest) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto baseComponent = componentFinder.getByUuidOrKey(dbSession, wsRequest.getBaseComponentId(), wsRequest.getBaseComponentKey(), BASE_COMPONENT_ID_AND_KEY);
      checkPermissions(baseComponent);
      java.util.Optional<SnapshotDto> baseSnapshot = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, baseComponent.projectUuid());
      if (!baseSnapshot.isPresent()) {
        return ComponentTreeData.builder()
          .setBaseComponent(baseComponent)
          .build();
      }
      Long developerId = searchDeveloperId(dbSession, wsRequest);

      ComponentTreeQuery dbQuery = toComponentTreeQuery(wsRequest, baseComponent);
      ComponentDtosAndTotal componentDtosAndTotal = searchComponents(dbSession, dbQuery, wsRequest);
      List<ComponentDto> components = componentDtosAndTotal.componentDtos;
      List<MetricDto> metrics = searchMetrics(dbSession, wsRequest);
      List<WsMeasures.Period> periods = snapshotToWsPeriods(baseSnapshot.get());
      Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric = searchMeasuresByComponentUuidAndMetric(dbSession, baseComponent, components, metrics,
        periods, developerId);

      components = filterComponents(components, measuresByComponentUuidAndMetric, metrics, wsRequest);
      components = sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);
      int componentCount = computeComponentCount(componentDtosAndTotal.total, components, componentWithMeasuresOnly(wsRequest));
      components = paginateComponents(components, wsRequest);
      Map<String, ComponentDto> referenceComponentsById = searchReferenceComponentsById(dbSession, components);

      return ComponentTreeData.builder()
        .setBaseComponent(baseComponent)
        .setComponentsFromDb(components)
        .setComponentCount(componentCount)
        .setMeasuresByComponentUuidAndMetric(measuresByComponentUuidAndMetric)
        .setMetrics(metrics)
        .setPeriods(periods)
        .setReferenceComponentsByUuid(referenceComponentsById)
        .build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static int computeComponentCount(int dbComponentCount, List<ComponentDto> components, boolean returnOnlyComponentsWithMeasures) {
    return returnOnlyComponentsWithMeasures ? components.size() : dbComponentCount;
  }

  @CheckForNull
  private Long searchDeveloperId(DbSession dbSession, ComponentTreeWsRequest wsRequest) {
    if (wsRequest.getDeveloperId() == null && wsRequest.getDeveloperKey() == null) {
      return null;
    }

    return componentFinder.getByUuidOrKey(dbSession, wsRequest.getDeveloperId(), wsRequest.getDeveloperKey(), DEVELOPER_ID_AND_KEY).getId();
  }

  private Map<String, ComponentDto> searchReferenceComponentsById(DbSession dbSession, List<ComponentDto> components) {
    List<String> referenceComponentUUids = from(components)
      .transform(ComponentDto::getCopyResourceUuid)
      .filter(Predicates.<String>notNull())
      .toList();
    if (referenceComponentUUids.isEmpty()) {
      return emptyMap();
    }

    return FluentIterable.from(dbClient.componentDao().selectByUuids(dbSession, referenceComponentUUids))
      .uniqueIndex(ComponentDto::uuid);
  }

  private ComponentDtosAndTotal searchComponents(DbSession dbSession, ComponentTreeQuery dbQuery, ComponentTreeWsRequest wsRequest) {
    if (dbQuery.getQualifiers() != null && dbQuery.getQualifiers().isEmpty()) {
      return new ComponentDtosAndTotal(Collections.emptyList(), 0);
    }
    String strategy = requireNonNull(wsRequest.getStrategy());
    switch (strategy) {
      case CHILDREN_STRATEGY:
        return new ComponentDtosAndTotal(
          dbClient.componentDao().selectChildren(dbSession, dbQuery),
          dbClient.componentDao().countChildren(dbSession, dbQuery));
      case LEAVES_STRATEGY:
      case ALL_STRATEGY:
        return new ComponentDtosAndTotal(
          dbClient.componentDao().selectDescendants(dbSession, dbQuery),
          dbClient.componentDao().countDescendants(dbSession, dbQuery));
      default:
        throw new IllegalStateException("Unknown component tree strategy");
    }
  }

  private List<MetricDto> searchMetrics(DbSession dbSession, ComponentTreeWsRequest request) {
    List<String> metricKeys = requireNonNull(request.getMetricKeys());
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    if (metrics.size() < metricKeys.size()) {
      List<String> foundMetricKeys = Lists.transform(metrics, MetricDtoFunctions.toKey());
      Set<String> missingMetricKeys = Sets.difference(
        new LinkedHashSet<>(metricKeys),
        new LinkedHashSet<>(foundMetricKeys));

      throw new NotFoundException(format("The following metric keys are not found: %s", Joiner.on(", ").join(missingMetricKeys)));
    }

    return metrics;
  }

  private Table<String, MetricDto, MeasureDto> searchMeasuresByComponentUuidAndMetric(DbSession dbSession, ComponentDto baseComponent,
    List<ComponentDto> components, List<MetricDto> metrics,
    List<WsMeasures.Period> periods, @Nullable Long developerId) {
    List<String> componentUuids = new ArrayList<>();
    componentUuids.add(baseComponent.uuid());
    components.stream().forEach(c -> componentUuids.add(c.uuid()));

    Map<Integer, MetricDto> metricsById = Maps.uniqueIndex(metrics, MetricDtoFunctions.toId());
    MeasureQuery measureQuery = MeasureQuery.builder()
      .setPersonId(developerId)
      .setComponentUuids(componentUuids)
      .setMetricIds(metricsById.keySet())
      .build();
    List<MeasureDto> measureDtos = dbClient.measureDao().selectByQuery(dbSession, measureQuery);

    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric = HashBasedTable.create(components.size(), metrics.size());
    for (MeasureDto measureDto : measureDtos) {
      measuresByComponentUuidAndMetric.put(
        measureDto.getComponentUuid(),
        metricsById.get(measureDto.getMetricId()),
        measureDto);
    }

    addBestValuesToMeasures(measuresByComponentUuidAndMetric, components, metrics, periods);

    return measuresByComponentUuidAndMetric;
  }

  /**
   * Conditions for best value measure:
   * <ul>
   * <li>component is a production file or test file</li>
   * <li>metric is optimized for best value</li>
   * </ul>
   */
  private static void addBestValuesToMeasures(Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric, List<ComponentDto> components,
    List<MetricDto> metrics, List<WsMeasures.Period> periods) {
    List<MetricDtoWithBestValue> metricDtosWithBestValueMeasure = from(metrics)
      .filter(MetricDtoFunctions.isOptimizedForBestValue())
      .transform(new MetricDtoToMetricDtoWithBestValue(periods))
      .toList();
    if (metricDtosWithBestValueMeasure.isEmpty()) {
      return;
    }

    List<ComponentDto> componentsEligibleForBestValue = from(components).filter(IsFileComponent.INSTANCE).toList();
    for (ComponentDto component : componentsEligibleForBestValue) {
      for (MetricDtoWithBestValue metricWithBestValue : metricDtosWithBestValueMeasure) {
        if (measuresByComponentUuidAndMetric.get(component.uuid(), metricWithBestValue.getMetric()) == null) {
          measuresByComponentUuidAndMetric.put(component.uuid(), metricWithBestValue.getMetric(), metricWithBestValue.getBestValue());
        }
      }
    }
  }

  private static List<ComponentDto> filterComponents(List<ComponentDto> components,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric, List<MetricDto> metrics, ComponentTreeWsRequest wsRequest) {
    if (!componentWithMeasuresOnly(wsRequest)) {
      return components;
    }

    final String metricKeyToSort = wsRequest.getMetricSort();
    Optional<MetricDto> metricToSort = from(metrics).firstMatch(new MatchMetricKey(metricKeyToSort));
    checkState(metricToSort.isPresent(), "Metric '%s' not found", metricKeyToSort, wsRequest.getMetricKeys());

    return components
      .stream()
      .filter(new HasMeasure(measuresByComponentUuidAndMetric, metricToSort.get(), wsRequest))
      .collect(Collectors.toList());
  }

  private static boolean componentWithMeasuresOnly(ComponentTreeWsRequest wsRequest) {
    return WITH_MEASURES_ONLY_METRIC_SORT_FILTER.equals(wsRequest.getMetricSortFilter());
  }

  private static List<ComponentDto> sortComponents(List<ComponentDto> components, ComponentTreeWsRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    if (!isSortByMetric(wsRequest)) {
      return components;
    }

    return ComponentTreeSort.sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);
  }

  private static List<ComponentDto> paginateComponents(List<ComponentDto> components, ComponentTreeWsRequest wsRequest) {
    if (!isSortByMetric(wsRequest)) {
      return components;
    }

    return from(components)
      .skip(offset(wsRequest.getPage(), wsRequest.getPageSize()))
      .limit(wsRequest.getPageSize())
      .toList();
  }

  private static boolean isSortByMetric(ComponentTreeWsRequest wsRequest) {
    requireNonNull(wsRequest.getSort());
    return wsRequest.getSort().contains(METRIC_SORT) || wsRequest.getSort().contains(METRIC_PERIOD_SORT);
  }

  @CheckForNull
  private List<String> childrenQualifiers(ComponentTreeWsRequest request, String baseQualifier) {
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

  private ComponentTreeQuery toComponentTreeQuery(ComponentTreeWsRequest wsRequest, ComponentDto baseComponent) {
    List<String> childrenQualifiers = childrenQualifiers(wsRequest, baseComponent.qualifier());

    List<String> sortsWithoutMetricSort = newArrayList(Iterables.filter(wsRequest.getSort(), IsNotMetricSort.INSTANCE));
    sortsWithoutMetricSort = sortsWithoutMetricSort.isEmpty() ? singletonList(NAME_SORT) : sortsWithoutMetricSort;

    ComponentTreeQuery.Builder dbQuery = ComponentTreeQuery.builder()
      .setBaseUuid(baseComponent.uuid())
      .setPage(wsRequest.getPage())
      .setPageSize(wsRequest.getPageSize())
      .setSortFields(sortsWithoutMetricSort)
      .setAsc(wsRequest.getAsc());

    if (wsRequest.getQuery() != null) {
      dbQuery.setNameOrKeyQuery(wsRequest.getQuery());
    }
    if (childrenQualifiers != null) {
      dbQuery.setQualifiers(childrenQualifiers);
    }
    // load all components if we must sort by metric value
    if (isSortByMetric(wsRequest)) {
      dbQuery.setPage(1);
      dbQuery.setPageSize(Integer.MAX_VALUE);
    }

    return dbQuery.build();
  }

  private void checkPermissions(ComponentDto baseComponent) {
    String projectUuid = firstNonNull(baseComponent.projectUuid(), baseComponent.uuid());
    if (!userSession.hasComponentUuidPermission(UserRole.ADMIN, projectUuid) &&
      !userSession.hasComponentUuidPermission(UserRole.USER, projectUuid)) {
      throw insufficientPrivilegesException();
    }
  }

  private static class ComponentDtosAndTotal {
    private final List<ComponentDto> componentDtos;
    private final int total;

    private ComponentDtosAndTotal(List<ComponentDto> componentDtos, int total) {
      this.componentDtos = componentDtos;
      this.total = total;
    }
  }

  private enum IsFileComponent implements Predicate<ComponentDto> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull ComponentDto input) {
      return QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE.contains(input.qualifier());
    }
  }

  private static class MetricDtoToMetricDtoWithBestValue implements Function<MetricDto, MetricDtoWithBestValue> {
    private final List<Integer> periodIndexes;

    MetricDtoToMetricDtoWithBestValue(List<WsMeasures.Period> periods) {
      this.periodIndexes = Lists.transform(periods, WsPeriodToIndex.INSTANCE);
    }

    @Override
    public MetricDtoWithBestValue apply(@Nonnull MetricDto input) {
      return new MetricDtoWithBestValue(input, periodIndexes);
    }
  }

  private enum WsPeriodToIndex implements Function<WsMeasures.Period, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull WsMeasures.Period input) {
      return input.getIndex();
    }
  }

  private enum IsNotMetricSort implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull String input) {
      return !input.equals(METRIC_SORT) && !input.equals(METRIC_PERIOD_SORT);
    }
  }

  private static class MatchMetricKey implements Predicate<MetricDto> {
    private final String metricKeyToSort;

    private MatchMetricKey(String metricKeyToSort) {
      this.metricKeyToSort = metricKeyToSort;
    }

    @Override
    public boolean apply(@Nonnull MetricDto input) {
      return input.getKey().equals(metricKeyToSort);
    }
  }
}
