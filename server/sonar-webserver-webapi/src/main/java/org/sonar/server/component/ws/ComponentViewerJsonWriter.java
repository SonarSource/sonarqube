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
package org.sonar.server.component.ws;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.user.UserSession;

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

public class ComponentViewerJsonWriter {
  private static final List<String> METRIC_KEYS = List.of(
    LINES_KEY,
    VIOLATIONS_KEY,
    COVERAGE_KEY,
    DUPLICATED_LINES_DENSITY_KEY,
    TESTS_KEY);

  private final DbClient dbClient;

  public ComponentViewerJsonWriter(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void writeComponentWithoutFav(JsonWriter json, ComponentDto component, DbSession session, @Nullable String branch, @Nullable String pullRequest) {
    json.prop("key", component.getKey());
    json.prop("uuid", component.uuid());
    json.prop("path", component.path());
    json.prop("name", component.name());
    json.prop("longName", component.longName());
    json.prop("q", component.qualifier());

    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, component.branchUuid());

    json.prop("project", project.getKey());
    json.prop("projectName", project.longName());
    if (branch != null) {
      json.prop("branch", branch);
    }
    if (pullRequest != null) {
      json.prop("pullRequest", pullRequest);
    }
  }

  public void writeComponent(JsonWriter json, ComponentDto component, UserSession userSession, DbSession session, @Nullable String branch,
    @Nullable String pullRequest) {
    writeComponentWithoutFav(json, component, session, branch, pullRequest);

    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey("favourite")
      .setComponentUuid(component.uuid())
      .setUserUuid(userSession.getUuid())
      .build(),
      session);
    boolean isFavourite = propertyDtos.size() == 1;
    json.prop("fav", isFavourite);
  }

  public void writeMeasures(JsonWriter json, ComponentDto component, DbSession session) {
    Map<String, LiveMeasureDto> measuresByMetricKey = loadMeasuresGroupedByMetricKey(component, session);

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
    Map<String, MetricDto> metricsByUuid = Maps.uniqueIndex(metrics, MetricDto::getUuid);
    List<LiveMeasureDto> measures = dbClient.liveMeasureDao()
      .selectByComponentUuidsAndMetricUuids(dbSession, Collections.singletonList(component.uuid()), metricsByUuid.keySet());
    return Maps.uniqueIndex(measures, m -> metricsByUuid.get(m.getMetricUuid()).getKey());
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
