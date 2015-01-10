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

package org.sonar.core.component;

import org.junit.Test;
import org.sonar.api.utils.DateUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotDtoTest {

  @Test
  public void test_getter_and_setter() throws Exception {
    SnapshotDto snapshotDto = new SnapshotDto()
      .setId(10L)
      .setParentId(2L)
      .setRootId(3L)
      .setRootProjectId(20L)
      .setBuildDate(DateUtils.parseDate("2014-07-02"))
      .setResourceId(21L)
      .setLast(true)
      .setScope("FIL")
      .setQualifier("FIL")
      .setVersion("1.0")
      .setPath("3.2.")
      .setDepth(1)
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
      .setPeriodDate(1, DateUtils.parseDate("2014-06-01"))
      .setPeriodDate(2, DateUtils.parseDate("2014-06-02"))
      .setPeriodDate(3, DateUtils.parseDate("2014-06-03"))
      .setPeriodDate(4, DateUtils.parseDate("2014-06-04"))
      .setPeriodDate(5, DateUtils.parseDate("2014-06-05"));

    assertThat(snapshotDto.getId()).isEqualTo(10L);
    assertThat(snapshotDto.getKey()).isEqualTo(10L);
    assertThat(snapshotDto.getParentId()).isEqualTo(2L);
    assertThat(snapshotDto.getRootId()).isEqualTo(3L);
    assertThat(snapshotDto.getRootProjectId()).isEqualTo(20L);
    assertThat(snapshotDto.getBuildDate()).isEqualTo(DateUtils.parseDate("2014-07-02"));
    assertThat(snapshotDto.getResourceId()).isEqualTo(21L);
    assertThat(snapshotDto.getLast()).isTrue();
    assertThat(snapshotDto.getScope()).isEqualTo("FIL");
    assertThat(snapshotDto.getQualifier()).isEqualTo("FIL");
    assertThat(snapshotDto.getVersion()).isEqualTo("1.0");
    assertThat(snapshotDto.getPath()).isEqualTo("3.2.");
    assertThat(snapshotDto.getDepth()).isEqualTo(1);
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
    assertThat(snapshotDto.getPeriodDate(1)).isEqualTo(DateUtils.parseDate("2014-06-01"));
    assertThat(snapshotDto.getPeriodDate(2)).isEqualTo(DateUtils.parseDate("2014-06-02"));
    assertThat(snapshotDto.getPeriodDate(3)).isEqualTo(DateUtils.parseDate("2014-06-03"));
    assertThat(snapshotDto.getPeriodDate(4)).isEqualTo(DateUtils.parseDate("2014-06-04"));
    assertThat(snapshotDto.getPeriodDate(5)).isEqualTo(DateUtils.parseDate("2014-06-05"));
  }

  @Test
  public void get_root_id_if_when_it_is_not_null() {
    SnapshotDto snapshot = new SnapshotDto().setRootId(123L).setId(456L);

    Long rootIdOrSelf = snapshot.getRootIdOrSelf();

    assertThat(rootIdOrSelf).isEqualTo(123L);
  }

  @Test
  public void getRootIdOrSelf_return_own_id_when_root_id_is_null() {
    SnapshotDto snapshot = new SnapshotDto().setRootId(null).setId(456L);

    Long rootIdOrSelf = snapshot.getRootIdOrSelf();

    assertThat(rootIdOrSelf).isEqualTo(456L);
  }
}
