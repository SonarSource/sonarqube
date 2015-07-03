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
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.debt.CharacteristicDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelLookupTest {

  @Mock
  CharacteristicDao dao;

  CharacteristicDto characteristicDto = new CharacteristicDto()
    .setId(1)
    .setKey("MEMORY_EFFICIENCY")
    .setName("Memory use")
    .setOrder(2)
    .setEnabled(true);

  DebtModelLookup service;

  @Before
  public void setUp() {
    service = new DebtModelLookup(dao);
  }

  @Test
  public void find_root_characteristics() {
    when(dao.selectEnabledRootCharacteristics()).thenReturn(newArrayList(characteristicDto));
    assertThat(service.rootCharacteristics()).hasSize(1);
  }

  @Test
  public void find_all_characteristics() {
    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(characteristicDto));
    assertThat(service.allCharacteristics()).hasSize(1);
  }

  @Test
  public void find_characteristic_by_id() {
    when(dao.selectById(1)).thenReturn(characteristicDto);

    DefaultDebtCharacteristic characteristic = (DefaultDebtCharacteristic) service.characteristicById(1);
    assertThat(characteristic.id()).isEqualTo(1);
    assertThat(characteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(characteristic.name()).isEqualTo("Memory use");
    assertThat(characteristic.order()).isEqualTo(2);
    assertThat(characteristic.parentId()).isNull();

    assertThat(service.characteristicById(111)).isNull();
  }

  @Test
  public void find_characteristic_by_key() {
    when(dao.selectByKey("MEMORY_EFFICIENCY")).thenReturn(characteristicDto);

    DefaultDebtCharacteristic characteristic = (DefaultDebtCharacteristic) service.characteristicByKey("MEMORY_EFFICIENCY");
    assertThat(characteristic.id()).isEqualTo(1);
    assertThat(characteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(characteristic.name()).isEqualTo("Memory use");
    assertThat(characteristic.order()).isEqualTo(2);
    assertThat(characteristic.parentId()).isNull();

    assertThat(service.characteristicByKey("UNKNOWN")).isNull();
  }

}
