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

package org.sonar.batch.debt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.debt.DebtCharacteristic;
import org.sonar.api.batch.debt.DebtModel;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelProviderTest {

  @Mock
  CharacteristicDao dao;

  DebtModelProvider provider;

  @Before
  public void before() {
    provider = new DebtModelProvider();
  }

  @Test
  public void provide_model() throws Exception {
    CharacteristicDto rootCharacteristicDto = new CharacteristicDto()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use")
      .setOrder(1);

    CharacteristicDto characteristicDto = new CharacteristicDto()
      .setId(2)
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParentId(1);

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(rootCharacteristicDto, characteristicDto));

    DebtModel result = provider.provide(dao);
    assertThat(result.characteristics()).hasSize(1);

    DebtCharacteristic characteristic = result.characteristicByKey("MEMORY_EFFICIENCY");
    assertThat(characteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(characteristic.name()).isEqualTo("Memory use");
    assertThat(characteristic.isSub()).isFalse();
    assertThat(characteristic.order()).isEqualTo(1);

    DebtCharacteristic subCharacteristic = result.characteristicByKey("EFFICIENCY");
    assertThat(subCharacteristic.key()).isEqualTo("EFFICIENCY");
    assertThat(subCharacteristic.name()).isEqualTo("Efficiency");
    assertThat(subCharacteristic.isSub()).isTrue();
    assertThat(subCharacteristic.order()).isNull();
  }
}
