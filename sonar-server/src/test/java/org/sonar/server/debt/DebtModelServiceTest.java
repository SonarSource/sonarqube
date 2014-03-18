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
package org.sonar.server.debt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelServiceTest {

  @Mock
  CharacteristicDao dao;

  DebtModelService service;

  @Before
  public void setUp() throws Exception {
    service = new DebtModelService(dao);
  }

  @Test
  public void find_root_characteristics() {
    CharacteristicDto dto = new CharacteristicDto()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");
    when(dao.selectEnabledRootCharacteristics()).thenReturn(newArrayList(dto));
    assertThat(service.rootCharacteristics()).hasSize(1);
  }

  @Test
  public void find_characteristics() {
    CharacteristicDto dto = new CharacteristicDto()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");
    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(dto));
    assertThat(service.characteristics()).hasSize(1);
  }

  @Test
  public void find_characteristic_by_id() {
    CharacteristicDto dto = new CharacteristicDto()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use")
      .setParentId(2)
      .setOrder(1);
    when(dao.selectById(1)).thenReturn(dto);

    DebtCharacteristic characteristic = service.characteristicById(1);
    assertThat(characteristic.id()).isEqualTo(1);
    assertThat(characteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(characteristic.name()).isEqualTo("Memory use");
    assertThat(characteristic.parentId()).isEqualTo(2);
    assertThat(characteristic.order()).isEqualTo(1);

    assertThat(service.characteristicById(10)).isNull();
  }

}
