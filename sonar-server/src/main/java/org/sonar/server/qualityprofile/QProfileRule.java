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
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleParam;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

public class QProfileRule extends Rule {

  public static final String INHERITED = "INHERITED";
  public static final String OVERRIDES = "OVERRIDES";

  private final Integer activeRuleId;
  private final Integer activeRuleParentId;
  private final String severity;
  private final String inheritance;
  private final QProfileRuleNote activeRuleNote;
  private final List<QProfileRuleParam> params;

  public QProfileRule(Map<String, Object> ruleSource, Map<String, Object> activeRuleSource) {

    super(ruleSource);

    if (activeRuleSource.isEmpty()) {
      activeRuleId = null;
      severity = (String) ruleSource.get(ActiveRuleDocument.FIELD_SEVERITY);
      inheritance = null;
      activeRuleNote = null;
      activeRuleParentId = null;
    } else {
      activeRuleId = (Integer) activeRuleSource.get(ActiveRuleDocument.FIELD_ID);
      severity = (String) activeRuleSource.get(ActiveRuleDocument.FIELD_SEVERITY);
      inheritance = (String) activeRuleSource.get(ActiveRuleDocument.FIELD_INHERITANCE);
      activeRuleParentId = (Integer) activeRuleSource.get(ActiveRuleDocument.FIELD_ACTIVE_RULE_PARENT_ID);

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
    Map<String, String> paramValues = Maps.newHashMap();
    if (activeRuleSource.containsKey(ActiveRuleDocument.FIELD_PARAMS)) {
      for (Map<String, Object> activeRuleParam: (List<Map<String, Object>>) activeRuleSource.get(ActiveRuleDocument.FIELD_PARAMS)) {
        paramValues.put((String) activeRuleParam.get(ActiveRuleDocument.FIELD_PARAM_KEY), (String) activeRuleParam.get(ActiveRuleDocument.FIELD_PARAM_VALUE));
      }
    }
    for (RuleParam param: super.params()) {
      params.add(new QProfileRuleParam(param, paramValues.get(param.key())));
    }
  }

  public QProfileRule(Map<String, Object> ruleSource) {
    this(ruleSource, Maps.<String, Object> newHashMap());
  }

  @CheckForNull
  public Integer activeRuleId() {
    return activeRuleId;
  }

  @Override
  public String severity() {
    return severity;
  }

  @CheckForNull
  public String inheritance() {
    return inheritance;
  }

  @CheckForNull
  public Integer activeRuleParentId() {
    return activeRuleParentId;
  }

  public boolean isInherited() {
    return INHERITED.equals(inheritance);
  }

  public boolean isOverrides() {
    return OVERRIDES.equals(inheritance);
  }

  @Override
  public List<? extends QProfileRuleParam> params() {
    return params;
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
