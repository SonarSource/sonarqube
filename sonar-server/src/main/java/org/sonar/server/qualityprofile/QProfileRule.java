/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.qualityprofile;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.sonar.api.rules.RulePriority;

import java.util.List;
import java.util.Map;

public class QProfileRule {

  private final Map<String, Object> ruleSource;
  private final Map<String, Object> activeRuleSource;

  private final String key;
  private final String name;
  private final String description;
  private final String status;

  private final RulePriority severity;
  private final List<Param> params;

  public QProfileRule(Map<String, Object> ruleSource, Map<String, Object> activeRuleSource) {
    this.ruleSource = ruleSource;
    this.activeRuleSource = activeRuleSource;

    key = (String) ruleSource.get("key");
    name = (String) ruleSource.get("name");
    description = (String) ruleSource.get("description");
    status = (String) ruleSource.get("status");

    severity = RulePriority.valueOf((String) activeRuleSource.get("severity"));
    params = Lists.newArrayList();
    if (ruleSource.containsKey("params")) {
      Map<String, Map<String, Object>> ruleParams = Maps.newHashMap();
      for (Map<String, Object> ruleParam: (List<Map<String, Object>>) ruleSource.get("params")) {
        ruleParams.put((String) ruleParam.get("key"), ruleParam);
      }
      Map<String, Map<String, Object>> activeRuleParams = Maps.newHashMap();
      for (Map<String, Object> activeRuleParam: (List<Map<String, Object>>) activeRuleSource.get("params")) {
        activeRuleParams.put((String) activeRuleParam.get("key"), activeRuleParam);
      }
      for(Map.Entry<String, Map<String, Object>> ruleParam: ruleParams.entrySet()) {
        params.add(new Param(
          (String) ruleParam.getValue().get("key"),
          activeRuleParams.containsKey(ruleParam.getKey()) ? (String) activeRuleParams.get(ruleParam.getKey())
            .get("value") : null,
          (String) ruleParam.getValue().get("description"),
          (String) ruleParam.getValue().get("defaultValue"),
          (String) ruleParam.getValue().get("type")
        ));
      }
    }
  }

  public Map<String, Object> ruleSource() {
    return ruleSource;
  }

  public Map<String, Object> activeRuleSource() {
    return activeRuleSource;
  }

  public String key() {
    return key;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public String status() {
    return status;
  }

  public RulePriority severity() {
    return severity;
  }

  public List<Param> params() {
    return params;
  }

  class Param {
    private final String key;
    private final String value;
    private final String description;
    private final String defaultValue;
    private final String type;
    public Param(String key, String value, String description, String defaultValue, String type) {
      super();
      this.key = key;
      this.value = value;
      this.description = description;
      this.defaultValue = defaultValue;
      this.type = type;
    }
    public String key() {
      return key;
    }
    public String value() {
      return value;
    }
    public String description() {
      return description;
    }
    public String defaultValue() {
      return defaultValue;
    }
    public String type() {
      return type;
    }
  }

  @Override
  public String toString() {
    return ruleSource + " / " + activeRuleSource;
  }
}
