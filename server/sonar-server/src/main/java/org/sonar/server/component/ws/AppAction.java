/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.component.ws;

import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;

public class AppAction implements RequestHandler {

  private static final String PARAM_UUID = "uuid";
  private static final String PARAM_PERIOD = "period";
  private static final List<String> METRIC_KEYS = newArrayList(CoreMetrics.LINES_KEY, CoreMetrics.VIOLATIONS_KEY,
    CoreMetrics.COVERAGE_KEY, CoreMetrics.IT_COVERAGE_KEY, CoreMetrics.OVERALL_COVERAGE_KEY,
    CoreMetrics.DUPLICATED_LINES_DENSITY_KEY, CoreMetrics.TESTS_KEY,
    CoreMetrics.TECHNICAL_DEBT_KEY, CoreMetrics.SQALE_RATING_KEY, CoreMetrics.SQALE_DEBT_RATIO_KEY);

  private final DbClient dbClient;

  private final Durations durations;
  private final I18n i18n;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public AppAction(DbClient dbClient, Durations durations, I18n i18n, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.durations = durations;
    this.i18n = i18n;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app")
      .setDescription("Coverage data required for rendering the component viewer")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.4")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_UUID)
      .setRequired(true)
      .setDescription("Component ID")
      .setExampleValue(UUID_EXAMPLE_01);

    action
      .createParam(PARAM_PERIOD)
      .setDescription("Period index in order to get differential measures")
      .setPossibleValues(1, 2, 3, 4, 5);
  }

  @Override
  public void handle(Request request, Response response) {
    String componentUuid = request.mandatoryParam(PARAM_UUID);

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = componentFinder.getByUuid(session, componentUuid);
      userSession.checkComponentPermission(UserRole.USER, component.getKey());

      Map<String, MeasureDto> measuresByMetricKey = measuresByMetricKey(component, session);
      appendComponent(json, component, userSession, session);
      appendPermissions(json, component, userSession);
      appendMeasures(json, measuresByMetricKey);

    } finally {
      MyBatis.closeQuietly(session);
    }

    json.endObject();
    json.close();
  }

  private void appendComponent(JsonWriter json, ComponentDto component, UserSession userSession, DbSession session) {
    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey("favourite")
      .setComponentId(component.getId())
      .setUserId(userSession.getUserId())
      .build(),
      session
      );
    boolean isFavourite = propertyDtos.size() == 1;

    json.prop("key", component.key());
    json.prop("uuid", component.uuid());
    json.prop("path", component.path());
    json.prop("name", component.name());
    json.prop("longName", component.longName());
    json.prop("q", component.qualifier());

    ComponentDto parentProject = nullableComponentById(component.parentProjectId(), session);
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, component.projectUuid());

    // Do not display parent project if parent project and project are the same
    boolean displayParentProject = parentProject != null && !parentProject.getId().equals(project.getId());
    json.prop("subProject", displayParentProject ? parentProject.key() : null);
    json.prop("subProjectName", displayParentProject ? parentProject.longName() : null);
    json.prop("project", project.key());
    json.prop("projectName", project.longName());

    json.prop("fav", isFavourite);
  }

  private static void appendPermissions(JsonWriter json, ComponentDto component, UserSession userSession) {
    boolean hasBrowsePermission = userSession.hasComponentPermission(UserRole.USER, component.key());
    json.prop("canMarkAsFavourite", userSession.isLoggedIn() && hasBrowsePermission);
    json.prop("canCreateManualIssue", userSession.isLoggedIn() && hasBrowsePermission);
  }

  private void appendMeasures(JsonWriter json, Map<String, MeasureDto> measuresByMetricKey) {
    json.name("measures").beginObject();
    json.prop("lines", formatMeasure(measuresByMetricKey.get(CoreMetrics.LINES_KEY)));
    json.prop("coverage", formatMeasure(coverageMeasure(measuresByMetricKey)));
    json.prop("duplicationDensity", formatMeasure(measuresByMetricKey.get(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY)));
    json.prop("issues", formatMeasure(measuresByMetricKey.get(CoreMetrics.VIOLATIONS_KEY)));
    json.prop("tests", formatMeasure(measuresByMetricKey.get(CoreMetrics.TESTS_KEY)));
    json.prop("debt", formatMeasure(measuresByMetricKey.get(CoreMetrics.TECHNICAL_DEBT_KEY)));
    json.prop("sqaleRating", formatMeasure(measuresByMetricKey.get(CoreMetrics.SQALE_RATING_KEY)));
    json.prop("debtRatio", formatMeasure(measuresByMetricKey.get(CoreMetrics.SQALE_DEBT_RATIO_KEY)));
    json.endObject();
  }

  private static MeasureDto coverageMeasure(Map<String, MeasureDto> measuresByMetricKey) {
    MeasureDto overallCoverage = measuresByMetricKey.get(CoreMetrics.OVERALL_COVERAGE_KEY);
    MeasureDto itCoverage = measuresByMetricKey.get(CoreMetrics.IT_COVERAGE_KEY);
    MeasureDto utCoverage = measuresByMetricKey.get(CoreMetrics.COVERAGE_KEY);
    if (overallCoverage != null) {
      return overallCoverage;
    } else if (utCoverage != null) {
      return utCoverage;
    } else {
      return itCoverage;
    }
  }

  private Map<String, MeasureDto> measuresByMetricKey(ComponentDto component, DbSession session) {
    Map<String, MeasureDto> measuresByMetricKey = newHashMap();
    String fileKey = component.getKey();
    for (MeasureDto measureDto : dbClient.measureDao().selectByComponentKeyAndMetricKeys(session, fileKey, METRIC_KEYS)) {
      measuresByMetricKey.put(measureDto.getMetricKey(), measureDto);
    }
    return measuresByMetricKey;
  }

  @CheckForNull
  private ComponentDto nullableComponentById(@Nullable Long componentId, DbSession session) {
    if (componentId != null) {
      return dbClient.componentDao().selectOrFailById(session, componentId);
    }
    return null;
  }

  @CheckForNull
  private String formatMeasure(@Nullable MeasureDto measure) {
    if (measure == null) {
      return null;
    }

    Metric metric = CoreMetrics.getMetric(measure.getMetricKey());
    Metric.ValueType metricType = metric.getType();
    Double value = getDoubleValue(measure, metric);
    String data = measure.getData();
    if (value != null) {
      switch (metricType) {
        case FLOAT:
          return i18n.formatDouble(userSession.locale(), value);
        case INT:
          return i18n.formatInteger(userSession.locale(), value.intValue());
        case PERCENT:
          return i18n.formatDouble(userSession.locale(), value) + "%";
        case WORK_DUR:
          return durations.format(userSession.locale(), durations.create(value.longValue()), Durations.DurationFormat.SHORT);
      }
    }
    if ((metricType.equals(Metric.ValueType.STRING) || metricType.equals(Metric.ValueType.RATING)) && data != null) {
      return data;
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
