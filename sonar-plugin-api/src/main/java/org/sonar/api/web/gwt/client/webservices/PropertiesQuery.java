/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.web.gwt.client.webservices;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.sonar.api.web.gwt.client.Utils;

import java.util.ArrayList;
import java.util.List;

public final class PropertiesQuery extends Query<Properties> {

  private String key;

  public PropertiesQuery() {
  }

  public PropertiesQuery(String key) {
    this.key = key;
  }

  @Override
  public String toString() {
    String url = Utils.getServerApiUrl() + "/properties";
    if (key != null) {
      url += "/" + key;
    }
    return url + "?";
  }

  @Override
  public void execute(QueryCallBack<Properties> callback) {
    JsonUtils.requestJson(this.toString(), new JSONHandlerDispatcher<Properties>(callback) {
      @Override
      public Properties parseResponse(JavaScriptObject obj) {
        return new Properties(parseProperties(obj));
      }

      private List<Property> parseProperties(JavaScriptObject obj) {
        JSONArray array = new JSONArray(obj);
        List<Property> properties = new ArrayList<Property>();
        for (int i = 0; i < array.size(); i++) {
          JSONObject jsonObject = array.get(i).isObject();
          if (jsonObject != null) {
            properties.add(parseProperty(jsonObject));
          }
        }
        return properties;
      }

      private Property parseProperty(JSONObject json) {
        return new Property(JsonUtils.getString(json, "key"), JsonUtils.getString(json, "value"));
      }
    });
  }
}
