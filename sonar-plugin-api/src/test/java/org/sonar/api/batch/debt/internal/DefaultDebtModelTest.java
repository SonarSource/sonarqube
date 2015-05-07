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

package org.sonar.api.batch.debt.internal;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultDebtModelTest {

  private DefaultDebtModel debtModel;

  @Before
  public void setUp() {
    debtModel = new DefaultDebtModel()
      .addCharacteristic(
        new DefaultDebtCharacteristic().setId(1)
          .setKey("MEMORY_EFFICIENCY")
          .setName("Memory use")
          .setOrder(1)
      )
      .addSubCharacteristic(
        new DefaultDebtCharacteristic().setId(2)
          .setKey("EFFICIENCY")
          .setName("Efficiency")
          .setParentId(1),
        "MEMORY_EFFICIENCY"
      );
  }

  @Test
  public void all_characteristics() {
    assertThat(debtModel.allCharacteristics()).hasSize(2);
  }

  @Test
  public void characteristics() {
    assertThat(debtModel.characteristics()).hasSize(1);
  }

  @Test
  public void sub_characteristics() {
    assertThat(debtModel.subCharacteristics("MEMORY_EFFICIENCY")).hasSize(1);
  }

  @Test
  public void characteristic_by_id() {
    DefaultDebtCharacteristic debtCharacteristic = (DefaultDebtCharacteristic) debtModel.characteristicById(1);
    assertThat(debtCharacteristic).isNotNull();
    assertThat(debtCharacteristic.id()).isEqualTo(1);
    assertThat(debtCharacteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(debtCharacteristic.name()).isEqualTo("Memory use");
    assertThat(debtCharacteristic.order()).isEqualTo(1);
    assertThat(debtCharacteristic.parentId()).isNull();
    assertThat(debtCharacteristic.isSub()).isFalse();


    assertThat(debtModel.characteristicById(555)).isNull();
  }

  @Test
  public void characteristic_by_key() {
    DefaultDebtCharacteristic debtCharacteristic = (DefaultDebtCharacteristic) debtModel.characteristicByKey("EFFICIENCY");
    assertThat(debtCharacteristic).isNotNull();
    assertThat(debtCharacteristic.id()).isEqualTo(2);
    assertThat(debtCharacteristic.key()).isEqualTo("EFFICIENCY");
    assertThat(debtCharacteristic.name()).isEqualTo("Efficiency");
    assertThat(debtCharacteristic.order()).isNull();
    assertThat(debtCharacteristic.parentId()).isEqualTo(1);
    assertThat(debtCharacteristic.isSub()).isTrue();

    assertThat(debtModel.characteristicByKey("UNKNOWN")).isNull();
  }
}
