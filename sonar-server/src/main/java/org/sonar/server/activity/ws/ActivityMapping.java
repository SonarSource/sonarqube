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

import org.sonar.api.resources.Languages;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.activity.Activity;
import org.sonar.server.activity.index.ActivityNormalizer;
import org.sonar.server.search.ws.BaseMapping;
import org.sonar.server.text.MacroInterpreter;

import java.util.Map;

/**
 * Conversion between Log and WS JSON response
 */
public class ActivityMapping extends BaseMapping {


  public ActivityMapping(Languages languages, MacroInterpreter macroInterpreter) {
    super();
    addIndexStringField("key", ActivityNormalizer.LogFields.KEY.field());
    addIndexStringField("type", ActivityNormalizer.LogFields.TYPE.field());
    addIndexDatetimeField("createdAt", ActivityNormalizer.LogFields.DATE.field());
    addIndexStringField("userLogin", ActivityNormalizer.LogFields.AUTHOR.field());
    addIndexStringField("message", ActivityNormalizer.LogFields.MESSAGE.field());
    addIndexStringField("executionTime", ActivityNormalizer.LogFields.EXECUTION.field());
    addField("details", new DetailField());
  }

  private static class DetailField extends IndexField<Activity> {
    DetailField() {
      super(ActivityNormalizer.LogFields.DETAILS.field());
    }

    @Override
    public void write(JsonWriter json, Activity activity) {
      json.name("details").beginObject();
      for (Map.Entry<String, String> detail : activity.details().entrySet()) {
        json.prop(detail.getKey(), detail.getValue());
      }
      json.endObject();
    }
  }
}
