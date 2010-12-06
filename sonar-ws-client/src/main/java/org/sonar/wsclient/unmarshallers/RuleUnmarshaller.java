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
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleParam;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.5
 */
public class RuleUnmarshaller extends AbstractUnmarshaller<Rule> {

  @Override
  protected Rule parse(JSONObject json) {
    Rule rule = new Rule();
    parseRuleFields(json, rule);
    parseParams(json, rule);
    return rule;
  }

  private void parseRuleFields(JSONObject json, Rule rule) {
    rule.setTitle(JsonUtils.getString(json, "title"))
        .setKey(JsonUtils.getString(json, "key"))
        .setPlugin(JsonUtils.getString(json, "plugin"))
        .setDescription(JsonUtils.getString(json, "description"))
        .setSeverity(JsonUtils.getString(json, "priority"));
  }

  private void parseParams(JSONObject json, Rule rule) {
    JSONArray paramsJson = (JSONArray) json.get("params");
    if (paramsJson != null) {
      rule.setParams(parseParams(paramsJson));
    }
  }

  private List<RuleParam> parseParams(JSONArray paramsJson) {
    List<RuleParam> ruleParams = new ArrayList<RuleParam>();
    int len = paramsJson.size();
    for (int i = 0; i < len; i++) {
      JSONObject paramJson = (JSONObject) paramsJson.get(i);
      if (paramJson != null) {
        RuleParam param = parseParam(paramJson);
        ruleParams.add(param);
      }
    }
    return ruleParams;
  }

  private RuleParam parseParam(JSONObject json) {
    RuleParam param = new RuleParam();
    param.setName(JsonUtils.getString(json, "name"))
        .setDescription(JsonUtils.getString(json, "description"))
        .setValue(JsonUtils.getString(json, "value"));
    return param;
  }

}
