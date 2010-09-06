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
package org.sonar.wsclient.gwt.unmarshallers;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.sonar.gwt.JsonUtils;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

import java.util.ArrayList;
import java.util.List;

public class ResourceUnmarshaller extends AbstractUnmarshaller<Resource> {

  private static final ResourceUnmarshaller INSTANCE = new ResourceUnmarshaller();

  private ResourceUnmarshaller() {
  }

  public static ResourceUnmarshaller getInstance() {
    return INSTANCE;
  }

  protected Resource parse(JSONObject json) {
    Resource resource = new Resource();
    parseResourceFields(json, resource);
    parseMeasures(json, resource);
    return resource;
  }

  private void parseResourceFields(JSONObject json, Resource resource) {
    resource.setId(JsonUtils.getInteger(json, "id"))
        .setKey(JsonUtils.getString(json, "key"))
        .setName(JsonUtils.getString(json, "name"))
        .setLongName(JsonUtils.getString(json, "lname"))
        .setScope(JsonUtils.getString(json, "scope"))
        .setQualifier(JsonUtils.getString(json, "qualifier"))
        .setLanguage(JsonUtils.getString(json, "lang"))
        .setVersion(JsonUtils.getString(json, "version"))
        .setCopy(JsonUtils.getInteger(json, "copy"));
  }

  private void parseMeasures(JSONObject json, Resource resource) {
    JSONValue measuresJson = json.get("msr");
    if (measuresJson != null) {
      resource.setMeasures(parseMeasures(measuresJson));
    }
  }

  private List<Measure> parseMeasures(JSONValue measuresJson) {
    List<Measure> projectMeasures = new ArrayList<Measure>();
    int len = JsonUtils.getArraySize(measuresJson);
    for (int i = 0; i < len; i++) {
      JSONObject measureJson = JsonUtils.getArray(measuresJson, i);
      if (measureJson != null) {
        Measure measure = parseMeasure(measureJson);
        projectMeasures.add(measure);
      }
    }
    return projectMeasures;
  }

  private Measure parseMeasure(JSONObject json) {
    Measure measure = new Measure();
    measure
        .setMetricKey(JsonUtils.getString(json, "key"))
        .setMetricName(JsonUtils.getString(json, "name"))
        .setValue(JsonUtils.getDouble(json, "val"))
        .setFormattedValue(JsonUtils.getString(json, "frmt_val"))
        .setData(JsonUtils.getString(json, "data"))
        .setTrend(JsonUtils.getInteger(json, "trend"))
        .setVar(JsonUtils.getInteger(json, "var"))
        .setRuleKey(JsonUtils.getString(json, "rule_key"))
        .setRuleName(JsonUtils.getString(json, "rule_name"))
        .setRuleCategory(JsonUtils.getString(json, "rule_category"))
        .setRulePriority(JsonUtils.getString(json, "rule_priority"))
        .setCharacteristicKey(JsonUtils.getString(json, "ctic_key"))
        .setCharacteristicName(JsonUtils.getString(json, "ctic_name"));
    return measure;
  }
}
