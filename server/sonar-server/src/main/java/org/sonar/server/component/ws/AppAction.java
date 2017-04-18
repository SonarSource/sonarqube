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
package org.sonar.server.component.ws;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class AppAction implements ComponentsWsAction {

  private static final String PARAM_COMPONENT_ID = "componentId";
  private static final String PARAM_COMPONENT = "component";
  private static final String PARAM_PERIOD = "period";
  static final List<String> METRIC_KEYS = newArrayList(CoreMetrics.LINES_KEY, CoreMetrics.VIOLATIONS_KEY,
    CoreMetrics.COVERAGE_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, CoreMetrics.TESTS_KEY,
    CoreMetrics.TECHNICAL_DEBT_KEY, CoreMetrics.SQALE_RATING_KEY, CoreMetrics.SQALE_DEBT_RATIO_KEY);

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

    action
      .createParam(PARAM_PERIOD)
      .setDescription("User leak Period in order to get differential measures")
      .setDeprecatedSince("6.4")
      .setPossibleValues(1);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession session = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByUuidOrKey(session,
        request.param(PARAM_COMPONENT_ID),
        request.param(PARAM_COMPONENT),
        ParamNames.COMPONENT_ID_AND_COMPONENT);
      userSession.checkComponentPermission(UserRole.USER, component);

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      Map<String, MeasureDto> measuresByMetricKey = measuresByMetricKey(component, session);
      appendComponent(json, component, userSession, session);
      appendPermissions(json, component, userSession);
      appendMeasures(json, measuresByMetricKey);
      json.endObject();
      json.close();
    }
  }

  private void appendComponent(JsonWriter json, ComponentDto component, UserSession userSession, DbSession session) {
    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey("favourite")
      .setComponentId(component.getId())
      .setUserId(userSession.getUserId())
      .build(),
      session);
    boolean isFavourite = propertyDtos.size() == 1;

    json.prop("key", component.key());
    json.prop("uuid", component.uuid());
    json.prop("path", component.path());
    json.prop("name", component.name());
    json.prop("longName", component.longName());
    json.prop("q", component.qualifier());

    ComponentDto parentProject = retrieveRootIfNotCurrentComponent(component, session);
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, component.projectUuid());

    // Do not display parent project if parent project and project are the same
    boolean displayParentProject = parentProject != null && !parentProject.uuid().equals(project.uuid());
    json.prop("subProject", displayParentProject ? parentProject.key() : null);
    json.prop("subProjectName", displayParentProject ? parentProject.longName() : null);
    json.prop("project", project.key());
    json.prop("projectName", project.longName());

    json.prop("fav", isFavourite);
  }

  private static void appendPermissions(JsonWriter json, ComponentDto component, UserSession userSession) {
    boolean hasBrowsePermission = userSession.hasComponentPermission(UserRole.USER, component);
    json.prop("canMarkAsFavorite", userSession.isLoggedIn() && hasBrowsePermission);
  }

  private static void appendMeasures(JsonWriter json, Map<String, MeasureDto> measuresByMetricKey) {
    json.name("measures").beginObject();
    json.prop("lines", formatMeasure(measuresByMetricKey, CoreMetrics.LINES));
    json.prop("coverage", formatCoverageMeasure(measuresByMetricKey));
    json.prop("duplicationDensity", formatMeasure(measuresByMetricKey, CoreMetrics.DUPLICATED_LINES_DENSITY));
    json.prop("issues", formatMeasure(measuresByMetricKey, CoreMetrics.VIOLATIONS));
    json.prop("tests", formatMeasure(measuresByMetricKey, CoreMetrics.TESTS));
    json.endObject();
  }

  private Map<String, MeasureDto> measuresByMetricKey(ComponentDto component, DbSession session) {
    MeasureQuery query = MeasureQuery.builder().setComponentUuid(component.uuid()).setMetricKeys(METRIC_KEYS).build();
    List<MeasureDto> measures = dbClient.measureDao().selectByQuery(session, query);
    Set<Integer> metricIds = measures.stream().map(MeasureDto::getMetricId).collect(Collectors.toSet());
    List<MetricDto> metrics = dbClient.metricDao().selectByIds(session, metricIds);
    Map<Integer, MetricDto> metricsById = Maps.uniqueIndex(metrics, MetricDto::getId);
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
  private static String formatCoverageMeasure(Map<String, MeasureDto> measuresByMetricKey) {
    MeasureDto coverage = measuresByMetricKey.get(CoreMetrics.COVERAGE_KEY);
    return formatMeasure(coverage, CoreMetrics.COVERAGE);
  }

  @CheckForNull
  private static String formatMeasure(Map<String, MeasureDto> measuresByMetricKey, Metric metric) {
    MeasureDto measure = measuresByMetricKey.get(metric.getKey());
    return formatMeasure(measure, metric);
  }

  private static String formatMeasure(@Nullable MeasureDto measure, Metric metric) {
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
  private static Double getDoubleValue(MeasureDto measure, Metric metric) {
    Double value = measure.getValue();
    if (BooleanUtils.isTrue(metric.isOptimizedBestValue()) && value == null) {
      value = metric.getBestValue();
    }
    return value;
  }
}
