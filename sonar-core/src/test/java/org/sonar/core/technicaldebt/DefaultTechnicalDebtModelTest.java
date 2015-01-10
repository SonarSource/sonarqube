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

package org.sonar.core.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTechnicalDebtModelTest {

  private DefaultTechnicalDebtModel sqaleModel;

  @Before
  public void setUp() throws Exception {
    DefaultDebtModel debtModel = new DefaultDebtModel();
    debtModel.addCharacteristic(
      new DefaultDebtCharacteristic().setId(1)
        .setKey("MEMORY_EFFICIENCY")
        .setName("Memory use")
        .setOrder(1)
    );
    debtModel.addSubCharacteristic(
      new DefaultDebtCharacteristic().setId(2)
        .setKey("EFFICIENCY")
        .setName("Efficiency")
        .setParentId(1),
      "MEMORY_EFFICIENCY"
    );
    sqaleModel = new DefaultTechnicalDebtModel(debtModel);
  }

  @Test
  public void get_characteristics() throws Exception {
    assertThat(sqaleModel.rootCharacteristics()).hasSize(1);

    DefaultCharacteristic resultRootCharacteristic = sqaleModel.rootCharacteristics().get(0);
    assertThat(resultRootCharacteristic.id()).isEqualTo(1);
    assertThat(resultRootCharacteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(resultRootCharacteristic.name()).isEqualTo("Memory use");
    assertThat(resultRootCharacteristic.order()).isEqualTo(1);
    assertThat(resultRootCharacteristic.children()).hasSize(1);
    assertThat(resultRootCharacteristic.parent()).isNull();
    assertThat(resultRootCharacteristic.root()).isNull();
  }

  @Test
  public void get_characteristic_by_key() throws Exception {
    assertThat(sqaleModel.characteristicByKey("MEMORY_EFFICIENCY")).isNotNull();
    assertThat(sqaleModel.characteristicByKey("EFFICIENCY")).isNotNull();
    assertThat(sqaleModel.characteristicByKey("EFFICIENCY").parent()).isNotNull();

    assertThat(sqaleModel.characteristicByKey("UNKNOWN")).isNull();
  }

  @Test
  public void characteristic_by_id() throws Exception {
    assertThat(sqaleModel.characteristicById(1)).isNotNull();
    assertThat(sqaleModel.characteristicById(2)).isNotNull();
    assertThat(sqaleModel.characteristicById(123)).isNull();
  }

  @Test
  public void get_requirement_by_rule_key_always_return_null() throws Exception {
    assertThat(sqaleModel.requirementsByRule(RuleKey.of("checkstyle", "Regexp"))).isNull();
  }

  @Test
  public void get_requirement_by_id_always_return_null() throws Exception {
    assertThat(sqaleModel.requirementsById(1)).isNull();
  }

  @Test
  public void get_requirements_always_return_empty_list() throws Exception {
    assertThat(sqaleModel.requirements()).isEmpty();
  }

}
