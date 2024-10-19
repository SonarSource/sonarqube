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
package org.sonar.server.metric.ws;

import java.util.List;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.metric.MetricDto;

public class MetricJsonWriter {
  public static final String FIELD_ID = "id";
  public static final String FIELD_KEY = "key";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_DESCRIPTION = "description";
  public static final String FIELD_DOMAIN = "domain";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_DIRECTION = "direction";
  public static final String FIELD_QUALITATIVE = "qualitative";
  public static final String FIELD_HIDDEN = "hidden";
  public static final String FIELD_DECIMAL_SCALE = "decimalScale";

  private MetricJsonWriter() {
    // static stuff only for the time being
  }

  public static void write(JsonWriter json, List<MetricDto> metrics) {
    json.name("metrics");
    json.beginArray();
    for (MetricDto metric : metrics) {
      write(json, metric);
    }
    json.endArray();
  }

  public static void write(JsonWriter json, MetricDto metric) {
    json.beginObject();
    json.prop(FIELD_ID, String.valueOf(metric.getUuid()));
    json.prop(FIELD_KEY, metric.getKey());
    json.prop(FIELD_TYPE, metric.getValueType());
    json.prop(FIELD_NAME, metric.getShortName());
    json.prop(FIELD_DESCRIPTION, metric.getDescription());
    json.prop(FIELD_DOMAIN, metric.getDomain());
    json.prop(FIELD_DIRECTION, metric.getDirection());
    json.prop(FIELD_QUALITATIVE, metric.isQualitative());
    json.prop(FIELD_HIDDEN, metric.isHidden());
    json.prop(FIELD_DECIMAL_SCALE, metric.getDecimalScale());
    json.endObject();
  }
}
