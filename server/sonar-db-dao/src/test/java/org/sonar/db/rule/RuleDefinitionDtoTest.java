/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import org.junit.Test;
import org.sonar.core.util.Uuids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleTesting.newRule;

public class RuleDefinitionDtoTest {

  @Test
  public void equals_is_based_on_uuid() {
    String uuid = Uuids.createFast();
    RuleDefinitionDto dto = newRule().setUuid(uuid);

    assertThat(dto).isEqualTo(dto);
    assertThat(dto).isEqualTo(newRule().setUuid(uuid));
    assertThat(dto).isEqualTo(newRule().setRuleKey(dto.getRuleKey()).setUuid(uuid));
    assertThat(dto).isNotEqualTo(null);
    assertThat(dto).isNotEqualTo(new Object());
    assertThat(dto).isNotEqualTo(newRule().setRuleKey(dto.getRuleKey()).setUuid(Uuids.createFast()));
    assertThat(dto).isNotEqualTo(newRule().setUuid(Uuids.createFast()));
  }

  @Test
  public void hashcode_is_based_on_uuid() {
    String uuid = Uuids.createFast();
    RuleDefinitionDto dto = newRule().setUuid(uuid);

    assertThat(dto.hashCode()).isEqualTo(dto.hashCode());
    assertThat(dto.hashCode()).isEqualTo(newRule().setUuid(uuid).hashCode());
    assertThat(dto.hashCode()).isEqualTo(newRule().setRuleKey(dto.getRuleKey()).setUuid(uuid).hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(null);
    assertThat(dto.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(newRule().setRuleKey(dto.getRuleKey()).setUuid(Uuids.createFast()).hashCode());
    assertThat(dto.hashCode()).isNotEqualTo(newRule().setUuid(Uuids.createFast()).hashCode());
  }
}
