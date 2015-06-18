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

package org.sonar.server.measure.custom.ws;

import org.sonar.api.measures.Metric;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.custom.db.CustomMeasureDto;
import org.sonar.core.metric.db.MetricDto;

import static org.sonar.server.measure.custom.ws.CreateAction.PARAM_DESCRIPTION;
import static org.sonar.server.measure.custom.ws.CreateAction.PARAM_PROJECT_ID;
import static org.sonar.server.measure.custom.ws.CreateAction.PARAM_PROJECT_KEY;
import static org.sonar.server.measure.custom.ws.CreateAction.PARAM_VALUE;

public class CustomMeasureJsonWriter {
  private static final String FIELD_ID = "id";
  private static final String FIELD_PROJECT_ID = PARAM_PROJECT_ID;
  private static final String FIELD_PROJECT_KEY = PARAM_PROJECT_KEY;
  private static final String FIELD_VALUE = PARAM_VALUE;
  private static final String FIELD_DESCRIPTION = PARAM_DESCRIPTION;
  private static final String FIELD_METRIC = "metric";
  private static final String FIELD_METRIC_KEY = "key";
  private static final String FIELD_METRIC_ID = "id";
  private static final String FIELD_METRIC_TYPE = "type";

  public void write(JsonWriter json, CustomMeasureDto measure, MetricDto metric, ComponentDto component) {
    json.beginObject();
    json.prop(FIELD_ID, String.valueOf(measure.getId()));
    json.name(FIELD_METRIC);
    writeMetric(json, metric);
    json.prop(FIELD_PROJECT_ID, component.uuid());
    json.prop(FIELD_PROJECT_KEY, component.key());
    json.prop(FIELD_DESCRIPTION, measure.getDescription());
    json.prop(FIELD_VALUE, measureValue(measure, metric));
    json.endObject();
  }

  private String measureValue(CustomMeasureDto measure, MetricDto metric) {
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
        throw new IllegalArgumentException("Unsupported metric type:" + metricType.description());
    }
  }

  private static void writeMetric(JsonWriter json, MetricDto metric) {
    json.beginObject();
    json.prop(FIELD_METRIC_ID, String.valueOf(metric.getId()));
    json.prop(FIELD_METRIC_KEY, metric.getKey());
    json.prop(FIELD_METRIC_TYPE, metric.getValueType());
    json.endObject();
  }

}
