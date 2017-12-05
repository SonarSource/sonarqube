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
package org.sonar.server.component.ws;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.sonar.api.measures.CoreMetrics.COVERAGE;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_COMPONENT;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;

public class AppAction implements ComponentsWsAction {

  private static final String PARAM_COMPONENT_ID = "componentId";
  private static final String PARAM_COMPONENT = "component";
  private static final List<String> METRIC_KEYS = unmodifiableList(asList(
    LINES_KEY,
    VIOLATIONS_KEY,
    COVERAGE_KEY,
    DUPLICATED_LINES_DENSITY_KEY,
    TESTS_KEY));

  private final DbClient dbClient;

  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public AppAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app")
      .setDescription("Coverage data required for rendering the component viewer.<br>" +
        "Requires the following permission: 'Browse'.")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.4")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_COMPONENT_ID)
      .setDescription("Component ID")
      .setDeprecatedSince("6.4")
      .setDeprecatedKey("uuid", "6.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("6.4");

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setSince("6.6")
      .setInternal(true)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession session = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(session, request);
      userSession.checkComponentPermission(UserRole.USER, component);

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      Map<String, LiveMeasureDto> measuresByMetricKey = loadMeasuresGroupedByMetricKey(component, session);
      appendComponent(json, component, userSession, session);
      appendPermissions(json, userSession);
      appendMeasures(json, measuresByMetricKey);
      json.endObject();
      json.close();
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String componentUuid = request.param(PARAM_COMPONENT_ID);
    String branch = request.param("branch");
    checkArgument(componentUuid == null || branch == null, "'%s' and '%s' parameters cannot be used at the same time", PARAM_COMPONENT_ID, PARAM_BRANCH);
    if (branch == null) {
      return componentFinder.getByUuidOrKey(dbSession, componentUuid, request.param(PARAM_COMPONENT), COMPONENT_ID_AND_COMPONENT);
    }
    return componentFinder.getByKeyAndOptionalBranch(dbSession, request.mandatoryParam(PARAM_COMPONENT), branch);
  }

  private void appendComponent(JsonWriter json, ComponentDto component, UserSession userSession, DbSession session) {
    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey("favourite")
      .setComponentId(component.getId())
      .setUserId(userSession.getUserId())
      .build(),
      session);
    boolean isFavourite = propertyDtos.size() == 1;

    json.prop("key", component.getKey());
    json.prop("uuid", component.uuid());
    json.prop("path", component.path());
    json.prop("name", component.name());
    json.prop("longName", component.longName());
    json.prop("q", component.qualifier());

    ComponentDto parentProject = retrieveRootIfNotCurrentComponent(component, session);
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, component.projectUuid());

    // Do not display parent project if parent project and project are the same
    boolean displayParentProject = parentProject != null && !parentProject.uuid().equals(project.uuid());
    json.prop("subProject", displayParentProject ? parentProject.getKey() : null);
    json.prop("subProjectName", displayParentProject ? parentProject.longName() : null);
    json.prop("project", project.getKey());
    json.prop("projectName", project.longName());
    String branch = project.getBranch();
    if (branch != null) {
      json.prop("branch", branch);
    }

    json.prop("fav", isFavourite);
  }

  private static void appendPermissions(JsonWriter json, UserSession userSession) {
    json.prop("canMarkAsFavorite", userSession.isLoggedIn());
  }

  private static void appendMeasures(JsonWriter json, Map<String, LiveMeasureDto> measuresByMetricKey) {
    json.name("measures").beginObject();
    json.prop("lines", formatMeasure(measuresByMetricKey, LINES));
    json.prop("coverage", formatMeasure(measuresByMetricKey, COVERAGE));
    json.prop("duplicationDensity", formatMeasure(measuresByMetricKey, DUPLICATED_LINES_DENSITY));
    json.prop("issues", formatMeasure(measuresByMetricKey, VIOLATIONS));
    json.prop("tests", formatMeasure(measuresByMetricKey, TESTS));
    json.endObject();
  }

  private Map<String, LiveMeasureDto> loadMeasuresGroupedByMetricKey(ComponentDto component, DbSession dbSession) {
    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, METRIC_KEYS);
    Map<Integer, MetricDto> metricsById = Maps.uniqueIndex(metrics, MetricDto::getId);
    List<LiveMeasureDto> measures = dbClient.liveMeasureDao()
      .selectByComponentUuidsAndMetricIds(dbSession, Collections.singletonList(component.uuid()), metricsById.keySet());
    return Maps.uniqueIndex(measures, m -> metricsById.get(m.getMetricId()).getKey());
  }

  @CheckForNull
  private ComponentDto retrieveRootIfNotCurrentComponent(ComponentDto componentDto, DbSession session) {
    if (componentDto.uuid().equals(componentDto.getRootUuid())) {
      return null;
    }
    return dbClient.componentDao().selectOrFailByUuid(session, componentDto.getRootUuid());
  }

  @CheckForNull
  private static String formatMeasure(Map<String, LiveMeasureDto> measuresByMetricKey, Metric metric) {
    LiveMeasureDto measure = measuresByMetricKey.get(metric.getKey());
    return formatMeasure(measure, metric);
  }

  private static String formatMeasure(@Nullable LiveMeasureDto measure, Metric metric) {
    if (measure == null) {
      return null;
    }
    Double value = getDoubleValue(measure, metric);
    if (value != null) {
      return Double.toString(value);
    }
    return null;
  }

  @CheckForNull
  private static Double getDoubleValue(LiveMeasureDto measure, Metric metric) {
    Double value = measure.getValue();
    if (BooleanUtils.isTrue(metric.isOptimizedBestValue()) && value == null) {
      value = metric.getBestValue();
    }
    return value;
  }
}
