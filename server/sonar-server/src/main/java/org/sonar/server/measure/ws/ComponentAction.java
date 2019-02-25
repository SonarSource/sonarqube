/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.ComponentWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY;
import static org.sonar.server.component.ws.MeasuresWsParameters.ACTION_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_METRICS;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_PERIODS;
import static org.sonar.server.component.ws.MeasuresWsParameters.DEPRECATED_PARAM_COMPONENT_ID;
import static org.sonar.server.component.ws.MeasuresWsParameters.DEPRECATED_PARAM_COMPONENT_KEY;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_DEVELOPER_KEY;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createAdditionalFieldsParameter;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createDeveloperParameters;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.measure.ws.MetricDtoToWsMetric.metricDtoToWsMetric;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriod.snapshotToWsPeriods;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

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
        new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
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

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true)
      .setSince("6.6");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setInternal(true)
      .setSince("7.1");

    createMetricKeysParameter(action);
    createAdditionalFieldsParameter(action);
    createDeveloperParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (request.param(PARAM_DEVELOPER_ID) != null || request.param(PARAM_DEVELOPER_KEY) != null) {
      throw new NotFoundException("The Developer Cockpit feature has been dropped. The specified developer cannot be found.");
    }

    ComponentWsResponse componentWsResponse = doHandle(toComponentWsRequest(request));
    writeProtobuf(componentWsResponse, request, response);
  }

  private ComponentWsResponse doHandle(ComponentRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String branch = request.getBranch();
      String pullRequest = request.getPullRequest();
      ComponentDto component = loadComponent(dbSession, request, branch, pullRequest);
      checkPermissions(component);
      SnapshotDto analysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, component.projectUuid()).orElse(null);

      boolean isSLBorPR = isSLBorPR(dbSession, component, branch, pullRequest);

      Set<String> metricKeysToRequest = new HashSet<>(request.metricKeys);

      if (isSLBorPR) {
        SLBorPRMeasureFix.addReplacementMetricKeys(metricKeysToRequest);
      }

      List<MetricDto> metrics = searchMetrics(dbSession, metricKeysToRequest);
      List<LiveMeasureDto> measures = searchMeasures(dbSession, component, metrics);
      Map<MetricDto, LiveMeasureDto> measuresByMetric = getMeasuresByMetric(measures, metrics);

      if (isSLBorPR) {
        Set<String> originalMetricKeys = new HashSet<>(request.metricKeys);
        SLBorPRMeasureFix.createReplacementMeasures(metrics, measuresByMetric, originalMetricKeys);
        SLBorPRMeasureFix.removeMetricsNotRequested(metrics, originalMetricKeys);
      }

      Optional<Measures.Period> period = snapshotToWsPeriods(analysis);
      Optional<ComponentDto> refComponent = getReferenceComponent(dbSession, component);
      return buildResponse(request, component, refComponent, measuresByMetric, metrics, period);
    }
  }

  public List<MetricDto> searchMetrics(DbSession dbSession, Collection<String> metricKeys) {
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    if (metrics.size() < metricKeys.size()) {
      Set<String> foundMetricKeys = metrics.stream().map(MetricDto::getKey).collect(Collectors.toSet());
      Set<String> missingMetricKeys = metricKeys.stream().filter(m -> !foundMetricKeys.contains(m)).collect(Collectors.toSet());
      throw new NotFoundException(format("The following metric keys are not found: %s", Joiner.on(", ").join(missingMetricKeys)));
    }

    return metrics;
  }

  private List<LiveMeasureDto> searchMeasures(DbSession dbSession, ComponentDto component, Collection<MetricDto> metrics) {
    Set<Integer> metricIds = metrics.stream().map(MetricDto::getId).collect(Collectors.toSet());
    List<LiveMeasureDto> measures = dbClient.liveMeasureDao().selectByComponentUuidsAndMetricIds(dbSession, singletonList(component.uuid()), metricIds);
    addBestValuesToMeasures(measures, component, metrics);
    return measures;
  }

  private static Map<MetricDto, LiveMeasureDto> getMeasuresByMetric(List<LiveMeasureDto> measures, Collection<MetricDto> metrics) {
    Map<Integer, MetricDto> metricsById = Maps.uniqueIndex(metrics, MetricDto::getId);
    Map<MetricDto, LiveMeasureDto> measuresByMetric = new HashMap<>();
    for (LiveMeasureDto measure : measures) {
      MetricDto metric = metricsById.get(measure.getMetricId());
      measuresByMetric.put(metric, measure);
    }
    return measuresByMetric;
  }

  /**
   * Conditions for best value measure:
   * <ul>
   * <li>component is a production file or test file</li>
   * <li>metric is optimized for best value</li>
   * </ul>
   */
  private static void addBestValuesToMeasures(List<LiveMeasureDto> measures, ComponentDto component, Collection<MetricDto> metrics) {
    if (!QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE.contains(component.qualifier())) {
      return;
    }

    List<MetricDtoWithBestValue> metricWithBestValueList = metrics.stream()
      .filter(MetricDtoFunctions.isOptimizedForBestValue())
      .map(MetricDtoWithBestValue::new)
      .collect(MoreCollectors.toList(metrics.size()));
    Map<Integer, LiveMeasureDto> measuresByMetricId = Maps.uniqueIndex(measures, LiveMeasureDto::getMetricId);

    for (MetricDtoWithBestValue metricWithBestValue : metricWithBestValueList) {
      if (measuresByMetricId.get(metricWithBestValue.getMetric().getId()) == null) {
        measures.add(metricWithBestValue.getBestValue());
      }
    }
  }

  private boolean isSLBorPR(DbSession dbSession, ComponentDto component, @Nullable String branch, @Nullable String pullRequest) {
    if (branch != null) {
      return dbClient.branchDao().selectByUuid(dbSession, component.projectUuid())
        .map(b -> b.getBranchType() == BranchType.SHORT).orElse(false);
    }
    return pullRequest != null;
  }

  private ComponentDto loadComponent(DbSession dbSession, ComponentRequest request, @Nullable String branch, @Nullable String pullRequest) {
    String componentKey = request.getComponent();
    String componentId = request.getComponentId();
    checkArgument(componentId == null || (branch == null && pullRequest == null), "Parameter '%s' cannot be used at the same time as '%s' or '%s'",
      DEPRECATED_PARAM_COMPONENT_ID, PARAM_BRANCH, PARAM_PULL_REQUEST);

    if (branch == null && pullRequest == null) {
      return componentFinder.getByUuidOrKey(dbSession, componentId, componentKey, COMPONENT_ID_AND_KEY);
    }

    checkRequest(componentKey != null, "The '%s' parameter is missing", PARAM_COMPONENT);
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  private Optional<ComponentDto> getReferenceComponent(DbSession dbSession, ComponentDto component) {
    if (component.getCopyResourceUuid() == null) {
      return Optional.empty();
    }

    return dbClient.componentDao().selectByUuid(dbSession, component.getCopyResourceUuid());
  }

  private static ComponentWsResponse buildResponse(ComponentRequest request, ComponentDto component, Optional<ComponentDto> refComponent,
    Map<MetricDto, LiveMeasureDto> measuresByMetric, Collection<MetricDto> metrics, Optional<Measures.Period> period) {
    ComponentWsResponse.Builder response = ComponentWsResponse.newBuilder();

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
      if (additionalFields.contains(ADDITIONAL_PERIODS) && period.isPresent()) {
        response.getPeriodsBuilder().addPeriods(period.get());
      }
    }

    return response.build();
  }

  private static ComponentRequest toComponentWsRequest(Request request) {
    ComponentRequest componentRequest = new ComponentRequest()
      .setComponentId(request.param(DEPRECATED_PARAM_COMPONENT_ID))
      .setComponent(request.param(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST))
      .setAdditionalFields(request.paramAsStrings(PARAM_ADDITIONAL_FIELDS))
      .setMetricKeys(request.mandatoryParamAsStrings(PARAM_METRIC_KEYS));
    checkRequest(!componentRequest.getMetricKeys().isEmpty(), "At least one metric key must be provided");
    return componentRequest;
  }

  private void checkPermissions(ComponentDto baseComponent) {
    userSession.checkComponentPermission(UserRole.USER, baseComponent);
  }

  private static class ComponentRequest {
    private String componentId;
    private String component;
    private String branch;
    private String pullRequest;
    private List<String> metricKeys;
    private List<String> additionalFields;

    /**
     * @deprecated since 6.6, please use {@link #getComponent()} instead
     */
    @Deprecated
    @CheckForNull
    private String getComponentId() {
      return componentId;
    }

    /**
     * @deprecated since 6.6, please use {@link #setComponent(String)} instead
     */
    @Deprecated
    private ComponentRequest setComponentId(@Nullable String componentId) {
      this.componentId = componentId;
      return this;
    }

    @CheckForNull
    private String getComponent() {
      return component;
    }

    private ComponentRequest setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    @CheckForNull
    private String getBranch() {
      return branch;
    }

    private ComponentRequest setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    @CheckForNull
    public String getPullRequest() {
      return pullRequest;
    }

    public ComponentRequest setPullRequest(@Nullable String pullRequest) {
      this.pullRequest = pullRequest;
      return this;
    }

    private List<String> getMetricKeys() {
      return metricKeys;
    }

    private ComponentRequest setMetricKeys(@Nullable List<String> metricKeys) {
      this.metricKeys = metricKeys;
      return this;
    }

    @CheckForNull
    private List<String> getAdditionalFields() {
      return additionalFields;
    }

    private ComponentRequest setAdditionalFields(@Nullable List<String> additionalFields) {
      this.additionalFields = additionalFields;
      return this;
    }
  }
}
