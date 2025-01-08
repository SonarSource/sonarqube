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
package org.sonar.server.v2.api.rule.enums;

import java.util.Arrays;
import org.sonar.api.rules.RuleType;

public enum RuleTypeRestEnum {
  CODE_SMELL(RuleType.CODE_SMELL),
  BUG(RuleType.BUG),
  VULNERABILITY(RuleType.VULNERABILITY),
  SECURITY_HOTSPOT(RuleType.SECURITY_HOTSPOT),
  ;

  private final RuleType ruleType;

  RuleTypeRestEnum(RuleType ruleType) {
    this.ruleType = ruleType;
  }

  public RuleType getRuleType() {
    return ruleType;
  }

  public static RuleTypeRestEnum from(RuleType ruleType) {
    return Arrays.stream(RuleTypeRestEnum.values())
      .filter(ruleTypeRestEnum -> ruleTypeRestEnum.ruleType.equals(ruleType))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported RuleType: " + ruleType));
  }
}
