/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.sonar.api.rules.RuleCharacteristic;
import org.sonar.api.rules.RuleType;

public class RuleTypeToRuleCharacteristicConverter {

  private RuleTypeToRuleCharacteristicConverter() {
  }

  public static RuleCharacteristic convertToRuleCharacteristic(int ruleType) {
    if (ruleType == 0) {
      return RuleCharacteristic.CLEAR;
    }
    return convertToRuleCharacteristic(RuleType.valueOf(ruleType));
  }

  public static RuleCharacteristic convertToRuleCharacteristic(RuleType ruleType) {
    return switch (ruleType) {
      case BUG -> RuleCharacteristic.ROBUST;
      case CODE_SMELL -> RuleCharacteristic.CLEAR;
      case SECURITY_HOTSPOT, VULNERABILITY -> RuleCharacteristic.SECURE;
    };
  }

}
