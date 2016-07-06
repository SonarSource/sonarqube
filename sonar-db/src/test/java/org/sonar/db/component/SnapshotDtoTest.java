/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;

public class SnapshotDtoTest {

  @Test
  public void test_getter_and_setter() throws Exception {
    SnapshotDto snapshotDto = new SnapshotDto()
      .setId(10L)
      .setBuildDate(parseDate("2014-07-02").getTime())
      .setComponentUuid("uuid_21")
      .setLast(true)
      .setVersion("1.0")
      .setPeriodMode(1, "mode1")
      .setPeriodMode(2, "mode2")
      .setPeriodMode(3, "mode3")
      .setPeriodMode(4, "mode4")
      .setPeriodMode(5, "mode5")
      .setPeriodParam(1, "param1")
      .setPeriodParam(2, "param2")
      .setPeriodParam(3, "param3")
      .setPeriodParam(4, "param4")
      .setPeriodParam(5, "param5")
      .setPeriodDate(1, parseDate("2014-06-01").getTime())
      .setPeriodDate(2, parseDate("2014-06-02").getTime())
      .setPeriodDate(3, parseDate("2014-06-03").getTime())
      .setPeriodDate(4, parseDate("2014-06-04").getTime())
      .setPeriodDate(5, parseDate("2014-06-05").getTime());

    assertThat(snapshotDto.getId()).isEqualTo(10L);
    assertThat(snapshotDto.getBuildDate()).isEqualTo(parseDate("2014-07-02").getTime());
    assertThat(snapshotDto.getComponentUuid()).isEqualTo("uuid_21");
    assertThat(snapshotDto.getLast()).isTrue();
    assertThat(snapshotDto.getVersion()).isEqualTo("1.0");
    assertThat(snapshotDto.getPeriodMode(1)).isEqualTo("mode1");
    assertThat(snapshotDto.getPeriodMode(2)).isEqualTo("mode2");
    assertThat(snapshotDto.getPeriodMode(3)).isEqualTo("mode3");
    assertThat(snapshotDto.getPeriodMode(4)).isEqualTo("mode4");
    assertThat(snapshotDto.getPeriodMode(5)).isEqualTo("mode5");
    assertThat(snapshotDto.getPeriodModeParameter(1)).isEqualTo("param1");
    assertThat(snapshotDto.getPeriodModeParameter(2)).isEqualTo("param2");
    assertThat(snapshotDto.getPeriodModeParameter(3)).isEqualTo("param3");
    assertThat(snapshotDto.getPeriodModeParameter(4)).isEqualTo("param4");
    assertThat(snapshotDto.getPeriodModeParameter(5)).isEqualTo("param5");
    assertThat(snapshotDto.getPeriodDate(1)).isEqualTo(parseDate("2014-06-01").getTime());
    assertThat(snapshotDto.getPeriodDate(2)).isEqualTo(parseDate("2014-06-02").getTime());
    assertThat(snapshotDto.getPeriodDate(3)).isEqualTo(parseDate("2014-06-03").getTime());
    assertThat(snapshotDto.getPeriodDate(4)).isEqualTo(parseDate("2014-06-04").getTime());
    assertThat(snapshotDto.getPeriodDate(5)).isEqualTo(parseDate("2014-06-05").getTime());
  }

}
