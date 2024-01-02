/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.db.component.BranchDto;
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

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.sonar.server.component.ws.MeasuresWsParameters.ACTION_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_METRICS;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_PERIOD;
import static org.sonar.server.component.ws.MeasuresWsParameters.DEPRECATED_ADDITIONAL_PERIODS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.measure.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createAdditionalFieldsParameter;
import static org.sonar.server.measure.ws.MeasuresWsParametersBuilder.createMetricKeysParameter;
import static org.sonar.server.measure.ws.MetricDtoToWsMetric.metricDtoToWsMetric;
import static org.sonar.server.measure.ws.SnapshotDtoToWsPeriod.snapshotToWsPeriods;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
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
      .setDescription("Return component with specified measures.<br>" +
        "Requires the following permission: 'Browse' on the project of specified component.")
      .setResponseExample(getClass().getResource("component-example.json"))
      .setSince("5.4")
      .setChangelog(
        new Change("9.3", format("The use of the following metrics in 'metricKeys' parameter is deprecated: %s",
          MeasuresWsModule.getDeprecatedMetrics())),
        new Change("8.8", "deprecated response field 'id' has been removed"),
        new Change("8.8", "deprecated response field 'refId' has been removed."),
        new Change("8.1", "the response field periods under measures field is deprecated. Use period instead."),
        new Change("8.1", "the response field periods is deprecated. Use period instead."),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
        new Change("6.6", "the response field 'id' is deprecated. Use 'key' instead."),
        new Change("6.6", "the response field 'refId' is deprecated. Use 'refKey' instead."))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setSince("6.6");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setSince("7.1");

    createMetricKeysParameter(action);
    createAdditionalFieldsParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ComponentWsResponse componentWsResponse = doHandle(toComponentWsRequest(request));
    writeProtobuf(componentWsResponse, request, response);
  }

  private ComponentWsResponse doHandle(ComponentRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String branch = request.getBranch();
      String pullRequest = request.getPullRequest();
      ComponentDto component = loadComponent(dbSession, request, branch, pullRequest);
      checkPermissions(component);
      SnapshotDto analysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, component.branchUuid()).orElse(null);

      boolean isPR = isPR(pullRequest);

      Set<String> metricKeysToRequest = new HashSet<>(request.metricKeys);

      if (isPR) {
        PrMeasureFix.addReplacementMetricKeys(metricKeysToRequest);
      }

      List<MetricDto> metrics = searchMetrics(dbSession, metricKeysToRequest);
      List<LiveMeasureDto> measures = searchMeasures(dbSession, component, metrics);
      Map<MetricDto, LiveMeasureDto> measuresByMetric = getMeasuresByMetric(measures, metrics);

      if (isPR) {
        Set<String> originalMetricKeys = new HashSet<>(request.metricKeys);
        PrMeasureFix.createReplacementMeasures(metrics, measuresByMetric, originalMetricKeys);
        PrMeasureFix.removeMetricsNotRequested(metrics, originalMetricKeys);
      }

      Optional<Measures.Period> period = snapshotToWsPeriods(analysis);
      Optional<RefComponent> reference = getReference(dbSession, component);
      return buildResponse(request, component, reference, measuresByMetric, metrics, period);
    }
  }

  public List<MetricDto> searchMetrics(DbSession dbSession, Collection<String> metricKeys) {
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    if (metrics.size() < metricKeys.size()) {
      Set<String> foundMetricKeys = metrics.stream().map(MetricDto::getKey).collect(Collectors.toSet());
      Set<String> missingMetricKeys = metricKeys.stream().filter(m -> !foundMetricKeys.contains(m)).collect(Collectors.toSet());
      throw new NotFoundException(format("The following metric keys are not found: %s", String.join(", ", missingMetricKeys)));
    }

    return metrics;
  }

  private List<LiveMeasureDto> searchMeasures(DbSession dbSession, ComponentDto component, Collection<MetricDto> metrics) {
    Set<String> metricUuids = metrics.stream().map(MetricDto::getUuid).collect(Collectors.toSet());
    List<LiveMeasureDto> measures = dbClient.liveMeasureDao().selectByComponentUuidsAndMetricUuids(dbSession, singletonList(component.uuid()), metricUuids);
    addBestValuesToMeasures(measures, component, metrics);
    return measures;
  }

  private static Map<MetricDto, LiveMeasureDto> getMeasuresByMetric(List<LiveMeasureDto> measures, Collection<MetricDto> metrics) {
    Map<String, MetricDto> metricsByUuid = Maps.uniqueIndex(metrics, MetricDto::getUuid);
    Map<MetricDto, LiveMeasureDto> measuresByMetric = new HashMap<>();
    for (LiveMeasureDto measure : measures) {
      MetricDto metric = metricsByUuid.get(measure.getMetricUuid());
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
    Map<String, LiveMeasureDto> measuresByMetricUuid = Maps.uniqueIndex(measures, LiveMeasureDto::getMetricUuid);

    for (MetricDtoWithBestValue metricWithBestValue : metricWithBestValueList) {
      if (measuresByMetricUuid.get(metricWithBestValue.getMetric().getUuid()) == null) {
        measures.add(metricWithBestValue.getBestValue());
      }
    }
  }

  private static boolean isPR(@Nullable String pullRequest) {
    return pullRequest != null;
  }

  private ComponentDto loadComponent(DbSession dbSession, ComponentRequest request, @Nullable String branch, @Nullable String pullRequest) {
    String componentKey = request.getComponent();
    checkRequest(componentKey != null, "The '%s' parameter is missing", PARAM_COMPONENT);
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  private Optional<RefComponent> getReference(DbSession dbSession, ComponentDto component) {
    String copyComponentUuid = component.getCopyComponentUuid();
    if (copyComponentUuid == null) {
      return Optional.empty();
    }

    Optional<ComponentDto> refComponent = dbClient.componentDao().selectByUuid(dbSession, copyComponentUuid);
    if (refComponent.isEmpty()) {
      return Optional.empty();
    }
    Optional<BranchDto> refBranch = dbClient.branchDao().selectByUuid(dbSession, refComponent.get().branchUuid());
    return refBranch.map(rb -> new RefComponent(rb, refComponent.get()));
  }

  private static ComponentWsResponse buildResponse(ComponentRequest request, ComponentDto component, Optional<RefComponent> reference,
    Map<MetricDto, LiveMeasureDto> measuresByMetric, Collection<MetricDto> metrics, Optional<Measures.Period> period) {
    ComponentWsResponse.Builder response = ComponentWsResponse.newBuilder();
    boolean isMainBranch = component.getMainBranchProjectUuid() == null;

    if (reference.isPresent()) {
      BranchDto refBranch = reference.get().getRefBranch();
      ComponentDto refComponent = reference.get().getComponent();
      response.setComponent(componentDtoToWsComponent(component, measuresByMetric, singletonMap(refComponent.uuid(), refComponent),
        refBranch.isMain() ? null : refBranch.getBranchKey(), null));
    } else {
      response.setComponent(componentDtoToWsComponent(component, measuresByMetric, emptyMap(), isMainBranch ? null : request.getBranch(), request.getPullRequest()));
    }

    setAdditionalFields(request, metrics, period, response);

    return response.build();
  }

  private static void setAdditionalFields(ComponentRequest request, Collection<MetricDto> metrics, Optional<Measures.Period> period, ComponentWsResponse.Builder response) {
    List<String> additionalFields = request.getAdditionalFields();
    if (additionalFields != null) {
      if (additionalFields.contains(ADDITIONAL_METRICS)) {
        for (MetricDto metric : metrics) {
          response.getMetricsBuilder().addMetrics(metricDtoToWsMetric(metric));
        }
      }

      // backward compatibility
      if (additionalFields.contains(DEPRECATED_ADDITIONAL_PERIODS) && period.isPresent()) {
        response.getPeriodsBuilder().addPeriods(period.get());
      }

      if (additionalFields.contains(ADDITIONAL_PERIOD) && period.isPresent()) {
        response.setPeriod(period.get());
      }
    }
  }

  private static ComponentRequest toComponentWsRequest(Request request) {
    ComponentRequest componentRequest = new ComponentRequest()
      .setComponent(request.mandatoryParam(PARAM_COMPONENT))
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
    private String component;
    private String branch;
    private String pullRequest;
    private List<String> metricKeys;
    private List<String> additionalFields;

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

  private static class RefComponent {

    private final BranchDto refBranch;
    private final ComponentDto component;

    public RefComponent(BranchDto refBranch, ComponentDto component) {
      this.refBranch = refBranch;
      this.component = component;
    }

    public BranchDto getRefBranch() {
      return refBranch;
    }

    public ComponentDto getComponent() {
      return component;
    }
  }
}
