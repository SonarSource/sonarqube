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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleTypeMapperTest {

  @Test
  void toApiRuleType_shouldMapToApiRuleType() {
    assertThat(RuleTypeMapper.toApiRuleType(org.sonar.core.rule.RuleType.BUG)).isEqualTo(org.sonar.api.rules.RuleType.BUG);
    assertThat(RuleTypeMapper.toApiRuleType(org.sonar.core.rule.RuleType.CODE_SMELL)).isEqualTo(org.sonar.api.rules.RuleType.CODE_SMELL);
    assertThat(RuleTypeMapper.toApiRuleType(org.sonar.core.rule.RuleType.SECURITY_HOTSPOT)).isEqualTo(org.sonar.api.rules.RuleType.SECURITY_HOTSPOT);
    assertThat(RuleTypeMapper.toApiRuleType(org.sonar.core.rule.RuleType.VULNERABILITY)).isEqualTo(org.sonar.api.rules.RuleType.VULNERABILITY);
  }

  @Test
  void toRuleType_shouldMapToCoreRuleType() {
    assertThat(RuleTypeMapper.toRuleType(org.sonar.api.rules.RuleType.BUG)).isEqualTo(org.sonar.core.rule.RuleType.BUG);
    assertThat(RuleTypeMapper.toRuleType(org.sonar.api.rules.RuleType.CODE_SMELL)).isEqualTo(org.sonar.core.rule.RuleType.CODE_SMELL);
    assertThat(RuleTypeMapper.toRuleType(org.sonar.api.rules.RuleType.SECURITY_HOTSPOT)).isEqualTo(org.sonar.core.rule.RuleType.SECURITY_HOTSPOT);
    assertThat(RuleTypeMapper.toRuleType(org.sonar.api.rules.RuleType.VULNERABILITY)).isEqualTo(org.sonar.core.rule.RuleType.VULNERABILITY);
  }

}
