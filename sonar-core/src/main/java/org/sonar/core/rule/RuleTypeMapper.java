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
package org.sonar.core.rule;

public class RuleTypeMapper {

  private RuleTypeMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static org.sonar.api.rules.RuleType toApiRuleType(RuleType ruleType) {
    return switch (ruleType) {
      case BUG -> org.sonar.api.rules.RuleType.BUG;
      case CODE_SMELL -> org.sonar.api.rules.RuleType.CODE_SMELL;
      case SECURITY_HOTSPOT -> org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
      case VULNERABILITY -> org.sonar.api.rules.RuleType.VULNERABILITY;
    };
  }

  public static RuleType toRuleType(org.sonar.api.rules.RuleType ruleType) {
    return switch (ruleType) {
      case BUG -> RuleType.BUG;
      case CODE_SMELL -> RuleType.CODE_SMELL;
      case SECURITY_HOTSPOT -> RuleType.SECURITY_HOTSPOT;
      case VULNERABILITY -> RuleType.VULNERABILITY;
    };
  }

}
