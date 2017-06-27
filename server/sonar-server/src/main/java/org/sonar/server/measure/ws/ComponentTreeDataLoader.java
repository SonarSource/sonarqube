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

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
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
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureTreeQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.ws.ComponentTreeData.Measure;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.server.component.ComponentFinder.ParamNames.BASE_COMPONENT_ID_AND_KEY;
import static org.sonar.server.component.ComponentFinder.ParamNames.DEVELOPER_ID_AND_KEY;
import static org.sonar.server.measure.ws.ComponentTreeAction.LEAVES_STRATEGY;
import static org.sonar.server.measure.ws.ComponentTreeAction.STRATEGIES;
import static org.sonar.server.measure.ws.ComponentTreeAction.WITH_MEASURES_ONLY_METRIC_SORT_FILTER;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriods.snapshotToWsPeriods;

public class ComponentTreeDataLoader {
  private static final Set<String> QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE = ImmutableSet.of(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);
  private static final Joiner COMA_JOINER = Joiner.on(", ");

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
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto baseComponent = componentFinder.getByUuidOrKey(dbSession, wsRequest.getBaseComponentId(), wsRequest.getBaseComponentKey(), BASE_COMPONENT_ID_AND_KEY);
      checkPermissions(baseComponent);
      Optional<SnapshotDto> baseSnapshot = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, baseComponent.projectUuid());
      if (!baseSnapshot.isPresent()) {
        return ComponentTreeData.builder()
          .setBaseComponent(baseComponent)
          .build();
      }
      Long developerId = searchDeveloperId(dbSession, wsRequest);

      ComponentTreeQuery componentTreeQuery = toComponentTreeQuery(wsRequest, baseComponent);
      List<ComponentDto> components = searchComponents(dbSession, componentTreeQuery);
      List<MetricDto> metrics = searchMetrics(dbSession, wsRequest);
      Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric = searchMeasuresByComponentUuidAndMetric(dbSession, baseComponent, componentTreeQuery,
        components,
        metrics, developerId);

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

  @CheckForNull
  private Long searchDeveloperId(DbSession dbSession, ComponentTreeWsRequest wsRequest) {
    if (wsRequest.getDeveloperId() == null && wsRequest.getDeveloperKey() == null) {
      return null;
    }

    return componentFinder.getByUuidOrKey(dbSession, wsRequest.getDeveloperId(), wsRequest.getDeveloperKey(), DEVELOPER_ID_AND_KEY).getId();
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

  private List<MetricDto> searchMetrics(DbSession dbSession, ComponentTreeWsRequest request) {
    List<String> metricKeys = requireNonNull(request.getMetricKeys());
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    if (metrics.size() < metricKeys.size()) {
      List<String> foundMetricKeys = Lists.transform(metrics, MetricDto::getKey);
      Set<String> missingMetricKeys = Sets.difference(
        new LinkedHashSet<>(metricKeys),
        new LinkedHashSet<>(foundMetricKeys));

      throw new NotFoundException(format("The following metric keys are not found: %s", COMA_JOINER.join(missingMetricKeys)));
    }
    String forbiddenMetrics = metrics.stream()
      .filter(metric -> ComponentTreeAction.FORBIDDEN_METRIC_TYPES.contains(metric.getValueType()))
      .map(MetricDto::getKey)
      .sorted()
      .collect(MoreCollectors.join(COMA_JOINER));
    checkArgument(forbiddenMetrics.isEmpty(), "Metrics %s can't be requested in this web service. Please use api/measures/component", forbiddenMetrics);
    return metrics;
  }

  private Table<String, MetricDto, Measure> searchMeasuresByComponentUuidAndMetric(DbSession dbSession, ComponentDto baseComponent,
    ComponentTreeQuery componentTreeQuery,
    List<ComponentDto> components, List<MetricDto> metrics, @Nullable Long developerId) {

    Map<Integer, MetricDto> metricsById = Maps.uniqueIndex(metrics, MetricDto::getId);
    MeasureTreeQuery measureQuery = MeasureTreeQuery.builder()
      .setStrategy(MeasureTreeQuery.Strategy.valueOf(componentTreeQuery.getStrategy().name()))
      .setNameOrKeyQuery(componentTreeQuery.getNameOrKeyQuery())
      .setQualifiers(componentTreeQuery.getQualifiers())
      .setPersonId(developerId)
      .setMetricIds(new ArrayList<>(metricsById.keySet()))
      .build();

    Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric = HashBasedTable.create(components.size(), metrics.size());
    dbClient.measureDao().selectTreeByQuery(dbSession, baseComponent, measureQuery, result -> {
      MeasureDto measureDto = result.getResultObject();
      measuresByComponentUuidAndMetric.put(
        measureDto.getComponentUuid(),
        metricsById.get(measureDto.getMetricId()),
        Measure.createFromMeasureDto(measureDto));
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
  private static void addBestValuesToMeasures(Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric, List<ComponentDto> components,
    List<MetricDto> metrics) {
    List<MetricDtoWithBestValue> metricDtosWithBestValueMeasure = metrics.stream()
      .filter(MetricDtoFunctions.isOptimizedForBestValue())
      .map(new MetricDtoToMetricDtoWithBestValue())
      .collect(MoreCollectors.toList(metrics.size()));
    if (metricDtosWithBestValueMeasure.isEmpty()) {
      return;
    }

    Stream<ComponentDto> componentsEligibleForBestValue = components.stream().filter(IsFileComponent.INSTANCE);
    componentsEligibleForBestValue.forEach(component -> {
      for (MetricDtoWithBestValue metricWithBestValue : metricDtosWithBestValueMeasure) {
        if (measuresByComponentUuidAndMetric.get(component.uuid(), metricWithBestValue.getMetric()) == null) {
          measuresByComponentUuidAndMetric.put(component.uuid(), metricWithBestValue.getMetric(),
            Measure.createFromMeasureDto(metricWithBestValue.getBestValue()));
        }
      }
    });
  }

  private static List<ComponentDto> filterComponents(List<ComponentDto> components,
    Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric, List<MetricDto> metrics, ComponentTreeWsRequest wsRequest) {
    if (!componentWithMeasuresOnly(wsRequest)) {
      return components;
    }

    String metricKeyToSort = wsRequest.getMetricSort();
    Optional<MetricDto> metricToSort = metrics.stream().filter(m -> metricKeyToSort.equals(m.getKey())).findFirst();
    checkState(metricToSort.isPresent(), "Metric '%s' not found", metricKeyToSort, wsRequest.getMetricKeys());

    return components
      .stream()
      .filter(new HasMeasure(measuresByComponentUuidAndMetric, metricToSort.get(), wsRequest))
      .collect(MoreCollectors.toList(components.size()));
  }

  private static boolean componentWithMeasuresOnly(ComponentTreeWsRequest wsRequest) {
    return WITH_MEASURES_ONLY_METRIC_SORT_FILTER.equals(wsRequest.getMetricSortFilter());
  }

  private static List<ComponentDto> sortComponents(List<ComponentDto> components, ComponentTreeWsRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric) {
    return ComponentTreeSort.sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);
  }

  private static List<ComponentDto> paginateComponents(List<ComponentDto> components, ComponentTreeWsRequest wsRequest) {
    return components.stream()
      .skip(offset(wsRequest.getPage(), wsRequest.getPageSize()))
      .limit(wsRequest.getPageSize())
      .collect(MoreCollectors.toList(wsRequest.getPageSize()));
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

  private enum IsFileComponent implements Predicate<ComponentDto> {
    INSTANCE;

    @Override
    public boolean test(@Nonnull ComponentDto input) {
      return QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE.contains(input.qualifier());
    }
  }

  private static class MetricDtoToMetricDtoWithBestValue implements Function<MetricDto, MetricDtoWithBestValue> {
    @Override
    public MetricDtoWithBestValue apply(@Nonnull MetricDto input) {
      return new MetricDtoWithBestValue(input);
    }
  }
}
