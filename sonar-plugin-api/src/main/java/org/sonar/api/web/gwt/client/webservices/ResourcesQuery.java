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
import com.google.gwt.json.client.JSONValue;
import org.sonar.api.web.gwt.client.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class ResourcesQuery extends AbstractResourceQuery<Resources> {

  public final static int DEPTH_UNLIMITED = -1;

  private Integer depth;
  private Integer limit;
  private String scopes;
  private String qualifiers;
  private String metrics;
  private String rules;
  private String ruleCategories;
  private String rulePriorities;
  private boolean verbose = false;

  /**
   * Alias for build()
   */
  public static ResourcesQuery get(String resourceKey) {
    return new ResourcesQuery(resourceKey);
  }

  public static ResourcesQuery build(String resourceKey) {
    return new ResourcesQuery(resourceKey);
  }

  public static ResourcesQuery build() {
    return new ResourcesQuery(null);
  }

  private ResourcesQuery(String resourceKey) {
    super(resourceKey);
  }

  public ResourcesQuery setDepth(Integer depth) {
    this.depth = depth;
    return this;
  }

  public ResourcesQuery setRules(String s) {
    this.rules = s;
    return this;
  }

  public ResourcesQuery filterOnRules(boolean b) {
    return setRules(b ? "true" : "false");
  }

  public ResourcesQuery filterOnRulePriorities(boolean b) {
    return setRulePriorities(b ? "true" : "false");
  }

  public ResourcesQuery filterOnRuleCategories(boolean b) {
    return setRuleCategories(b ? "true" : "false");
  }

  public ResourcesQuery setRulePriorities(String s) {
    this.rulePriorities = s;
    return this;
  }

  public ResourcesQuery setRuleCategories(String s) {
    this.ruleCategories = s;
    return this;
  }

  public ResourcesQuery setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public ResourcesQuery setScopes(String scopes) {
    this.scopes = scopes;
    return this;
  }

  public ResourcesQuery setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  public ResourcesQuery setQualifiers(String qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public ResourcesQuery setMetrics(List<WSMetrics.Metric> metrics) {
    this.metrics = getMetricsWSRequest(metrics);
    return this;
  }

  public ResourcesQuery setMetric(WSMetrics.Metric m) {
    this.metrics = m.getKey();
    return this;
  }

  public ResourcesQuery setMetric(String metricKey) {
    this.metrics = metricKey;
    return this;
  }

  private String getMetricsWSRequest(List<WSMetrics.Metric> metrics) {
    StringBuilder metricsDelimByComma = new StringBuilder(64);
    for (WSMetrics.Metric metric : metrics) {
      metricsDelimByComma.append(metric.getKey()).append(",");
    }
    return metricsDelimByComma.substring(0, metricsDelimByComma.length() - 1);
  }

  @Override
  public String toString() {
    String url = Utils.getServerApiUrl() + "/resources?";
    if (getResourceKey() != null) {
      url += "resource=" + getResourceKey() + "&";
    }
    if (metrics != null) {
      url += "metrics=" + metrics + "&";
    }
    if (scopes != null) {
      url += "scopes=" + scopes + "&";
    }
    if (qualifiers != null) {
      url += "qualifiers=" + qualifiers + "&";
    }
    if (depth != null) {
      url += "depth=" + depth + "&";
    }
    if (limit != null) {
      url += "limit=" + limit + "&";
    }
    if (rules != null) {
      url += "rules=" + rules + "&";
    }
    if (ruleCategories != null) {
      url += "rule_categories=" + ruleCategories + "&";
    }
    if (rulePriorities != null) {
      url += "rule_priorities=" + rulePriorities + "&";
    }
    if (verbose) {
      url += "verbose=true&";
    }
    return url;
  }

  @Override
  public void execute(QueryCallBack<Resources> callback) {
    JsonUtils.requestJson(this.toString(), new JSONHandlerDispatcher<Resources>(callback) {
      @Override
      public Resources parseResponse(JavaScriptObject obj) {
        return new Resources(parseResources(obj));
      }
    });
  }

  public static List<Resource> parseResources(JavaScriptObject json) {
    JSONArray array = new JSONArray(json);
    List<Resource> resources = new ArrayList<Resource>();
    for (int i = 0; i < array.size(); i++) {
      JSONObject jsStock = array.get(i).isObject();
      if (jsStock != null) {
        resources.add(parseResource(jsStock));
      }
    }
    return resources;
  }

  private static Resource parseResource(JSONObject json) {
    Double id = JsonUtils.getDouble(json, "id");
    String key = JsonUtils.getString(json, "key");
    String name = JsonUtils.getString(json, "name");
    String longName = JsonUtils.getString(json, "lname");
    String qualifier = JsonUtils.getString(json, "qualifier");
    String language = JsonUtils.getString(json, "lang");
    String scope = JsonUtils.getString(json, "scope");
    Integer copy = JsonUtils.getInteger(json, "copy");
    Date date = JsonUtils.getDate(json, "date");

    List<Measure> measures = null;
    JSONValue measuresJson;
    if ((measuresJson = json.get("msr")) != null) {
      measures = parseMeasures(measuresJson, date);
    }

    final Resource resource = new Resource(id.intValue(), key, name, scope, qualifier, language, copy, measures);
    resource.setLongName(longName);
    return resource;
  }

  private static List<Measure> parseMeasures(JSONValue measures, Date date) {
    List<Measure> projectMeasures = new ArrayList<Measure>();
    int len = JsonUtils.getArraySize(measures);
    for (int i = 0; i < len; i++) {
      JSONObject measure = JsonUtils.getArray(measures, i);
      if (measure != null) {
        Measure measureEntry = parseMeasure(measure, date);
        if (measureEntry != null) {
          projectMeasures.add(measureEntry);
        }
      }
    }
    return projectMeasures;
  }

  private static Measure parseMeasure(JSONObject measure, Date date) {
    String metric = JsonUtils.getString(measure, "key");
    if (metric == null) {
      return null;
    }

    final Measure m = new Measure(metric, JsonUtils.getDouble(measure, "val"), JsonUtils.getString(measure, "frmt_val"));
    m.setData(JsonUtils.getString(measure, "data"));
    String metricName = JsonUtils.getString(measure, "name");
    if (metricName != null) {
      m.setMetricName(metricName);
    }

    m.setRuleKey(JsonUtils.getString(measure, "rule_key"));
    m.setRuleName(JsonUtils.getString(measure, "rule_name"));
    m.setRuleCategory(JsonUtils.getString(measure, "rule_category"));
    m.setRulePriority(JsonUtils.getString(measure, "rule_priority"));
    m.setDate(date);
    return m;
  }

}
