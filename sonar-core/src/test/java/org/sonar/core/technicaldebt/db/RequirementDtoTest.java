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
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.DefaultRequirement;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class RequirementDtoTest {

  @Test
  public void to_default_requirement(){
    RuleDto ruleDto = new RuleDto();
    ruleDto.setRepositoryKey("squid");
    ruleDto.setRuleKey("AvoidCycle");
    CharacteristicDto characteristicDto = new CharacteristicDto();
    CharacteristicDto rootCharacteristicDto = new CharacteristicDto();

    RequirementPropertyDto requirementPropertyDto1 = new RequirementPropertyDto()
      .setKey("remediationFactor")
      .setValue(30d)
      .setTextValue("mn");
    RequirementPropertyDto requirementPropertyDto2 = new RequirementPropertyDto()
      .setKey("remediationFunction")
      .setTextValue("linear");

    RequirementDto dto = new RequirementDto()
      .setId(1L)
      .setRule(ruleDto)
      .setCharacteristic(characteristicDto)
      .setRootCharacteristic(rootCharacteristicDto)
      .setProperties(newArrayList(requirementPropertyDto1, requirementPropertyDto2));

    DefaultRequirement result = dto.toDefaultRequirement();
    assertThat(result).isNotNull();
    assertThat(result.ruleKey()).isNotNull();
    assertThat(result.characteristic()).isNotNull();
    assertThat(result.rootCharacteristic()).isNotNull();
    assertThat(result.function()).isNotNull();
    assertThat(result.factor()).isNotNull();
  }
}
