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

package org.sonar.core.measure.db;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class MeasureDtoTest {

  @Test
  public void test_getter_and_setter() throws Exception {
    MeasureDto measureDto = MeasureDto.createFor(MeasureKey.of("component", "metric"))
      .setId(10L)
      .setValue(2d)
      .setTextValue("text value")
      .setData(new byte[]{})
      .setVariation(1, 1d)
      .setVariation(2, 2d)
      .setVariation(3, 3d)
      .setVariation(4, 4d)
      .setVariation(5, 5d);

    assertThat(measureDto.getId()).isEqualTo(10);
    assertThat(measureDto.getValue()).isEqualTo(2d);
    assertThat(measureDto.getData()).isNotNull();
    assertThat(measureDto.getVariation(1)).isEqualTo(1d);
    assertThat(measureDto.getVariation(2)).isEqualTo(2d);
    assertThat(measureDto.getVariation(3)).isEqualTo(3d);
    assertThat(measureDto.getVariation(4)).isEqualTo(4d);
    assertThat(measureDto.getVariation(5)).isEqualTo(5d);
  }

  @Test
  public void test_data() throws Exception {
    assertThat(MeasureDto.createFor(MeasureKey.of("component", "metric"))
      .setTextValue("text value")
      .setData(null).getData()).isEqualTo("text value");

    assertThat(MeasureDto.createFor(MeasureKey.of("component", "metric"))
      .setTextValue(null)
      .setData(new byte[]{}).getData()).isNotNull();
  }

  @Test
  public void fail_to_set_out_of_bounds_variation() throws Exception {
    try {
      MeasureDto.createFor(MeasureKey.of("component", "metric"))
        .setVariation(6, 1d);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
    }
  }

  @Test
  public void fail_to_get_out_of_bounds_variation() throws Exception {
    try {
      MeasureDto.createFor(MeasureKey.of("component", "metric"))
        .getVariation(6);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
    }
  }
}
