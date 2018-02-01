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

package org.sonar.db.rule;

import java.util.Objects;
import javax.annotation.CheckForNull;

/**
 * Map the table "deprecated_rule_keys"
 */
public class DeprecatedRuleKeyDto {
  /**
   *  Uuid of the deprecated key
   */
  private String uuid;
  /**
   * the id of the current rule for this deprecated key
   */
  private Integer ruleId;
  /**
   * repository key that was deprecated
   */
  private String oldRepositoryKey;
  /**
   * rule key that was deprecated, not nullable
   */
  private String oldRuleKey;
  /**
   * creation date of the row
   */
  private Long createdAt;

  /**
   * current repository key retrieved from an external join on rule_id
   */
  private String newRepositoryKey;
  /**
   * current rule key retrieved from an external join on rule_id
   */
  private String newRuleKey;

  public String getUuid() {
    return uuid;
  }

  public DeprecatedRuleKeyDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public DeprecatedRuleKeyDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public String getOldRepositoryKey() {
    return oldRepositoryKey;
  }

  public DeprecatedRuleKeyDto setOldRepositoryKey(String oldRepositoryKey) {
    this.oldRepositoryKey = oldRepositoryKey;
    return this;
  }

  public String getOldRuleKey() {
    return oldRuleKey;
  }

  public DeprecatedRuleKeyDto setOldRuleKey(String oldRuleKey) {
    this.oldRuleKey = oldRuleKey;
    return this;
  }

  /**
   * This value may be null if the rule has been deleted
   *
   * @return the current repository key
   */
  @CheckForNull
  public String getNewRepositoryKey() {
    return newRepositoryKey;
  }

  /**
   * This value may be null if the rule has been deleted
   *
   * @return the current rule key
   */
  @CheckForNull
  public String getNewRuleKey() {
    return newRuleKey;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public DeprecatedRuleKeyDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DeprecatedRuleKeyDto other = (DeprecatedRuleKeyDto) obj;
    return Objects.equals(this.uuid, other.uuid);
  }
}
