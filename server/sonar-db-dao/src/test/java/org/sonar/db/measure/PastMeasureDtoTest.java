/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.measure;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PastMeasureDtoTest {

  @Test
  public void test_getter_and_setter() {
    PastMeasureDto dto = new PastMeasureDto()
      .setValue(1d)
      .setMetricId(2);

    assertThat(dto.hasValue()).isTrue();
    assertThat(dto.getValue()).isEqualTo(1d);
    assertThat(dto.getMetricId()).isEqualTo(2);
  }

  @Test
  public void test_has_value() {
    PastMeasureDto measureWithValue = new PastMeasureDto()
      .setValue(1d)
      .setMetricId(2);
    assertThat(measureWithValue.hasValue()).isTrue();

    PastMeasureDto measureWithoutValue = new PastMeasureDto()
      .setMetricId(2);
    assertThat(measureWithoutValue.hasValue()).isFalse();
  }

  @Test(expected = NullPointerException.class)
  public void get_value_throw_a_NPE_if_value_is_null() {
    new PastMeasureDto().getValue();
  }
}
