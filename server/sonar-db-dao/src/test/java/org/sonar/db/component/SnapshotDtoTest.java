/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.component;


import org.junit.jupiter.api.Test;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.utils.DateUtils.parseDate;

class SnapshotDtoTest {


  @Test
  void test_getter_and_setter() {
    SnapshotDto snapshotDto = create();

    assertThat(snapshotDto.getAnalysisDate()).isEqualTo(parseDate("2014-07-02").getTime());
    assertThat(snapshotDto.getRootComponentUuid()).isEqualTo("uuid_21");
    assertThat(snapshotDto.getLast()).isTrue();
    assertThat(snapshotDto.getProjectVersion()).isEqualTo("1.0");
    assertThat(snapshotDto.getBuildString()).isEqualTo("1.0.1.123");
    assertThat(snapshotDto.getPeriodMode()).isEqualTo("mode1");
    assertThat(snapshotDto.getPeriodModeParameter()).isEqualTo("param1");
    assertThat(snapshotDto.getPeriodDate()).isEqualTo(parseDate("2014-06-01").getTime());
  }

  @Test
  void fail_if_projectVersion_is_longer_then_100_characters() {
    SnapshotDto snapshotDto = new SnapshotDto();
    snapshotDto.setProjectVersion(null);
    snapshotDto.setProjectVersion("1.0");
    snapshotDto.setProjectVersion(repeat("a", 100));

    assertThatThrownBy(() -> snapshotDto.setProjectVersion(repeat("a", 101)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("projectVersion" +
        " length (101) is longer than the maximum authorized (100). " +
        "'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' was provided.");
  }

  @Test
  void fail_if_buildString_is_longer_then_100_characters() {
    SnapshotDto snapshotDto = new SnapshotDto();
    snapshotDto.setBuildString(null);
    snapshotDto.setBuildString("1.0");
    snapshotDto.setBuildString(repeat("a", 100));

    assertThatThrownBy(() -> snapshotDto.setBuildString(repeat("a", 101)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("buildString" +
        " length (101) is longer than the maximum authorized (100). " +
        "'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' was provided.");
  }

  @Test
  void equals_whenSameObject_shouldReturnTrue() {
    SnapshotDto snapshotDto = create();
    assertThat(snapshotDto.equals(snapshotDto)).isTrue();
  }

  @Test
  void equals_whenComparedToNull_shouldReturnFalse() {
    SnapshotDto snapshotDto = create();
    assertThat(snapshotDto.equals(null)).isFalse();
  }

  @Test
  void equals_whenComparedToDifferentClass_shouldReturnFalse() {
    SnapshotDto snapshotDto = create();
    Object differentObject = new Object();
    assertThat(snapshotDto.equals(differentObject)).isFalse();
  }

  @Test
  void equals_whenComparedToDifferentInstanceWithSameValues_shouldReturnTrue() {
    SnapshotDto snapshotDto1 = create();
    SnapshotDto snapshotDto2 = create();
    assertThat(snapshotDto1.equals(snapshotDto2)).isTrue();
    assertThat(snapshotDto2.equals(snapshotDto1)).isTrue();
  }

  @Test
  void equals_whenComparedToDifferentInstanceWithDifferentValues_shouldReturnFalse() {
    SnapshotDto snapshotDto1 = create();
    SnapshotDto snapshotDto2 = create().setBuildString("some-other-string");
    assertThat(snapshotDto1.equals(snapshotDto2)).isFalse();
    assertThat(snapshotDto2.equals(snapshotDto1)).isFalse();
  }

  @Test
  void hashcode_whenDifferentInstanceWithSameValues_shouldBeEqual() {
    SnapshotDto snapshotDto1 = create();
    SnapshotDto snapshotDto2 = create();
    assertThat(snapshotDto1).hasSameHashCodeAs(snapshotDto2);
  }

  @Test
  void hashcode_whenDifferentInstanceWithDifferentValues_shouldNotBeEqual() {
    SnapshotDto snapshotDto1 = create();
    SnapshotDto snapshotDto2 = create().setBuildString("some-other-string");
    assertThat(snapshotDto1).doesNotHaveSameHashCodeAs(snapshotDto2);
  }

  private SnapshotDto create() {
    return new SnapshotDto()
      .setAnalysisDate(parseDate("2014-07-02").getTime())
      .setRootComponentUuid("uuid_21")
      .setLast(true)
      .setProjectVersion("1.0")
      .setBuildString("1.0.1.123")
      .setPeriodMode("mode1")
      .setPeriodParam("param1")
      .setPeriodDate(parseDate("2014-06-01").getTime());
  }

}
