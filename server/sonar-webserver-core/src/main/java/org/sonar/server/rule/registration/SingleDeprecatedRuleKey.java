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
package org.sonar.server.rule.registration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.db.rule.DeprecatedRuleKeyDto;

@Immutable
class SingleDeprecatedRuleKey {
  private String oldRuleKey;
  private String oldRepositoryKey;
  private String newRuleKey;
  private String newRepositoryKey;
  private String uuid;
  private String ruleUuid;

  /**
   * static methods {@link #from(RulesDefinition.Rule)} and {@link #from(DeprecatedRuleKeyDto)} must be used
   */
  private SingleDeprecatedRuleKey() {
    // empty
  }

  public static Set<SingleDeprecatedRuleKey> from(RulesDefinition.Rule rule) {
    rule.deprecatedRuleKeys();
    return rule.deprecatedRuleKeys().stream()
      .map(r -> new SingleDeprecatedRuleKey()
        .setNewRepositoryKey(rule.repository().key())
        .setNewRuleKey(rule.key())
        .setOldRepositoryKey(r.repository())
        .setOldRuleKey(r.rule()))
      .collect(Collectors.toSet());
  }

  public static SingleDeprecatedRuleKey from(DeprecatedRuleKeyDto rule) {
    return new SingleDeprecatedRuleKey()
      .setUuid(rule.getUuid())
      .setRuleUuid(rule.getRuleUuid())
      .setNewRepositoryKey(rule.getNewRepositoryKey())
      .setNewRuleKey(rule.getNewRuleKey())
      .setOldRepositoryKey(rule.getOldRepositoryKey())
      .setOldRuleKey(rule.getOldRuleKey());
  }

  public String getOldRuleKey() {
    return oldRuleKey;
  }

  public String getOldRepositoryKey() {
    return oldRepositoryKey;
  }

  public RuleKey getOldRuleKeyAsRuleKey() {
    return RuleKey.of(oldRepositoryKey, oldRuleKey);
  }

  public RuleKey getNewRuleKeyAsRuleKey() {
    return RuleKey.of(newRepositoryKey, newRuleKey);
  }

  @CheckForNull
  public String getNewRuleKey() {
    return newRuleKey;
  }

  @CheckForNull
  public String getNewRepositoryKey() {
    return newRepositoryKey;
  }

  @CheckForNull
  public String getUuid() {
    return uuid;
  }

  @CheckForNull
  public String getRuleUuid() {
    return ruleUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingleDeprecatedRuleKey that = (SingleDeprecatedRuleKey) o;
    return Objects.equals(oldRuleKey, that.oldRuleKey) &&
      Objects.equals(oldRepositoryKey, that.oldRepositoryKey) &&
      Objects.equals(newRuleKey, that.newRuleKey) &&
      Objects.equals(newRepositoryKey, that.newRepositoryKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oldRuleKey, oldRepositoryKey, newRuleKey, newRepositoryKey);
  }

  private SingleDeprecatedRuleKey setRuleUuid(String ruleUuid) {
    this.ruleUuid = ruleUuid;
    return this;
  }

  private SingleDeprecatedRuleKey setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  private SingleDeprecatedRuleKey setOldRuleKey(String oldRuleKey) {
    this.oldRuleKey = oldRuleKey;
    return this;
  }

  private SingleDeprecatedRuleKey setOldRepositoryKey(String oldRepositoryKey) {
    this.oldRepositoryKey = oldRepositoryKey;
    return this;
  }

  private SingleDeprecatedRuleKey setNewRuleKey(@Nullable String newRuleKey) {
    this.newRuleKey = newRuleKey;
    return this;
  }

  private SingleDeprecatedRuleKey setNewRepositoryKey(@Nullable String newRepositoryKey) {
    this.newRepositoryKey = newRepositoryKey;
    return this;
  }
}
