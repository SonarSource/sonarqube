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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.sonar.check.Cardinality;
import org.sonar.server.rule.ActiveRuleDocument;
import org.sonar.server.rule.RuleDocument;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class QProfileRule {

  public static final String INHERITED = "INHERITED";
  public static final String OVERRIDES = "OVERRIDES";

  private final Integer id;
  private final Integer parentId;
  private final String key;
  private final String repositoryKey;
  private final String name;
  private final String description;
  private final String status;
  private final String cardinality;
  private final Integer ruleId;
  private final Date createdAt;
  private final Date updatedAt;
  private final QProfileRuleNote ruleNote;

  private final Integer activeRuleId;
  private final String severity;
  private final String inheritance;
  private final QProfileRuleNote activeRuleNote;
  private final List<QProfileRuleParam> params;

  public QProfileRule(Map<String, Object> ruleSource, Map<String, Object> activeRuleSource) {

    id = (Integer) ruleSource.get(RuleDocument.FIELD_ID);
    key = (String) ruleSource.get(RuleDocument.FIELD_KEY);
    repositoryKey = (String) ruleSource.get(RuleDocument.FIELD_REPOSITORY_KEY);
    name = (String) ruleSource.get(RuleDocument.FIELD_NAME);
    description = (String) ruleSource.get(RuleDocument.FIELD_DESCRIPTION);
    status = (String) ruleSource.get(RuleDocument.FIELD_STATUS);
    cardinality = (String) ruleSource.get("cardinality");
    ruleId = (Integer) ruleSource.get(RuleDocument.FIELD_PARENT_KEY);
    createdAt = parseOptionalDate(RuleDocument.FIELD_CREATED_AT, ruleSource);
    updatedAt = parseOptionalDate(RuleDocument.FIELD_UPDATED_AT, ruleSource);

    if (ruleSource.containsKey(RuleDocument.FIELD_NOTE)) {
      Map<String, Object> ruleNoteDocument = (Map<String, Object>) ruleSource.get(RuleDocument.FIELD_NOTE);
      ruleNote = new QProfileRuleNote(
        (String) ruleNoteDocument.get(RuleDocument.FIELD_NOTE_DATA),
        (String) ruleNoteDocument.get(RuleDocument.FIELD_NOTE_USER_LOGIN),
        parseOptionalDate(RuleDocument.FIELD_NOTE_CREATED_AT, ruleNoteDocument),
        parseOptionalDate(RuleDocument.FIELD_NOTE_UPDATED_AT, ruleNoteDocument)
      );
    } else {
      ruleNote = null;
    }

    if (activeRuleSource.isEmpty()) {
      activeRuleId = null;
      severity = (String) ruleSource.get(ActiveRuleDocument.FIELD_SEVERITY);
      inheritance = null;
      activeRuleNote = null;
      parentId = null;
    } else {
      activeRuleId = (Integer) activeRuleSource.get(ActiveRuleDocument.FIELD_ID);
      severity = (String) activeRuleSource.get(ActiveRuleDocument.FIELD_SEVERITY);
      inheritance = (String) activeRuleSource.get(ActiveRuleDocument.FIELD_INHERITANCE);
      parentId = (Integer) activeRuleSource.get(ActiveRuleDocument.FIELD_PARENT_ID);

      if (activeRuleSource.containsKey(ActiveRuleDocument.FIELD_NOTE)) {
        Map<String, Object> ruleNoteDocument = (Map<String, Object>) activeRuleSource.get(ActiveRuleDocument.FIELD_NOTE);
        activeRuleNote = new QProfileRuleNote(
          (String) ruleNoteDocument.get(ActiveRuleDocument.FIELD_NOTE_DATA),
          (String) ruleNoteDocument.get(ActiveRuleDocument.FIELD_NOTE_USER_LOGIN),
          parseOptionalDate(ActiveRuleDocument.FIELD_NOTE_CREATED_AT, ruleNoteDocument),
          parseOptionalDate(ActiveRuleDocument.FIELD_NOTE_UPDATED_AT, ruleNoteDocument)
        );
      } else {
        activeRuleNote = null;
      }
    }

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
        params.add(new QProfileRuleParam(
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_KEY),
          activeRuleParams.containsKey(ruleParam.getKey()) ? (String) activeRuleParams.get(ruleParam.getKey()).get(ActiveRuleDocument.FIELD_PARAM_VALUE) : null,
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_DESCRIPTION),
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_DEFAULT_VALUE),
          (String) ruleParam.getValue().get(RuleDocument.FIELD_PARAM_TYPE)
        ));
      }
    }
  }

  public QProfileRule(Map<String, Object> ruleSource) {
    this(ruleSource, Maps.<String, Object> newHashMap());
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

  @CheckForNull
  public Integer activeRuleId() {
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

  @CheckForNull
  public Integer ruleId() {
    return ruleId;
  }

  @CheckForNull
  public Integer parentId() {
    return parentId;
  }

  public boolean isInherited() {
    return INHERITED.equals(inheritance);
  }

  public boolean isOverrides() {
    return OVERRIDES.equals(inheritance);
  }

  public boolean isTemplate() {
    return Cardinality.MULTIPLE.toString().equals(cardinality);
  }

  public boolean isEditable() {
    return ruleId != null;
  }

  public List<QProfileRuleParam> params() {
    return params;
  }

  public QProfileRuleNote ruleNote() {
    return ruleNote;
  }

  @CheckForNull
  public QProfileRuleNote activeRuleNote() {
    return activeRuleNote;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this).toString();
  }

}
