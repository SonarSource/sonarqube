/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.server.es.BaseDoc;

import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_IMPACTS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_PRIORITIZED_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SOFTWARE_QUALITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;

public class ActiveRuleDoc extends BaseDoc {

  public static final String DOC_ID_PREFIX = "ar_";

  public ActiveRuleDoc(String uuid) {
    super(TYPE_ACTIVE_RULE, Maps.newHashMapWithExpectedSize(10));
    setField(FIELD_ACTIVE_RULE_UUID, uuid);
  }

  // Invoked dynamically
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

  public ActiveRuleDoc setImpacts(Map<SoftwareQuality, Severity> impacts) {
    List<Map<String, String>> convertedMap = impacts
      .entrySet()
      .stream()
      .map(entry -> Map.of(
        SUB_FIELD_SOFTWARE_QUALITY, entry.getKey().name(),
        SUB_FIELD_SEVERITY, entry.getValue().name()))
      .toList();
    setField(FIELD_ACTIVE_RULE_IMPACTS, convertedMap);
    return this;
  }

  public Map<SoftwareQuality, Severity> getImpacts() {
    Object objectValue = getField(FIELD_ACTIVE_RULE_IMPACTS);
    if (objectValue instanceof List<?> valueList) {
      return valueList.stream()
        .map(v -> (Map<String, String>) v)
        .collect(Collectors.toMap(v -> SoftwareQuality.valueOf(v.get(SUB_FIELD_SOFTWARE_QUALITY)), v -> Severity.valueOf(v.get(SUB_FIELD_SEVERITY))));
    } else {
      return Map.of();
    }
  }

  public ActiveRuleDoc setInheritance(@Nullable String s) {
    setField(FIELD_ACTIVE_RULE_INHERITANCE, s);
    return this;
  }
}
