/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.qualityprofile.ActiveRule;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_ORGANIZATION_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_CREATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_REPOSITORY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_UPDATED_AT;

public class ActiveRuleDoc extends BaseDoc {

  private final ActiveRuleKey key;

  public ActiveRuleDoc(ActiveRuleKey key) {
    super(Maps.newHashMapWithExpectedSize(9));
    checkNotNull(key, "ActiveRuleKey cannot be null");
    this.key = key;
    setField(FIELD_ACTIVE_RULE_KEY, key.toString());
    setField(FIELD_ACTIVE_RULE_PROFILE_KEY, key.qProfile());
    setField(FIELD_ACTIVE_RULE_RULE_KEY, key.ruleKey().toString());
    setField(FIELD_ACTIVE_RULE_REPOSITORY, key.ruleKey().repository());
  }

  @Override
  public String getId() {
    return key().toString();
  }

  @Override
  public String getRouting() {
    return null;
  }

  @Override
  public String getParent() {
    return key.ruleKey().toString();
  }

  public ActiveRuleKey key() {
    return key;
  }

  String organizationUuid() {
    return getField(FIELD_ACTIVE_RULE_ORGANIZATION_UUID);
  }

  public ActiveRuleDoc setOrganizationUuid(String s) {
    setField(FIELD_ACTIVE_RULE_ORGANIZATION_UUID, s);
    return this;
  }

  String severity() {
    return getNullableField(FIELD_ACTIVE_RULE_SEVERITY);
  }

  public ActiveRuleDoc setSeverity(@Nullable String s) {
    setField(FIELD_ACTIVE_RULE_SEVERITY, s);
    return this;
  }

  ActiveRule.Inheritance inheritance() {
    String inheritance = getNullableField(FIELD_ACTIVE_RULE_INHERITANCE);
    if (inheritance == null || inheritance.isEmpty() ||
      containsIgnoreCase(inheritance, "none")) {
      return ActiveRule.Inheritance.NONE;
    } else if (containsIgnoreCase(inheritance, "herit")) {
      return ActiveRule.Inheritance.INHERITED;
    } else if (containsIgnoreCase(inheritance, "over")) {
      return ActiveRule.Inheritance.OVERRIDES;
    } else {
      throw new IllegalStateException("Value \"" + inheritance + "\" is not valid for rule's inheritance");
    }
  }

  public ActiveRuleDoc setInheritance(@Nullable String s) {
    setField(FIELD_ACTIVE_RULE_INHERITANCE, s);
    return this;
  }

  long createdAt() {
    return (Long) getField(FIELD_ACTIVE_RULE_CREATED_AT);
  }

  public ActiveRuleDoc setCreatedAt(@Nullable Long l) {
    setField(FIELD_ACTIVE_RULE_CREATED_AT, l);
    return this;
  }

  long updatedAt() {
    return (Long) getField(FIELD_ACTIVE_RULE_UPDATED_AT);
  }

  public ActiveRuleDoc setUpdatedAt(@Nullable Long l) {
    setField(FIELD_ACTIVE_RULE_UPDATED_AT, l);
    return this;
  }
}
