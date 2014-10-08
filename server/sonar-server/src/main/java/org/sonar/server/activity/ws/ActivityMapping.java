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
package org.sonar.server.activity.ws;

import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.activity.Activity;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityNormalizer;
import org.sonar.server.search.ws.BaseMapping;
import org.sonar.server.search.ws.SearchOptions;

import java.util.Map;

/**
 * Conversion between {@link org.sonar.server.activity.index.ActivityDoc} and WS JSON response
 */
public class ActivityMapping extends BaseMapping<ActivityDoc, Object> {

  public ActivityMapping() {
    map("type", ActivityNormalizer.LogFields.TYPE.field());
    map("action", ActivityNormalizer.LogFields.ACTION.field());
    mapDateTime("createdAt", ActivityNormalizer.LogFields.CREATED_AT.field());
    map("login", ActivityNormalizer.LogFields.LOGIN.field());
    map("message", ActivityNormalizer.LogFields.MESSAGE.field());
    map("details", new IndexMapper<ActivityDoc, Object>(ActivityNormalizer.LogFields.DETAILS.field()) {
      @Override
      public void write(JsonWriter json, ActivityDoc activity, Object context) {
        json.name("details").beginObject();
        for (Map.Entry<String, String> detail : activity.details().entrySet()) {
          json.prop(detail.getKey(), detail.getValue());
        }
        json.endObject();
      }
    });
  }

  public void write(Activity activity, JsonWriter writer, SearchOptions options) {
    doWrite((ActivityDoc)activity, null, writer, options);
  }

}
