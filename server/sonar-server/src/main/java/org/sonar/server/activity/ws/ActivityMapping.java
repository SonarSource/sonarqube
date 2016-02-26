/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.activity.ws;

import java.util.Map;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityIndexDefinition;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.ws.BaseMapping;

/**
 * Conversion between {@link org.sonar.server.activity.index.ActivityDoc} and WS JSON response
 */
public class ActivityMapping extends BaseMapping<ActivityDoc, Object> {

  public ActivityMapping() {
    super();
    map("type", ActivityIndexDefinition.FIELD_TYPE);
    map("action", ActivityIndexDefinition.FIELD_ACTION);
    mapDateTime("createdAt", ActivityIndexDefinition.FIELD_CREATED_AT);
    map("login", ActivityIndexDefinition.FIELD_LOGIN);
    map("message", ActivityIndexDefinition.FIELD_MESSAGE);
    map("details", new IndexMapper<ActivityDoc, Object>(ActivityIndexDefinition.FIELD_DETAILS) {
      @Override
      public void write(JsonWriter json, ActivityDoc activity, Object context) {
        json.name("details").beginObject();
        for (Map.Entry<String, String> detail : activity.getDetails().entrySet()) {
          json.prop(detail.getKey(), detail.getValue());
        }
        json.endObject();
      }
    });
  }

  public void write(ActivityDoc activity, JsonWriter writer, QueryContext context) {
    doWrite(activity, null, writer, context);
  }

}
