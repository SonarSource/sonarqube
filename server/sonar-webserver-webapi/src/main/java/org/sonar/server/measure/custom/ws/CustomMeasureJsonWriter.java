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
package org.sonar.server.measure.custom.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.metric.ws.MetricJsonWriter;
import org.sonar.server.user.ws.UserJsonWriter;

import static org.sonar.server.ws.JsonWriterUtils.isFieldNeeded;
import static org.sonar.server.ws.JsonWriterUtils.writeIfNeeded;

public class CustomMeasureJsonWriter {
  private static final String FIELD_ID = "id";
  private static final String FIELD_PROJECT_ID = "projectId";
  private static final String FIELD_PROJECT_KEY = "projectKey";
  private static final String FIELD_VALUE = "value";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_METRIC = "metric";
  private static final String FIELD_CREATED_AT = "createdAt";
  private static final String FIELD_UPDATED_AT = "updatedAt";
  private static final String FIELD_USER = "user";
  private static final String FIELD_PENDING = "pending";

  public static final Set<String> OPTIONAL_FIELDS = ImmutableSet.of(FIELD_PROJECT_ID, FIELD_PROJECT_KEY, FIELD_VALUE, FIELD_DESCRIPTION, FIELD_METRIC, FIELD_CREATED_AT,
    FIELD_UPDATED_AT, FIELD_USER, FIELD_PENDING);

  private final UserJsonWriter userJsonWriter;

  public CustomMeasureJsonWriter(UserJsonWriter userJsonWriter) {
    this.userJsonWriter = userJsonWriter;
  }

  public void write(JsonWriter json, CustomMeasureDto measure, MetricDto metric, ComponentDto component, UserDto user, boolean isPending,
    @Nullable Collection<String> fieldsToReturn) {
    json.beginObject();
    json.prop(FIELD_ID, String.valueOf(measure.getId()));
    writeIfNeeded(json, measureValue(measure, metric), FIELD_VALUE, fieldsToReturn);
    writeIfNeeded(json, measure.getDescription(), FIELD_DESCRIPTION, fieldsToReturn);
    if (isFieldNeeded(FIELD_METRIC, fieldsToReturn)) {
      json.name(FIELD_METRIC);
      MetricJsonWriter.write(json, metric, MetricJsonWriter.MANDATORY_FIELDS);
    }
    writeIfNeeded(json, component.uuid(), FIELD_PROJECT_ID, fieldsToReturn);
    writeIfNeeded(json, component.getDbKey(), FIELD_PROJECT_KEY, fieldsToReturn);
    writeIfNeeded(json, new Date(measure.getCreatedAt()), FIELD_CREATED_AT, fieldsToReturn);
    writeIfNeeded(json, new Date(measure.getUpdatedAt()), FIELD_UPDATED_AT, fieldsToReturn);
    writeIfNeeded(json, isPending, FIELD_PENDING, fieldsToReturn);

    if (isFieldNeeded(FIELD_USER, fieldsToReturn)) {
      json.name(FIELD_USER);
      userJsonWriter.write(json, user);
    }

    json.endObject();
  }

  private static String measureValue(CustomMeasureDto measure, MetricDto metric) {
    Metric.ValueType metricType = Metric.ValueType.valueOf(metric.getValueType());
    Double doubleValue = measure.getValue();
    String stringValue = measure.getTextValue();

    switch (metricType) {
      case BOOL:
        return doubleValue == 1.0d ? "true" : "false";
      case INT:
      case MILLISEC:
        return String.valueOf(doubleValue.intValue());
      case WORK_DUR:
        return String.valueOf(doubleValue.longValue());
      case FLOAT:
      case PERCENT:
      case RATING:
        return String.valueOf(doubleValue);
      case LEVEL:
      case STRING:
      case DATA:
      case DISTRIB:
        return stringValue;
      default:
        throw new IllegalArgumentException("Unsupported metric type: " + metricType.name());
    }
  }

}
