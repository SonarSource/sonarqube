/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.pushapi.qualityprofile;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sonar.core.util.ParamChange;
import org.sonar.core.util.rule.RuleChange;
import org.sonar.core.util.rule.RuleSetChangedEvent;
import org.sonar.server.pushapi.sonarlint.SonarLintClient;

import static java.util.Arrays.asList;

public class RuleSetChangeBroadcastUtils {
  private RuleSetChangeBroadcastUtils() {
  }

  public static Predicate<SonarLintClient> getFilterForEvent(RuleSetChangedEvent ruleSetChangedEvent) {
    List<String> affectedProjects = asList(ruleSetChangedEvent.getProjects());
    return client -> {
      Set<String> clientProjectKeys = client.getClientProjectKeys();
      Set<String> languages = client.getLanguages();
      return !Collections.disjoint(clientProjectKeys, affectedProjects) && languages.contains(ruleSetChangedEvent.getLanguage());
    };
  }

  public static String getMessage(RuleSetChangedEvent ruleSetChangedEvent) {
    return "event: " + ruleSetChangedEvent.getEvent() + "\n"
      + "data: " + toJson(ruleSetChangedEvent);
  }

  private static String toJson(RuleSetChangedEvent ruleSetChangedEvent) {
    JSONObject data = new JSONObject();
    data.put("projects", ruleSetChangedEvent.getProjects());

    JSONArray activatedRulesJson = new JSONArray();
    for (RuleChange rule : ruleSetChangedEvent.getActivatedRules()) {
      activatedRulesJson.put(toJson(rule));
    }
    data.put("activatedRules", activatedRulesJson);

    JSONArray deactivatedRulesJson = new JSONArray();
    for (String ruleKey : ruleSetChangedEvent.getDeactivatedRules()) {
      deactivatedRulesJson.put(ruleKey);
    }
    data.put("deactivatedRules", deactivatedRulesJson);

    return data.toString();
  }

  private static JSONObject toJson(RuleChange rule) {
    JSONObject ruleJson = new JSONObject();
    ruleJson.put("key", rule.getKey());
    ruleJson.put("language", rule.getLanguage());
    ruleJson.put("severity", rule.getSeverity());
    ruleJson.put("templateKey", rule.getTemplateKey());

    JSONArray params = new JSONArray();
    for (ParamChange paramChange : rule.getParams()) {
      params.put(toJson(paramChange));
    }
    ruleJson.put("params", params);
    return ruleJson;
  }

  private static JSONObject toJson(ParamChange paramChange) {
    JSONObject param = new JSONObject();
    param.put("key", paramChange.getKey());
    param.put("value", paramChange.getValue());
    return param;
  }

}
