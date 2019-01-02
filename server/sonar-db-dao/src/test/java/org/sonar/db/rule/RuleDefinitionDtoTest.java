/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.rule;

import java.util.Random;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleTesting.newRule;

public class RuleDefinitionDtoTest {

  private static final Random RANDOM = new Random();

  @Test
  public void equals_is_based_on_id() {
    int id = RANDOM.nextInt(153151);
    RuleDefinitionDto dto = newRule().setId(id);

    assertThat(dto).isEqualTo(dto);
    assertThat(dto).isEqualTo(newRule().setId(id));
    assertThat(dto).isEqualTo(newRule().setRuleKey(dto.getRuleKey()).setId(id));
    assertThat(dto).isNotEqualTo(null);
    assertThat(dto).isNotEqualTo(new Object());
    assertThat(dto).isNotEqualTo(newRule().setRuleKey(dto.getRuleKey()).setId(id - 1));
    assertThat(dto).isNotEqualTo(newRule().setId(id + 1));
  }

  @Test
  public void hashcode_is_based_on_id() {
    int id = RANDOM.nextInt(153151);
    RuleDefinitionDto dto = newRule().setId(id);

    assertThat(dto.hashCode()).isEqualTo(dto.hashCode());
    assertThat(dto.hashCode()).isEqualTo(newRule().setId(id).hashCode());
    assertThat(dto.hashCode()).isEqualTo(newRule().setRuleKey(dto.getRuleKey()).setId(id).hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(null);
    assertThat(dto.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(newRule().setRuleKey(dto.getRuleKey()).setId(id - 1).hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(newRule().setId(id + 1).hashCode());
  }
}
