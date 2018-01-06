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
package org.sonar.db.qualityprofile;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.sonar.api.rule.RuleKey;

/**
 *
 * @since 4.4
 */
public class ActiveRuleKey implements Serializable, Comparable<ActiveRuleKey> {

  private final String ruleProfileUuid;
  private final RuleKey ruleKey;

  protected ActiveRuleKey(String ruleProfileUuid, RuleKey ruleKey) {
    this.ruleProfileUuid = ruleProfileUuid;
    this.ruleKey = ruleKey;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static ActiveRuleKey of(QProfileDto profile, RuleKey ruleKey) {
    return new ActiveRuleKey(profile.getRulesProfileUuid(), ruleKey);
  }

  public static ActiveRuleKey of(RulesProfileDto rulesProfile, RuleKey ruleKey) {
    return new ActiveRuleKey(rulesProfile.getKee(), ruleKey);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static ActiveRuleKey parse(String s) {
    Preconditions.checkArgument(s.split(":").length >= 3, "Bad format of activeRule key: " + s);
    int semiColonPos = s.indexOf(':');
    String ruleProfileUuid = s.substring(0, semiColonPos);
    String ruleKey = s.substring(semiColonPos + 1);
    return new ActiveRuleKey(ruleProfileUuid, RuleKey.parse(ruleKey));
  }

  /**
   * Never null
   */
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  /**
   * Never null
   */
  public String getRuleProfileUuid() {
    return ruleProfileUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActiveRuleKey activeRuleKey = (ActiveRuleKey) o;
    if (!ruleProfileUuid.equals(activeRuleKey.ruleProfileUuid)) {
      return false;
    }
    return ruleKey.equals(activeRuleKey.ruleKey);
  }

  @Override
  public int hashCode() {
    int result = ruleProfileUuid.hashCode();
    result = 31 * result + ruleKey.hashCode();
    return result;
  }

  /**
   * Format is "qprofile:rule", for example "12345:squid:AvoidCycle"
   */
  @Override
  public String toString() {
    return String.format("%s:%s", ruleProfileUuid, ruleKey.toString());
  }

  @Override
  public int compareTo(ActiveRuleKey o) {
    int compareQualityProfileKey = this.ruleProfileUuid.compareTo(o.ruleProfileUuid);
    if (compareQualityProfileKey == 0) {
      return this.ruleKey.compareTo(o.ruleKey);
    }
    return compareQualityProfileKey;
  }
}
