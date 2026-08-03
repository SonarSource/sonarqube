/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.issue;

import org.sonar.core.rule.RuleType;

/**
 * An {@link IssueDto} carrying, in addition to the full issue columns, the type of the finding's rule as resolved
 * by the same scroll query (a join on {@code rules}). Used by the Hotspots-to-Issues migration (MMF-5734) so the
 * target type — the type to the rule (Vulnerability, Code Smell, or Bug) — is returned inline in a
 * single query, while still reusing {@link IssueDto#toDefaultIssue()} and the full mapping for the in-place update.
 */
public class HotspotToMigrateDto extends IssueDto {

  // Safe default: an unpopulated instance reads as still a hotspot (skipped by the migration) rather than the
  // invalid db constant 0. In practice the value is always set from r.rule_type by the scroll query's result map.
  private int ruleType = RuleType.SECURITY_HOTSPOT.getDbConstant();

  public int getRuleType() {
    return ruleType;
  }

  public HotspotToMigrateDto setRuleType(int ruleType) {
    this.ruleType = ruleType;
    return this;
  }

  /**
   * The rule's type as an enum — for hotspots pending migration this is the type assigned to the rule.
   */
  public RuleType getRuleTypeEnum() {
    return RuleType.fromDbConstant(ruleType);
  }
}