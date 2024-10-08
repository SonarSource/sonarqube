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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateIfTest {
  private static final String STR_40_CHARS = "0123456789012345678901234567890123456789";

  @Test
  void newProperties_constructor_accepts_null_workerUuid() {
    UpdateIf.NewProperties newProperties = new UpdateIf.NewProperties(CeQueueDto.Status.PENDING, null, 123, 456);

    assertThat(newProperties.getWorkerUuid()).isNull();
  }

  @Test
  void newProperties_constructor_fails_with_NPE_if_status_is_null() {
    assertThatThrownBy(() -> new UpdateIf.NewProperties(null, "foo", 123, 456))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("status can't be null");
  }

  @Test
  void newProperties_constructor_fails_with_IAE_if_workerUuid_is_41_or_more() {
    String workerUuid = RandomStringUtils.secure().nextAlphanumeric(41);

    assertThatThrownBy(() -> new UpdateIf.NewProperties(CeQueueDto.Status.PENDING, workerUuid, 123, 456))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("worker uuid is too long: " + workerUuid);
  }

  @ParameterizedTest
  @MethodSource("workerUuidValidValues")
  void newProperties_constructor_accepts_null_empty_and_string_40_chars_or_less(String workerUuid) {
    assertThatNoException().isThrownBy(() -> new UpdateIf.NewProperties(CeQueueDto.Status.PENDING, workerUuid, 123, 345));
  }

  static Object[][] workerUuidValidValues() {
    return new Object[][]{
      {null},
      {""},
      {"bar"},
      {STR_40_CHARS}
    };
  }

  @Test
  void newProperties_constructor_IAE_if_workerUuid_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    assertThatThrownBy(() -> new UpdateIf.NewProperties(CeQueueDto.Status.PENDING, str_41_chars, 123, 345))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("worker uuid is too long: " + str_41_chars);
  }
}
