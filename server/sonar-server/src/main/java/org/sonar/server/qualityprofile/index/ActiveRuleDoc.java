/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.qualityprofile.index;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;

import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_ID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_RULE_ID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;

public class ActiveRuleDoc extends BaseDoc {

  public ActiveRuleDoc(long id) {
    super(Maps.newHashMapWithExpectedSize(10));
    setField(FIELD_ACTIVE_RULE_ID, String.valueOf(id));
  }

  public ActiveRuleDoc(Map<String, Object> source) {
    super(source);
  }

  @Override
  public String getId() {
    return getField(FIELD_ACTIVE_RULE_ID);
  }

  @Override
  public String getRouting() {
    return getRuleIdAsString();
  }

  @Override
  public String getParent() {
    return getRuleIdAsString();
  }

  private String getRuleIdAsString() {
    return getField(FIELD_ACTIVE_RULE_RULE_ID);
  }

  ActiveRuleDoc setRuleId(int ruleId) {
    setField(FIELD_ACTIVE_RULE_RULE_ID, String.valueOf(ruleId));
    return this;
  }

  String getSeverity() {
    return getNullableField(FIELD_ACTIVE_RULE_SEVERITY);
  }

  ActiveRuleDoc setSeverity(@Nullable String s) {
    setField(FIELD_ACTIVE_RULE_SEVERITY, s);
    return this;
  }

  String getRuleProfileUuid() {
    return getField(FIELD_ACTIVE_RULE_PROFILE_UUID);
  }

  ActiveRuleDoc setRuleProfileUuid(String s) {
    setField(FIELD_ACTIVE_RULE_PROFILE_UUID, s);
    return this;
  }

  ActiveRuleInheritance getInheritance() {
    String inheritance = getNullableField(FIELD_ACTIVE_RULE_INHERITANCE);
    if (inheritance == null || inheritance.isEmpty() ||
      containsIgnoreCase(inheritance, "none")) {
      return ActiveRuleInheritance.NONE;
    } else if (containsIgnoreCase(inheritance, "herit")) {
      return ActiveRuleInheritance.INHERITED;
    } else if (containsIgnoreCase(inheritance, "over")) {
      return ActiveRuleInheritance.OVERRIDES;
    } else {
      throw new IllegalStateException("Value \"" + inheritance + "\" is not valid for rule's inheritance");
    }
  }

  public ActiveRuleDoc setInheritance(@Nullable String s) {
    setField(FIELD_ACTIVE_RULE_INHERITANCE, s);
    return this;
  }
}
