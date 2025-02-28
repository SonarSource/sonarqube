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
package org.sonar.core.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class RuleTypeTest {

  @Test
  void fromDbConstant_shouldReturnCorrectEnum() {
    assertThat(RuleType.fromDbConstant(1)).isEqualTo(RuleType.CODE_SMELL);
    assertThat(RuleType.fromDbConstant(2)).isEqualTo(RuleType.BUG);
    assertThat(RuleType.fromDbConstant(3)).isEqualTo(RuleType.VULNERABILITY);
    assertThat(RuleType.fromDbConstant(4)).isEqualTo(RuleType.SECURITY_HOTSPOT);
  }

  @Test
  void fromDbConstant_shouldThrowExceptionForInvalidValue() {
    assertThatThrownBy(() -> RuleType.fromDbConstant(99))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported type value : 99");
  }

  @Test
  void valueOfNullable_shouldReturnCorrectEnumOrNull() {
    assertThat(RuleType.valueOfNullable(1)).isEqualTo(RuleType.CODE_SMELL);
    assertThat(RuleType.valueOfNullable(0)).isNull();
  }

  @Test
  void valueOfNullable_shouldThrowExceptionForInvalidValue() {
    assertThatThrownBy(() -> RuleType.valueOfNullable(99))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported type value : 99");
  }

  @Test
  void names_shouldReturnAllEnumNames() {
    assertThat(RuleType.names()).containsExactly("CODE_SMELL", "BUG", "VULNERABILITY", "SECURITY_HOTSPOT");
  }

  @Test
  void getDbConstant_shouldReturnCorrectDbConstant() {
    assertThat(RuleType.CODE_SMELL.getDbConstant()).isEqualTo(1);
    assertThat(RuleType.BUG.getDbConstant()).isEqualTo(2);
    assertThat(RuleType.VULNERABILITY.getDbConstant()).isEqualTo(3);
    assertThat(RuleType.SECURITY_HOTSPOT.getDbConstant()).isEqualTo(4);
  }
}
