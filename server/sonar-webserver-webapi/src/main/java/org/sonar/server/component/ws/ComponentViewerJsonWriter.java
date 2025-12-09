/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.BooleanUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.user.UserSession;

import static org.sonar.api.measures.CoreMetrics.COVERAGE;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.LINES;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS;

public class ComponentViewerJsonWriter {

  private final DbClient dbClient;

  public ComponentViewerJsonWriter(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void writeComponentWithoutFav(JsonWriter json, EntityDto entity, ComponentDto component, @Nullable String branch, @Nullable String pullRequest) {
    json.prop("key", component.getKey());
    json.prop("uuid", component.uuid());
    json.prop("path", component.path());
    json.prop("name", component.name());
    json.prop("longName", component.longName());
    json.prop("q", component.qualifier());

    json.prop("project", entity.getKey());
    json.prop("projectName", entity.getName());
    if (branch != null) {
      json.prop("branch", branch);
    }
    if (pullRequest != null) {
      json.prop("pullRequest", pullRequest);
    }
  }

  public void writeComponent(JsonWriter json, EntityDto entity, ComponentDto component, UserSession userSession, DbSession session, @Nullable String branch,
    @Nullable String pullRequest) {
    writeComponentWithoutFav(json, entity, component, branch, pullRequest);

    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
        .setKey("favourite")
        .setEntityUuid(entity.getUuid())
        .setUserUuid(userSession.getUuid())
        .build(),
      session);
    boolean isFavourite = propertyDtos.size() == 1;
    json.prop("fav", isFavourite);
  }

  public void writeMeasures(JsonWriter json, ComponentDto component, DbSession session) {
    MeasureDto measureDto = loadMeasures(component, session);

    json.name("measures").beginObject();
    json.prop("lines", formatMeasure(measureDto, LINES));
    json.prop("coverage", formatMeasure(measureDto, COVERAGE));
    json.prop("duplicationDensity", formatMeasure(measureDto, DUPLICATED_LINES_DENSITY));
    json.prop("issues", formatMeasure(measureDto, VIOLATIONS));
    json.prop("tests", formatMeasure(measureDto, TESTS));
    json.endObject();
  }

  @CheckForNull
  private MeasureDto loadMeasures(ComponentDto component, DbSession dbSession) {
    return dbClient.measureDao().selectByComponentUuid(dbSession, component.uuid()).orElse(null);
  }

  @CheckForNull
  private static String formatMeasure(@Nullable MeasureDto measureDto, Metric<?> metric) {
    if (measureDto == null) {
      return null;
    }
    Double value = getDoubleValue(measureDto, metric);
    if (value != null) {
      return Double.toString(value);
    }
    return null;
  }

  @CheckForNull
  private static Double getDoubleValue(MeasureDto measureDto, Metric<?> metric) {
    Double value = measureDto.getDouble(metric.getKey());
    if (BooleanUtils.isTrue(metric.isOptimizedBestValue()) && value == null) {
      value = metric.getBestValue();
    }
    return value;
  }
}
