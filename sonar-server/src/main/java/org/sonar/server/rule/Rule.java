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
package org.sonar.server.rule;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.check.Cardinality;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Rule {

  private int id;
  private String key;
  private String language;
  private String name;
  private String description;
  private Integer templateId;
  private String repositoryKey;
  private String severity;
  private String status;
  private String cardinality;
  private Date createdAt;
  private Date updatedAt;
  private List<String> systemTags;
  private List<String> adminTags;
  private RuleNote ruleNote;
  private List<RuleParam> params;

  public Rule(Map<String, Object> ruleSource) {
    id = (Integer) ruleSource.get(RuleDocument.FIELD_ID);
    key = (String) ruleSource.get(RuleDocument.FIELD_KEY);
    language = (String) ruleSource.get(RuleDocument.FIELD_LANGUAGE);
    repositoryKey = (String) ruleSource.get(RuleDocument.FIELD_REPOSITORY_KEY);
    severity = (String) ruleSource.get(RuleDocument.FIELD_SEVERITY);
    name = (String) ruleSource.get(RuleDocument.FIELD_NAME);
    description = (String) ruleSource.get(RuleDocument.FIELD_DESCRIPTION);
    status = (String) ruleSource.get(RuleDocument.FIELD_STATUS);
    cardinality = (String) ruleSource.get("cardinality");
    templateId = (Integer) ruleSource.get(RuleDocument.FIELD_TEMPLATE_ID);
    createdAt = parseOptionalDate(RuleDocument.FIELD_CREATED_AT, ruleSource);
    updatedAt = parseOptionalDate(RuleDocument.FIELD_UPDATED_AT, ruleSource);

    if (ruleSource.containsKey(RuleDocument.FIELD_NOTE)) {
      Map<String, Object> ruleNoteDocument = (Map<String, Object>) ruleSource.get(RuleDocument.FIELD_NOTE);
      ruleNote = new RuleNote(
        (String) ruleNoteDocument.get(RuleDocument.FIELD_NOTE_DATA),
        (String) ruleNoteDocument.get(RuleDocument.FIELD_NOTE_USER_LOGIN),
        parseOptionalDate(RuleDocument.FIELD_NOTE_CREATED_AT, ruleNoteDocument),
        parseOptionalDate(RuleDocument.FIELD_NOTE_UPDATED_AT, ruleNoteDocument)
      );
    } else {
      ruleNote = null;
    }

    params = Lists.newArrayList();
    if (ruleSource.containsKey(RuleDocument.FIELD_PARAMS)) {
      Map<String, Map<String, Object>> ruleParams = Maps.newHashMap();
      for (Map<String, Object> ruleParam: (List<Map<String, Object>>) ruleSource.get(RuleDocument.FIELD_PARAMS)) {
        ruleParams.put((String) ruleParam.get(RuleDocument.FIELD_PARAM_KEY), ruleParam);
      }
      for(Map.Entry<String, Map<String, Object>> ruleParam: ruleParams.entrySet()) {
        RuleParamType type = RuleParamType.parse((String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_TYPE));
        params.add(new RuleParam(
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_KEY),
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_DESCRIPTION),
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_DEFAULT_VALUE),
          type
        ));
      }
    }

    systemTags = Lists.newArrayList();
    if (ruleSource.containsKey(RuleDocument.FIELD_SYSTEM_TAGS)) {
      for (String tag: (List<String>) ruleSource.get(RuleDocument.FIELD_SYSTEM_TAGS)) {
        systemTags.add(tag);
      }
    }
    adminTags = Lists.newArrayList();
    if (ruleSource.containsKey(RuleDocument.FIELD_ADMIN_TAGS)) {
      for (String tag: (List<String>) ruleSource.get(RuleDocument.FIELD_ADMIN_TAGS)) {
        adminTags.add(tag);
      }
    }
  }

  public int id() {
    return id;
  }

  public String key() {
    return key;
  }

  public String repositoryKey() {
    return repositoryKey;
  }

  public String language() {
    return language;
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

  @CheckForNull
  public Integer templateId() {
    return templateId;
  }

  public boolean isTemplate() {
    return Cardinality.MULTIPLE.toString().equals(cardinality);
  }

  public boolean isEditable() {
    return templateId != null;
  }

  public String severity() {
    return severity;
  }

  public static Date parseOptionalDate(String field, Map<String, Object> ruleSource) {
    String dateValue = (String) ruleSource.get(field);
    if (dateValue == null) {
      return null;
    } else {
      return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(dateValue).toDate();
    }
  }

  @CheckForNull
  public RuleNote ruleNote() {
    return ruleNote;
  }

  public List<String> systemTags() {
    return systemTags;
  }

  public List<String> adminTags() {
    return adminTags;
  }

  public List<? extends RuleParam> params() {
    return params;
  }

  public RuleKey ruleKey() {
    return RuleKey.of(repositoryKey, key);
  }
}
