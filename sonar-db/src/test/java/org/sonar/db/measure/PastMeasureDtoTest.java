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

package org.sonar.db.measure;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PastMeasureDtoTest {

  @Test
  public void test_getter_and_setter() throws Exception {
    PastMeasureDto dto = new PastMeasureDto()
      .setId(10L)
      .setValue(1d)
      .setMetricId(2)
      .setRuleId(3)
      .setCharacteristicId(4)
      .setPersonId(5);

    assertThat(dto.getId()).isEqualTo(10L);
    assertThat(dto.hasValue()).isTrue();
    assertThat(dto.getValue()).isEqualTo(1d);
    assertThat(dto.getMetricId()).isEqualTo(2);
    assertThat(dto.getRuleId()).isEqualTo(3);
    assertThat(dto.getCharacteristicId()).isEqualTo(4);
    assertThat(dto.getPersonId()).isEqualTo(5);
  }

  @Test
  public void test_has_value() throws Exception {
    PastMeasureDto measureWithValue = new PastMeasureDto()
      .setId(10L)
      .setValue(1d)
      .setMetricId(2)
      .setRuleId(3)
      .setCharacteristicId(4)
      .setPersonId(5);
    assertThat(measureWithValue.hasValue()).isTrue();

    PastMeasureDto measureWithoutValue = new PastMeasureDto()
      .setId(10L)
      .setMetricId(2)
      .setRuleId(3)
      .setCharacteristicId(4)
      .setPersonId(5);
    assertThat(measureWithoutValue.hasValue()).isFalse();
  }

  @Test(expected = NullPointerException.class)
  public void get_value_throw_a_NPE_if_value_is_null() {
    new PastMeasureDto().getValue();
  }
}
