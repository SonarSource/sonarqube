/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CeQueueDtoTest {
  private static final String STR_15_CHARS = "012345678901234";
  private static final String STR_40_CHARS = "0123456789012345678901234567890123456789";
  private static final String STR_255_CHARS = STR_40_CHARS + STR_40_CHARS + STR_40_CHARS + STR_40_CHARS
    + STR_40_CHARS + STR_40_CHARS + STR_15_CHARS;
  private final CeQueueDto underTest = new CeQueueDto();

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
      .hasMessage("Value is too long for column CE_QUEUE.COMPONENT_UUID: " + str_41_chars);
  }

  @Test
  void setEntityUuid_accepts_null_empty_and_string_40_chars_or_less() {
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid(null));
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid(""));
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid("bar"));
    assertThatNoException().isThrownBy(() -> underTest.setEntityUuid(STR_40_CHARS));
  }

  @Test
  void setEntityUuid_throws_IAE_if_value_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    assertThatThrownBy(() -> underTest.setEntityUuid(str_41_chars))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value is too long for column CE_QUEUE.ENTITY_UUID: " + str_41_chars);
  }

  @Test
  void setTaskType_throws_NPE_if_argument_is_null() {
    assertThatThrownBy(() -> underTest.setTaskType(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void setTaskType_accepts_empty_and_string_15_chars_or_less() {
    assertThatNoException().isThrownBy(() -> underTest.setTaskType(""));
    assertThatNoException().isThrownBy(() -> underTest.setTaskType("bar"));
    assertThatNoException().isThrownBy(() -> underTest.setTaskType(STR_15_CHARS));
  }

  @Test
  void setTaskType_throws_IAE_if_value_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    assertThatThrownBy(() -> underTest.setTaskType(str_41_chars))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of task type is too long: " + str_41_chars);
  }

  @Test
  void setSubmitterLogin_accepts_null_empty_and_string_255_chars_or_less() {
    assertThatNoException().isThrownBy(() -> underTest.setSubmitterUuid(null));
    assertThatNoException().isThrownBy(() -> underTest.setSubmitterUuid(""));
    assertThatNoException().isThrownBy(() -> underTest.setSubmitterUuid("bar"));
    assertThatNoException().isThrownBy(() -> underTest.setSubmitterUuid(STR_255_CHARS));
  }

  @Test
  void setSubmitterLogin_throws_IAE_if_value_is_41_chars() {
    String str_256_chars = STR_255_CHARS + "a";

    assertThatThrownBy(() -> underTest.setSubmitterUuid(str_256_chars))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of submitter uuid is too long: " + str_256_chars);
  }
}
