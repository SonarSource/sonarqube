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

import com.google.common.base.Strings;
import org.junit.Test;
import org.sonar.api.rule.Severity;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureDtoTest {

  MeasureDto sut = new MeasureDto();

  @Test
  public void test_getter_and_setter() throws Exception {
    sut
      .setComponentKey("component")
      .setMetricKey("metric")
      .setId(10L)
      .setValue(2d)
      .setData("text value")
      .setVariation(1, 1d)
      .setVariation(2, 2d)
      .setVariation(3, 3d)
      .setVariation(4, 4d)
      .setVariation(5, 5d);

    assertThat(sut.getId()).isEqualTo(10);
    assertThat(sut.getValue()).isEqualTo(2d);
    assertThat(sut.getData()).isNotNull();
    assertThat(sut.getVariation(1)).isEqualTo(1d);
    assertThat(sut.getVariation(2)).isEqualTo(2d);
    assertThat(sut.getVariation(3)).isEqualTo(3d);
    assertThat(sut.getVariation(4)).isEqualTo(4d);
    assertThat(sut.getVariation(5)).isEqualTo(5d);
  }

  @Test
  public void byte_data() throws Exception {
    byte[] data = new byte[]{0x4f};
    sut.setByteData(data);
    assertThat(sut.getByteData()).isEqualTo(data);
  }

  @Test
  public void value_with_text_over_4000_characters() throws Exception {
    assertThat(sut.setData(Strings.repeat("1", 4001)).getData()).isNotNull();
  }

  @Test
  public void text_value_under_4000_characters() throws Exception {
    assertThat(sut.setData("text value").getData()).isEqualTo("text value");
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void fail_to_set_out_of_bounds_variation() throws Exception {
    sut.setVariation(6, 1d);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void fail_to_get_out_of_bounds_variation() throws Exception {
    sut.getVariation(6);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_non_existent_severity() throws Exception {
    sut.setSeverity("MAYOR");
  }

  @Test
  public void severity_values_are_retrieved() throws Exception {
    assertThat(sut.getSeverity()).isNull();

    for (String severity : Severity.ALL) {
      sut = new MeasureDto().setSeverity(severity);
      assertThat(sut.getSeverity()).isEqualTo(severity);
    }
  }
}
