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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class RequirementDaoTest extends AbstractDaoTestCase {

  RequirementDao dao;

  @Before
  public void createDao() {
    dao = new RequirementDao(getMyBatis());
  }

  @Test
  public void select_requirement_from_rule() {
    setupData("select_requirement_from_rule");

    RequirementDto requirementDto = dao.selectByRuleId(1L);

    assertThat(requirementDto).isNotNull();
    assertThat(requirementDto.getId()).isEqualTo(2);

    assertThat(requirementDto.getRule().getId()).isEqualTo(1);
    assertThat(requirementDto.getRule().getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(requirementDto.getRule().getRuleKey()).isEqualTo("import");
    assertThat(requirementDto.getRule().getName()).isEqualTo("Regular exp");

    assertThat(requirementDto.getRootCharacteristic().getId()).isEqualTo(3);
    assertThat(requirementDto.getRootCharacteristic().getKey()).isEqualTo("PORTABILITY");

    assertThat(requirementDto.getCharacteristic().getId()).isEqualTo(1);
    assertThat(requirementDto.getCharacteristic().getKey()).isEqualTo("COMPILER_RELATED_PORTABILITY");

    assertThat(requirementDto.getProperties()).hasSize(1);
    RequirementPropertyDto prop = requirementDto.getProperties().get(0);
    assertThat(prop.getId()).isEqualTo(1);
    assertThat(prop.getKey()).isEqualTo("remediationFactor");
    assertThat(prop.getValue()).isEqualTo(30d);
    assertThat(prop.getTextValue()).isEqualTo("mn");
  }

  @Test
  public void select_requirement_from_rule_with_two_properties(){
    setupData("select_requirement_from_rule_with_two_properties");

    RequirementDto requirementDto = dao.selectByRuleId(1L);

    assertThat(requirementDto.getProperties()).hasSize(2);
  }

  @Test
  public void return_null_on_unknown_rule(){
    setupData("select_requirement_from_rule");

    RequirementDto requirementDto = dao.selectByRuleId(999L);

    assertThat(requirementDto).isNull();
  }


}
