/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.rule;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleDto.DISABLED_CHARACTERISTIC_ID;

public class RuleDtoTest {

  public static final int FAKE_SUB_CHAR_1 = 27;
  public static final int FAKE_SUB_CHAR_2 = 42;

  @Test
  public void effective_sub_characteristic_id() {
    RuleDto dto = new RuleDto();

    // characteristic is not set
    dto.setSubCharacteristicId(null).setDefaultSubCharacteristicId(null);
    assertThat(dto.getEffectiveSubCharacteristicId()).isNull();

    // default characteristic is set
    dto.setSubCharacteristicId(null).setDefaultSubCharacteristicId(FAKE_SUB_CHAR_2);
    assertThat(dto.getEffectiveSubCharacteristicId()).isEqualTo(FAKE_SUB_CHAR_2);

    // default characteristic is set to "none"
    dto.setSubCharacteristicId(null).setDefaultSubCharacteristicId(DISABLED_CHARACTERISTIC_ID);
    assertThat(dto.getEffectiveSubCharacteristicId()).isNull();

    // characteristic is overridden
    dto.setSubCharacteristicId(FAKE_SUB_CHAR_1).setDefaultSubCharacteristicId(FAKE_SUB_CHAR_2);
    assertThat(dto.getEffectiveSubCharacteristicId()).isEqualTo(FAKE_SUB_CHAR_1);

    // characteristic is set, no defaults
    dto.setSubCharacteristicId(FAKE_SUB_CHAR_1).setDefaultSubCharacteristicId(null);
    assertThat(dto.getEffectiveSubCharacteristicId()).isEqualTo(FAKE_SUB_CHAR_1);

    // characteristic is set to "none"
    dto.setSubCharacteristicId(DISABLED_CHARACTERISTIC_ID).setDefaultSubCharacteristicId(FAKE_SUB_CHAR_2);
    assertThat(dto.getEffectiveSubCharacteristicId()).isNull();
  }
}
