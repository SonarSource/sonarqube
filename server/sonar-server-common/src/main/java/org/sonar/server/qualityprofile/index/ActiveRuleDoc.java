/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_PRIORITIZED_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;

public class ActiveRuleDoc extends BaseDoc {

  public static final String DOC_ID_PREFIX = "ar_";

  public ActiveRuleDoc(String uuid) {
    super(TYPE_ACTIVE_RULE, Maps.newHashMapWithExpectedSize(10));
    setField(FIELD_ACTIVE_RULE_UUID, uuid);
  }

  public ActiveRuleDoc(Map<String, Object> source) {
    super(TYPE_ACTIVE_RULE, source);
  }

  public static String docIdOf(String activeRuleUuid) {
    return DOC_ID_PREFIX + activeRuleUuid;
  }

  public static String activeRuleUuidOf(String docId) {
    if (docId.startsWith(DOC_ID_PREFIX)) {
      return docId.substring(DOC_ID_PREFIX.length());
    }
    // support for old active rule docId
    return docId;
  }

  @Override
  public String getId() {
    return docIdOf(getField(FIELD_ACTIVE_RULE_UUID));
  }

  ActiveRuleDoc setRuleUuid(String ruleUuid) {
    setParent(ruleUuid);
    setField(FIELD_RULE_UUID, ruleUuid);
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

  ActiveRuleDoc setPrioritizedRule(@Nullable Boolean b) {
    Boolean notNull = Optional.ofNullable(b).orElse(Boolean.FALSE);
    setField(FIELD_PRIORITIZED_RULE, notNull);
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
