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
package org.sonar.server.metric.ws;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.metric.MetricDto;

import static org.sonar.server.ws.JsonWriterUtils.writeIfNeeded;

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
  public static final String FIELD_CUSTOM = "custom";
  public static final String FIELD_DECIMAL_SCALE = "decimalScale";
  public static final Set<String> OPTIONAL_FIELDS = ImmutableSet.of(FIELD_NAME, FIELD_DESCRIPTION, FIELD_DOMAIN,
    FIELD_DIRECTION, FIELD_QUALITATIVE, FIELD_HIDDEN, FIELD_CUSTOM, FIELD_DECIMAL_SCALE);
  public static final Set<String> MANDATORY_FIELDS = ImmutableSet.of(FIELD_ID, FIELD_KEY, FIELD_NAME, FIELD_DOMAIN, FIELD_TYPE);
  public static final Set<String> ALL_FIELDS = ImmutableSet.copyOf(Sets.union(MANDATORY_FIELDS, OPTIONAL_FIELDS));

  private MetricJsonWriter() {
    // static stuff only for the time being
  }

  public static void write(JsonWriter json, List<MetricDto> metrics, Set<String> fieldsToReturn) {
    json.name("metrics");
    json.beginArray();
    for (MetricDto metric : metrics) {
      write(json, metric, fieldsToReturn);
    }
    json.endArray();
  }

  public static void write(JsonWriter json, MetricDto metric, Set<String> fieldsToReturn) {
    json.beginObject();
    json.prop(FIELD_ID, String.valueOf(metric.getId()));
    json.prop(FIELD_KEY, metric.getKey());
    json.prop(FIELD_TYPE, metric.getValueType());
    writeIfNeeded(json, metric.getShortName(), FIELD_NAME, fieldsToReturn);
    writeIfNeeded(json, metric.getDescription(), FIELD_DESCRIPTION, fieldsToReturn);
    writeIfNeeded(json, metric.getDomain(), FIELD_DOMAIN, fieldsToReturn);
    writeIfNeeded(json, metric.getDirection(), FIELD_DIRECTION, fieldsToReturn);
    writeIfNeeded(json, metric.isQualitative(), FIELD_QUALITATIVE, fieldsToReturn);
    writeIfNeeded(json, metric.isHidden(), FIELD_HIDDEN, fieldsToReturn);
    writeIfNeeded(json, metric.isUserManaged(), FIELD_CUSTOM, fieldsToReturn);
    writeIfNeeded(json, metric.getDecimalScale(), FIELD_DECIMAL_SCALE, fieldsToReturn);
    json.endObject();
  }
}
