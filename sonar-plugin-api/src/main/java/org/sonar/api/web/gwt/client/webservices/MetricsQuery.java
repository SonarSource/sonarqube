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

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.web.gwt.client.Utils;
import org.sonar.api.web.gwt.client.webservices.WSMetrics.MetricsList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

public final class MetricsQuery extends Query<MetricsList> {
  
  private Boolean userManaged;
  private List<WSMetrics.Metric.ValueType> excludedTypes = new ArrayList<WSMetrics.Metric.ValueType>();

  public static MetricsQuery get() {
    return new MetricsQuery();
  }

  private MetricsQuery() {
    super();
  }
  
  public Boolean isUserManaged() {
    return userManaged;
  }

  public MetricsQuery setUserManaged(Boolean userManaged) {
    this.userManaged = userManaged;
    return this;
  }
  
  public MetricsQuery excludeTypes(WSMetrics.Metric.ValueType... types) {
    for (WSMetrics.Metric.ValueType valueType : types) {
      excludedTypes.add(valueType);
    }
    return this;
  }

  @Override
  public String toString() {
    return Utils.getServerApiUrl() + "/metrics?";
  }

  @Override
  public void execute(QueryCallBack<MetricsList> callback) {
    JsonUtils.requestJson(this.toString(), new JSONHandlerDispatcher<MetricsList>(callback) {
      @Override
      public MetricsList parseResponse(JavaScriptObject obj) {
        return parseMetrics(obj);
      }
    });
  }

  private MetricsList parseMetrics(JavaScriptObject json) {
    JSONArray array = new JSONArray(json);
    MetricsList list = new MetricsList();
    for (int i = 0; i < array.size(); i++) {
      JSONObject jsStock = array.get(i).isObject();
      if (jsStock != null) {
        WSMetrics.Metric m = parseMetric(jsStock);
        boolean skip = (isUserManaged() != null && (!isUserManaged() && m.isUserManaged())) || excludedTypes.contains(m.getType());
        if (!skip) {
          list.getMetrics().add(m);
        }
      }
    }
    return list;
  }

  private WSMetrics.Metric parseMetric(JSONObject json) {
    String key = JsonUtils.getString(json, "key");
    String name = JsonUtils.getString(json, "name");
    String description = JsonUtils.getString(json, "description");
    String domain = JsonUtils.getString(json, "domain");
    String type = JsonUtils.getString(json, "val_type");
    boolean qualitative = JsonUtils.getBoolean(json, "qualitative");
    boolean userManaged = JsonUtils.getBoolean(json, "user_managed");
    Integer direction = JsonUtils.getInteger(json, "direction");
    return new WSMetrics.Metric(key, name, description, domain, qualitative, userManaged, direction, WSMetrics.Metric.ValueType.valueOf(type));
  }

}
