/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.technicaldebt.db;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.internal.WorkDuration;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class CharacteristicDtoTest {

  @Test
  public void to_dto_from_requirement() throws Exception {
    DefaultRequirement requirement = new DefaultRequirement()
      .setFunction("constant_issue")
      .setFactorValue(10)
      .setFactorUnit(WorkDuration.UNIT.DAYS)
      .setOffsetValue(5)
      .setOffsetUnit(WorkDuration.UNIT.MINUTES)
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());

    CharacteristicDto dto = CharacteristicDto.toDto(requirement, 2, 1, 10);
    assertThat(dto.getRuleId()).isEqualTo(10);
    assertThat(dto.getParentId()).isEqualTo(2);
    assertThat(dto.getRootId()).isEqualTo(1);
    assertThat(dto.getFunction()).isEqualTo("constant_issue");
    assertThat(dto.getFactorValue()).isEqualTo(10d);
    assertThat(dto.getFactorUnit()).isEqualTo(CharacteristicDto.DAYS);
    assertThat(dto.getOffsetValue()).isEqualTo(5d);
    assertThat(dto.getOffsetUnit()).isEqualTo(CharacteristicDto.MINUTES);
    assertThat(dto.isEnabled()).isTrue();
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
  }

  @Test
  public void to_requirement() throws Exception {
    CharacteristicDto requirementDto = new CharacteristicDto()
      .setId(3)
      .setParentId(2)
      .setRuleId(100)
      .setFunction("linear")
      .setFactorValue(2d)
      .setFactorUnit(CharacteristicDto.DAYS)
      .setOffsetValue(0d)
      .setOffsetUnit(CharacteristicDto.MINUTES)
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());

    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic()
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParent(rootCharacteristic);

    DefaultRequirement requirement =  requirementDto.toRequirement(RuleKey.of("squid", "S106"), characteristic, rootCharacteristic);
    assertThat(requirement.ruleKey()).isEqualTo(RuleKey.of("squid", "S106"));
    assertThat(requirement.characteristic()).isEqualTo(characteristic);
    assertThat(requirement.rootCharacteristic()).isEqualTo(rootCharacteristic);
    assertThat(requirement.function()).isEqualTo("linear");
    assertThat(requirement.factorValue()).isEqualTo(2);
    assertThat(requirement.factorUnit()).isEqualTo(WorkDuration.UNIT.DAYS);
    assertThat(requirement.offsetValue()).isEqualTo(0);
    assertThat(requirement.offsetUnit()).isEqualTo(WorkDuration.UNIT.MINUTES);
    assertThat(requirement.createdAt()).isNotNull();
    assertThat(requirement.updatedAt()).isNotNull();

  }
}
