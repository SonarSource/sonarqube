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

package org.sonar.batch.debt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtModel;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
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
  public void find_all() throws Exception {
    CharacteristicDto rootCharacteristicDto = new CharacteristicDto()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");

    CharacteristicDto characteristicDto = new CharacteristicDto()
      .setId(2)
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParentId(1);

    CharacteristicDto requirementDto = new CharacteristicDto()
      .setId(3)
      .setParentId(2)
      .setRuleId(100)
      .setFunction("linear")
      .setFactorValue(2d)
      .setFactorUnit(CharacteristicDto.DAYS)
      .setOffsetValue(0d)
      .setOffsetUnit(CharacteristicDto.MINUTES);

    RuleKey ruleKey = RuleKey.of("checkstyle", "Regexp");
    Rule rule = Rule.create(ruleKey.repository(), ruleKey.rule());
    rule.setId(100);
    when(dao.selectCharacteristics()).thenReturn(newArrayList(rootCharacteristicDto, characteristicDto, requirementDto));

    DefaultTechnicalDebtModel result = (DefaultTechnicalDebtModel) provider.provide(dao);
    assertThat(result.rootCharacteristics()).hasSize(1);

    DefaultCharacteristic rootCharacteristic = result.characteristicByKey("MEMORY_EFFICIENCY");
    assertThat(rootCharacteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(rootCharacteristic.name()).isEqualTo("Memory use");
    assertThat(rootCharacteristic.parent()).isNull();
    assertThat(rootCharacteristic.children()).hasSize(1);
    assertThat(rootCharacteristic.children().get(0).key()).isEqualTo("EFFICIENCY");

    DefaultCharacteristic characteristic = result.characteristicByKey("EFFICIENCY");
    assertThat(characteristic.key()).isEqualTo("EFFICIENCY");
    assertThat(characteristic.name()).isEqualTo("Efficiency");
    assertThat(characteristic.parent().key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(characteristic.children()).isEmpty();
  }

}
