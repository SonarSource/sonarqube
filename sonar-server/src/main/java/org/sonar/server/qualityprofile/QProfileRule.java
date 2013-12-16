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
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.api.rules.ActiveRule;
import org.sonar.check.Cardinality;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class QProfileRule {

  private final Map<String, Object> ruleSource;
  private final Map<String, Object> activeRuleSource;

  private final Integer id;
  private final String key;
  private final String repositoryKey;
  private final String name;
  private final String description;
  private final String status;
  private final String cardinality;
  private final String parentKey;
  private final Date createdAt;
  private final Date updatedAt;

  private final int activeRuleId;
  private final String severity;
  private final String inheritance;
  private final List<Param> params;

  public QProfileRule(Map<String, Object> ruleSource, Map<String, Object> activeRuleSource) {
    this.ruleSource = ruleSource;
    this.activeRuleSource = activeRuleSource;

    id = (Integer) ruleSource.get("id");
    key = (String) ruleSource.get("key");
    repositoryKey = (String) ruleSource.get("repositoryKey");
    name = (String) ruleSource.get("name");
    description = (String) ruleSource.get("description");
    status = (String) ruleSource.get("status");
    cardinality = (String) ruleSource.get("cardinality");
    parentKey = (String) ruleSource.get("parentKey");
    createdAt = parseOptionalDate("createdAt", ruleSource);
    updatedAt = parseOptionalDate("updatedAt", ruleSource);

    activeRuleId = (Integer) activeRuleSource.get("id");
    severity = (String) activeRuleSource.get("severity");
    inheritance = (String) activeRuleSource.get("inheritance");
    params = Lists.newArrayList();
    if (ruleSource.containsKey("params")) {
      Map<String, Map<String, Object>> ruleParams = Maps.newHashMap();
      for (Map<String, Object> ruleParam: (List<Map<String, Object>>) ruleSource.get("params")) {
        ruleParams.put((String) ruleParam.get("key"), ruleParam);
      }
      Map<String, Map<String, Object>> activeRuleParams = Maps.newHashMap();
      if (activeRuleSource.containsKey("params")) {
        for (Map<String, Object> activeRuleParam: (List<Map<String, Object>>) activeRuleSource.get("params")) {
          activeRuleParams.put((String) activeRuleParam.get("key"), activeRuleParam);
        }
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

  protected Date parseOptionalDate(String field, Map<String, Object> ruleSource) {
    String dateValue = (String) ruleSource.get(field);
    if (dateValue == null) {
      return null;
    } else {
      return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(dateValue).toDate();
    }
  }

  public Map<String, Object> ruleSource() {
    return ruleSource;
  }

  public Map<String, Object> activeRuleSource() {
    return activeRuleSource;
  }

  public int id() {
    return id;
  }

  public int activeRuleId() {
    return activeRuleId;
  }

  public String key() {
    return key;
  }

  public String repositoryKey() {
    return repositoryKey;
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

  public Date createdAt() {
    return createdAt;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public String severity() {
    return severity;
  }

  public String inheritance() {
    return inheritance;
  }

  public boolean isInherited() {
    return ActiveRule.INHERITED.equals(inheritance);
  }

  public boolean isOverrides() {
    return ActiveRule.OVERRIDES.equals(inheritance);
  }

  public boolean isTemplate() {
    return Cardinality.MULTIPLE.toString().equals(cardinality);
  }

  public boolean isEditable() {
    return parentKey != null;
  }

  public List<Param> params() {
    return params;
  }

  public String note() {
    return null;
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
