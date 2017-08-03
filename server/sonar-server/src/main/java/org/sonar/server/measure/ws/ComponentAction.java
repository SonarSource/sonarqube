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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.ws.MetricDtoWithBestValue.MetricDtoToMetricDtoWithBestValueFunction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.ComponentWsResponse;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY;
import static org.sonar.server.component.ComponentFinder.ParamNames.DEVELOPER_ID_AND_KEY;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createAdditionalFieldsParameter;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createDeveloperParameters;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.measure.ws.MetricDtoToWsMetric.metricDtoToWsMetric;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriods.snapshotToWsPeriods;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ACTION_COMPONENT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ADDITIONAL_METRICS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ADDITIONAL_PERIODS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.DEPRECATED_PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.DEPRECATED_PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;

public class ComponentAction implements MeasuresWsAction {
  private static final Set<String> QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE = ImmutableSortedSet.of(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public ComponentAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_COMPONENT)
      .setDescription(format("Return component with specified measures. The %s or the %s parameter must be provided.<br>" +
        "Requires the following permission: 'Browse' on the project of specified component.",
        DEPRECATED_PARAM_COMPONENT_ID, PARAM_COMPONENT))
      .setResponseExample(getClass().getResource("component-example.json"))
      .setSince("5.4")
      .setChangelog(
        new Change("6.6", "the response field id is deprecated. Use key instead."),
        new Change("6.6", "the response field refId is deprecated. Use refKey instead."))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setDeprecatedKey(DEPRECATED_PARAM_COMPONENT_KEY, "6.6");

    action.createParam(DEPRECATED_PARAM_COMPONENT_ID)
      .setDescription("Component id")
      .setExampleValue(UUID_EXAMPLE_01)
      .setDeprecatedSince("6.6");

    createMetricKeysParameter(action);
    createAdditionalFieldsParameter(action);
    createDeveloperParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ComponentWsResponse componentWsResponse = doHandle(toComponentWsRequest(request));
    writeProtobuf(componentWsResponse, request, response);
  }

  private ComponentWsResponse doHandle(ComponentWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByUuidOrKey(dbSession, request.getComponentId(), request.getComponent(), COMPONENT_ID_AND_KEY);
      Long developerId = searchDeveloperId(dbSession, request);
      Optional<ComponentDto> refComponent = getReferenceComponent(dbSession, component);
      checkPermissions(component);
      SnapshotDto analysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, component.projectUuid()).orElse(null);
      List<MetricDto> metrics = searchMetrics(dbSession, request);
      List<WsMeasures.Period> periods = snapshotToWsPeriods(analysis);
      List<MeasureDto> measures = searchMeasures(dbSession, component, analysis, metrics, developerId);

      return buildResponse(request, component, refComponent, measures, metrics, periods);
    }
  }

  @CheckForNull
  private Long searchDeveloperId(DbSession dbSession, ComponentWsRequest request) {
    if (request.getDeveloperId() == null && request.getDeveloperKey() == null) {
      return null;
    }

    return componentFinder.getByUuidOrKey(dbSession, request.getDeveloperId(), request.getDeveloperKey(), DEVELOPER_ID_AND_KEY).getId();
  }

  private Optional<ComponentDto> getReferenceComponent(DbSession dbSession, ComponentDto component) {
    if (component.getCopyResourceUuid() == null) {
      return Optional.absent();
    }

    return dbClient.componentDao().selectByUuid(dbSession, component.getCopyResourceUuid());
  }

  private static ComponentWsResponse buildResponse(ComponentWsRequest request, ComponentDto component, Optional<ComponentDto> refComponent, List<MeasureDto> measures,
    List<MetricDto> metrics, List<WsMeasures.Period> periods) {
    ComponentWsResponse.Builder response = ComponentWsResponse.newBuilder();
    Map<Integer, MetricDto> metricsById = Maps.uniqueIndex(metrics, MetricDto::getId);
    Map<MetricDto, MeasureDto> measuresByMetric = new HashMap<>();
    for (MeasureDto measure : measures) {
      MetricDto metric = metricsById.get(measure.getMetricId());
      measuresByMetric.put(metric, measure);
    }
    if (refComponent.isPresent()) {
      response.setComponent(componentDtoToWsComponent(component, measuresByMetric, singletonMap(refComponent.get().uuid(), refComponent.get())));
    } else {
      response.setComponent(componentDtoToWsComponent(component, measuresByMetric, emptyMap()));
    }

    List<String> additionalFields = request.getAdditionalFields();
    if (additionalFields != null) {
      if (additionalFields.contains(ADDITIONAL_METRICS)) {
        for (MetricDto metric : metrics) {
          response.getMetricsBuilder().addMetrics(metricDtoToWsMetric(metric));
        }
      }
      if (additionalFields.contains(ADDITIONAL_PERIODS)) {
        response.getPeriodsBuilder().addAllPeriods(periods);
      }
    }

    return response.build();
  }

  private List<MetricDto> searchMetrics(DbSession dbSession, ComponentWsRequest request) {
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, request.getMetricKeys());
    if (metrics.size() < request.getMetricKeys().size()) {
      List<String> foundMetricKeys = Lists.transform(metrics, MetricDto::getKey);
      Set<String> missingMetricKeys = Sets.difference(
        new LinkedHashSet<>(request.getMetricKeys()),
        new LinkedHashSet<>(foundMetricKeys));

      throw new NotFoundException(format("The following metric keys are not found: %s", Joiner.on(", ").join(missingMetricKeys)));
    }

    return metrics;
  }

  private List<MeasureDto> searchMeasures(DbSession dbSession, ComponentDto component, @Nullable SnapshotDto analysis, List<MetricDto> metrics, @Nullable Long developerId) {
    if (analysis == null) {
      return emptyList();
    }

    List<Integer> metricIds = Lists.transform(metrics, MetricDto::getId);
    MeasureQuery query = MeasureQuery.builder()
      .setPersonId(developerId)
      .setMetricIds(metricIds)
      .setComponentUuid(component.uuid())
      .build();
    List<MeasureDto> measures = dbClient.measureDao().selectByQuery(dbSession, query);
    addBestValuesToMeasures(measures, component, metrics);

    return measures;
  }

  /**
   * Conditions for best value measure:
   * <ul>
   * <li>component is a production file or test file</li>
   * <li>metric is optimized for best value</li>
   * </ul>
   */
  private static void addBestValuesToMeasures(List<MeasureDto> measures, ComponentDto component, List<MetricDto> metrics) {
    if (!QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE.contains(component.qualifier())) {
      return;
    }

    List<MetricDtoWithBestValue> metricWithBestValueList = metrics.stream()
      .filter(MetricDtoFunctions.isOptimizedForBestValue())
      .map(new MetricDtoToMetricDtoWithBestValueFunction())
      .collect(MoreCollectors.toList(metrics.size()));
    Map<Integer, MeasureDto> measuresByMetricId = Maps.uniqueIndex(measures, MeasureDto::getMetricId);

    for (MetricDtoWithBestValue metricWithBestValue : metricWithBestValueList) {
      if (measuresByMetricId.get(metricWithBestValue.getMetric().getId()) == null) {
        measures.add(metricWithBestValue.getBestValue());
      }
    }
  }

  private static ComponentWsRequest toComponentWsRequest(Request request) {
    ComponentWsRequest componentWsRequest = new ComponentWsRequest()
      .setComponentId(request.param(DEPRECATED_PARAM_COMPONENT_ID))
      .setComponent(request.param(PARAM_COMPONENT))
      .setAdditionalFields(request.paramAsStrings(PARAM_ADDITIONAL_FIELDS))
      .setMetricKeys(request.mandatoryParamAsStrings(PARAM_METRIC_KEYS))
      .setDeveloperId(request.param(PARAM_DEVELOPER_ID))
      .setDeveloperKey(request.param(PARAM_DEVELOPER_KEY));
    checkRequest(!componentWsRequest.getMetricKeys().isEmpty(), "At least one metric key must be provided");
    return componentWsRequest;
  }

  private void checkPermissions(ComponentDto baseComponent) {
    userSession.checkComponentPermission(UserRole.USER, baseComponent);
  }
}
