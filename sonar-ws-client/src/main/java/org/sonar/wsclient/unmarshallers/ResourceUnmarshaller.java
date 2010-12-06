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
package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

import java.util.ArrayList;
import java.util.List;

public class ResourceUnmarshaller extends AbstractUnmarshaller<Resource> {

  @Override
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
        .setDescription(JsonUtils.getString(json, "description"))
        .setDate(JsonUtils.getDateTime(json, "date"))
        .setVersion(JsonUtils.getString(json, "version"));
  }

  private void parseMeasures(JSONObject json, Resource resource) {
    JSONArray measuresJson = (JSONArray) json.get("msr");
    if (measuresJson != null) {
      resource.setMeasures(parseMeasures(measuresJson));
    }
  }

  private List<Measure> parseMeasures(JSONArray measuresJson) {
    List<Measure> projectMeasures = new ArrayList<Measure>();
    int len = measuresJson.size();
    for (int i = 0; i < len; i++) {
      JSONObject measureJson = (JSONObject) measuresJson.get(i);
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
        .setTrend(JsonUtils.getInteger(json, "trend"))
        .setVar(JsonUtils.getInteger(json, "var"))
        .setData(JsonUtils.getString(json, "data"))
        .setRuleKey(JsonUtils.getString(json, "rule_key"))
        .setRuleName(JsonUtils.getString(json, "rule_name"))
        .setRuleCategory(JsonUtils.getString(json, "rule_category"))
        .setRuleSeverity(JsonUtils.getString(json, "rule_priority"))
        .setCharacteristicKey(JsonUtils.getString(json, "ctic_key"))
        .setCharacteristicName(JsonUtils.getString(json, "ctic_name"));
    return measure;
  }
}
