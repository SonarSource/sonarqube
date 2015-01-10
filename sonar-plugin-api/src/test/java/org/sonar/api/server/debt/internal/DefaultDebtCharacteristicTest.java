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

package org.sonar.api.server.debt.internal;

import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultDebtCharacteristicTest {

  @Test
  public void setter_and_getter_on_characteristic() throws Exception {
    DefaultDebtCharacteristic debtCharacteristic = new DefaultDebtCharacteristic()
      .setId(1)
      .setKey("PORTABILITY")
      .setName("Portability")
      .setOrder(1)
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());

    assertThat(debtCharacteristic.id()).isEqualTo(1);
    assertThat(debtCharacteristic.key()).isEqualTo("PORTABILITY");
    assertThat(debtCharacteristic.name()).isEqualTo("Portability");
    assertThat(debtCharacteristic.order()).isEqualTo(1);
    assertThat(debtCharacteristic.parentId()).isNull();
    assertThat(debtCharacteristic.isSub()).isFalse();
    assertThat(debtCharacteristic.createdAt()).isNotNull();
    assertThat(debtCharacteristic.updatedAt()).isNotNull();
  }

  @Test
  public void setter_and_getter_on_sub_characteristic() throws Exception {
    DefaultDebtCharacteristic debtCharacteristic = new DefaultDebtCharacteristic()
      .setId(1)
      .setKey("COMPILER")
      .setName("Compiler")
      .setParentId(2)
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());

    assertThat(debtCharacteristic.id()).isEqualTo(1);
    assertThat(debtCharacteristic.key()).isEqualTo("COMPILER");
    assertThat(debtCharacteristic.name()).isEqualTo("Compiler");
    assertThat(debtCharacteristic.order()).isNull();
    assertThat(debtCharacteristic.parentId()).isEqualTo(2);
    assertThat(debtCharacteristic.isSub()).isTrue();
    assertThat(debtCharacteristic.createdAt()).isNotNull();
    assertThat(debtCharacteristic.updatedAt()).isNotNull();
  }

  @Test
  public void to_string() throws Exception {
    assertThat(new DefaultDebtCharacteristic()
      .setId(1)
      .setKey("PORTABILITY")
      .setName("Portability")
      .setOrder(1)
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date())).isNotNull();
  }
}
