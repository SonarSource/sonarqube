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
package org.sonar.db.ce;

import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CeActivityDtoTest {
  private static final String STR_40_CHARS = "0123456789012345678901234567890123456789";
  private static final String STR_100_CHARS = secure().nextAlphabetic(100);
  private final CeActivityDto underTest = new CeActivityDto();

  @Test
  void constructor_from_CeQueueDto_populates_fields() {
    long now = Instant.now().toEpochMilli();
    CeQueueDto ceQueueDto = new CeQueueDto()
      .setUuid(secure().nextAlphanumeric(10))
      .setTaskType(secure().nextAlphanumeric(11))
      .setComponentUuid(secure().nextAlphanumeric(12))
      .setEntityUuid(secure().nextAlphanumeric(13))
      .setSubmitterUuid(secure().nextAlphanumeric(14))
      .setWorkerUuid(secure().nextAlphanumeric(15))
      .setCreatedAt(now + 9_999)
      .setStartedAt(now + 865);

    CeActivityDto underTest = new CeActivityDto(ceQueueDto);

    assertThat(underTest.getUuid()).isEqualTo(ceQueueDto.getUuid());
    assertThat(underTest.getTaskType()).isEqualTo(ceQueueDto.getTaskType());
    assertThat(underTest.getComponentUuid()).isEqualTo(ceQueueDto.getComponentUuid());
    assertThat(underTest.getEntityUuid()).isEqualTo(ceQueueDto.getEntityUuid());
    assertThat(underTest.getIsLastKey()).isEqualTo(ceQueueDto.getTaskType() + ceQueueDto.getComponentUuid());
    assertThat(underTest.getIsLast()).isFalse();
    assertThat(underTest.getMainIsLastKey()).isEqualTo(ceQueueDto.getTaskType() + ceQueueDto.getEntityUuid());
    assertThat(underTest.getMainIsLast()).isFalse();
    assertThat(underTest.getSubmitterUuid()).isEqualTo(ceQueueDto.getSubmitterUuid());
    assertThat(underTest.getWorkerUuid()).isEqualTo(ceQueueDto.getWorkerUuid());
    assertThat(underTest.getSubmittedAt()).isEqualTo(ceQueueDto.getCreatedAt());
    assertThat(underTest.getStartedAt()).isEqualTo(ceQueueDto.getStartedAt());
    assertThat(underTest.getStatus()).isNull();
  }

  @Test
  void setComponentUuid_accepts_null_empty_and_string_40_chars_or_less() {
    assertThatNoException().isThrownBy(() -> underTest.setComponentUuid(null));
    assertThatNoException().isThrownBy(() -> underTest.setComponentUuid(""));
    assertThatNoException().isThrownBy(() -> underTest.setComponentUuid("bar"));
    assertThatNoException().isThrownBy(() -> underTest.setComponentUuid(STR_40_CHARS));
  }

  @Test
  void setComponentUuid_throws_IAE_if_value_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    assertThatThrownBy(() -> underTest.setComponentUuid(str_41_chars))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value is too long for column CE_ACTIVITY.COMPONENT_UUID: " + str_41_chars);
  }

  @Test
  void setMainComponentUuid_accepts_null_empty_and_string_40_chars_or_less() {
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid(null));
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid(""));
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid("bar"));
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid(STR_40_CHARS));
  }

  @Test
  void seEntityUuid_throws_IAE_if_value_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    assertThatThrownBy(() -> underTest.setEntityUuid(str_41_chars))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value is too long for column CE_ACTIVITY.ENTITY_UUID: " + str_41_chars);
  }

  @Test
  void setNodeName_accepts_null_empty_and_string_100_chars_or_less() {
    underTest.setNodeName(null);
    underTest.setNodeName("");
    underTest.setNodeName("bar");
    underTest.setNodeName(STR_100_CHARS);
    assertThat(underTest.getNodeName()).isEqualTo(STR_100_CHARS);
  }

  @Test
  void setNodeName_ifMoreThan100chars_truncates() {
    underTest.setNodeName(STR_100_CHARS + "This should be truncated");
    assertThat(underTest.getNodeName()).isEqualTo(STR_100_CHARS);
  }

  @ParameterizedTest
  @MethodSource("stringsWithChar0")
  void setStacktrace_filters_out_char_zero(String withChar0, String expected) {
    underTest.setErrorStacktrace(withChar0);

    assertThat(underTest.getErrorStacktrace()).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("stringsWithChar0")
  void setErrorMessage_filters_out_char_zero(String withChar0, String expected) {
    underTest.setErrorMessage(withChar0);

    assertThat(underTest.getErrorMessage()).isEqualTo(expected);
  }

  @Test
  void setErrorMessage_truncates_to_1000_after_removing_char_zero() {
    String before = secure().nextAlphanumeric(50);
    String after = secure().nextAlphanumeric(950);
    String truncated = secure().nextAlphanumeric(1 + new Random().nextInt(50));
    underTest.setErrorMessage(before + "\u0000" + after + truncated);

    assertThat(underTest.getErrorMessage()).isEqualTo(before + after);
  }

  static Object[][] stringsWithChar0() {
    return new Object[][]{
      {"\u0000", ""},
      {"\u0000\u0000\u0000\u0000", ""},
      {"\u0000 \u0000a\u0000b\u0000   ", " ab   "},
      {"a \u0000 0message", "a  0message"}
    };
  }
}
