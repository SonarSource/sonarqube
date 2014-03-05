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

package org.sonar.core.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.internal.WorkDuration;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultTechnicalDebtModelTest {

  private DefaultTechnicalDebtModel sqaleModel;

  @Before
  public void setUp() throws Exception {
    sqaleModel = new DefaultTechnicalDebtModel();
  }

  @Test
  public void get_root_characteristics() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic()
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");

    new DefaultCharacteristic()
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParent(rootCharacteristic);

    sqaleModel.addRootCharacteristic(rootCharacteristic);

    assertThat(sqaleModel.rootCharacteristics()).hasSize(1);
    DefaultCharacteristic resultRootCharacteristic = sqaleModel.rootCharacteristics().get(0);
    assertThat(resultRootCharacteristic).isEqualTo(rootCharacteristic);
  }

  @Test
  public void get_characteristic_by_key() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic()
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParent(rootCharacteristic);

    sqaleModel.addRootCharacteristic(rootCharacteristic);

    assertThat(sqaleModel.characteristicByKey("MEMORY_EFFICIENCY")).isEqualTo(rootCharacteristic);
    assertThat(sqaleModel.characteristicByKey("EFFICIENCY")).isEqualTo(characteristic);
    assertThat(sqaleModel.characteristicByKey("EFFICIENCY").parent()).isEqualTo(rootCharacteristic);

    assertThat(sqaleModel.characteristicByKey("UNKNOWN")).isNull();
  }

  @Test
  public void get_requirement_by_rule_key() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic()
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParent(rootCharacteristic);

    RuleKey ruleKey = RuleKey.of("checkstyle", "Regexp");
    DefaultRequirement requirement = new DefaultRequirement()
      .setCharacteristic(characteristic)
      .setRuleKey(ruleKey)
      .setFunction("linear")
      .setFactorValue(2)
      .setFactorUnit(WorkDuration.UNIT.HOURS)
      .setOffsetValue(0)
      .setOffsetUnit(WorkDuration.UNIT.HOURS);

    sqaleModel.addRootCharacteristic(rootCharacteristic);

    assertThat(sqaleModel.requirementsByRule(ruleKey)).isEqualTo(requirement);
    assertThat(sqaleModel.requirementsByRule(RuleKey.of("not", "found"))).isNull();
  }
}
