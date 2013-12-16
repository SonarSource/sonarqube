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
import org.sonar.server.rule.ActiveRuleDocument;
import org.sonar.server.rule.RuleDocument;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class QProfileRule {

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

    id = (Integer) ruleSource.get(RuleDocument.FIELD_ID);
    key = (String) ruleSource.get(RuleDocument.FIELD_KEY);
    repositoryKey = (String) ruleSource.get(RuleDocument.FIELD_REPOSITORY_KEY);
    name = (String) ruleSource.get(RuleDocument.FIELD_NAME);
    description = (String) ruleSource.get(RuleDocument.FIELD_DESCRIPTION);
    status = (String) ruleSource.get(RuleDocument.FIELD_STATUS);
    cardinality = (String) ruleSource.get("cardinality");
    parentKey = (String) ruleSource.get(RuleDocument.FIELD_PARENT_KEY);
    createdAt = parseOptionalDate(RuleDocument.FIELD_CREATED_AT, ruleSource);
    updatedAt = parseOptionalDate(RuleDocument.FIELD_UPDATED_AT, ruleSource);

    activeRuleId = (Integer) activeRuleSource.get(ActiveRuleDocument.FIELD_ID);
    severity = (String) activeRuleSource.get(ActiveRuleDocument.FIELD_SEVERITY);
    inheritance = (String) activeRuleSource.get(ActiveRuleDocument.FIELD_INHERITANCE);
    params = Lists.newArrayList();
    if (ruleSource.containsKey(RuleDocument.FIELD_PARAMS)) {
      Map<String, Map<String, Object>> ruleParams = Maps.newHashMap();
      for (Map<String, Object> ruleParam: (List<Map<String, Object>>) ruleSource.get(RuleDocument.FIELD_PARAMS)) {
        ruleParams.put((String) ruleParam.get(RuleDocument.FIELD_PARAM_KEY), ruleParam);
      }
      Map<String, Map<String, Object>> activeRuleParams = Maps.newHashMap();
      if (activeRuleSource.containsKey(ActiveRuleDocument.FIELD_PARAMS)) {
        for (Map<String, Object> activeRuleParam: (List<Map<String, Object>>) activeRuleSource.get(ActiveRuleDocument.FIELD_PARAMS)) {
          activeRuleParams.put((String) activeRuleParam.get(ActiveRuleDocument.FIELD_PARAM_KEY), activeRuleParam);
        }
      }
      for(Map.Entry<String, Map<String, Object>> ruleParam: ruleParams.entrySet()) {
        params.add(new Param(
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_KEY),
          activeRuleParams.containsKey(ruleParam.getKey()) ? (String) activeRuleParams.get(ruleParam.getKey())
            .get(ActiveRuleDocument.FIELD_PARAM_VALUE) : null,
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_DESCRIPTION),
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_DEFAULT_VALUE),
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_TYPE)
        ));
      }
    }
  }

  private static Date parseOptionalDate(String field, Map<String, Object> ruleSource) {
    String dateValue = (String) ruleSource.get(field);
    if (dateValue == null) {
      return null;
    } else {
      return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(dateValue).toDate();
    }
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

  @CheckForNull
  public Date updatedAt() {
    return updatedAt;
  }

  public String severity() {
    return severity;
  }

  @CheckForNull
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

  static class Param {
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
}
