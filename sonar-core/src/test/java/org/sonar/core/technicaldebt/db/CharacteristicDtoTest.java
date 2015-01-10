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

package org.sonar.core.technicaldebt.db;

import org.junit.Test;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class CharacteristicDtoTest {

  @Test
  public void to_dto_from_characteristic() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(2)
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParent(rootCharacteristic)
      .setOrder(5)
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());

    CharacteristicDto dto = CharacteristicDto.toDto(characteristic, 1);
    assertThat(dto.getId()).isNull();
    assertThat(dto.getParentId()).isEqualTo(1);
    assertThat(dto.getKey()).isEqualTo("EFFICIENCY");
    assertThat(dto.getName()).isEqualTo("Efficiency");
    assertThat(dto.getOrder()).isEqualTo(5);
    assertThat(dto.isEnabled()).isTrue();
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
  }

  @Test
  public void to_characteristic() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");


    CharacteristicDto dto = new CharacteristicDto()
      .setId(2)
      .setParentId(1)
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setOrder(5)
      .setEnabled(false)
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());

    DefaultCharacteristic characteristic = dto.toCharacteristic(rootCharacteristic);
    assertThat(characteristic.id()).isEqualTo(2);
    assertThat(characteristic.parent()).isEqualTo(rootCharacteristic);
    assertThat(characteristic.key()).isEqualTo("EFFICIENCY");
    assertThat(characteristic.name()).isEqualTo("Efficiency");
    assertThat(characteristic.order()).isEqualTo(5);
    assertThat(characteristic.createdAt()).isNotNull();
    assertThat(characteristic.updatedAt()).isNotNull();
  }
}
