/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.rules.CleanCodeAttribute;

public class RuleChangeDto {

  private String uuid;
  private CleanCodeAttribute oldCleanCodeAttribute;
  private CleanCodeAttribute newCleanCodeAttribute;
  private Set<RuleImpactChangeDto> ruleImpactChanges = new HashSet<>();
  private String ruleUuid;

  public RuleChangeDto() {
    // nothing to do
  }

  public CleanCodeAttribute getOldCleanCodeAttribute() {
    return oldCleanCodeAttribute;
  }

  public void setOldCleanCodeAttribute(CleanCodeAttribute oldCleanCodeAttribute) {
    this.oldCleanCodeAttribute = oldCleanCodeAttribute;
  }

  public CleanCodeAttribute getNewCleanCodeAttribute() {
    return newCleanCodeAttribute;
  }

  public void setNewCleanCodeAttribute(CleanCodeAttribute newCleanCodeAttribute) {
    this.newCleanCodeAttribute = newCleanCodeAttribute;
  }

  public Set<RuleImpactChangeDto> getRuleImpactChanges() {
    return ruleImpactChanges;
  }

  public void setRuleImpactChanges(Set<RuleImpactChangeDto> ruleImpactChanges) {
    this.ruleImpactChanges = ruleImpactChanges;
  }

  public String getRuleUuid() {
    return ruleUuid;
  }

  public void setRuleUuid(String ruleUuid) {
    this.ruleUuid = ruleUuid;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public void addRuleImpactChangeDto(RuleImpactChangeDto ruleImpactChangeDto) {
    this.ruleImpactChanges.add(ruleImpactChangeDto);
  }
}
