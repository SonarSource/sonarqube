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

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.ce.CeTaskMessageDto.MAX_MESSAGE_SIZE;

class CeTaskMessageDtoTest {

  private final CeTaskMessageDto underTest = new CeTaskMessageDto();

  @Test
  void setMessage_fails_with_IAE_if_argument_is_null() {
    assertThatThrownBy(() -> underTest.setMessage(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("message can't be null nor empty");
  }

  @Test
  void setMessage_fails_with_IAE_if_argument_is_empty() {
    assertThatThrownBy(() -> underTest.setMessage(""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("message can't be null nor empty");
  }

  @Test
  void setMessage_accept_argument_of_size_4000() {
    String str = repeat("a", MAX_MESSAGE_SIZE);
    underTest.setMessage(str);

    assertThat(underTest.getMessage()).isEqualTo(str);
  }

  @Test
  void setMessage_truncates_the_message_if_argument_has_size_bigger_then_4000() {
    String str = repeat("a", MAX_MESSAGE_SIZE) + "extra long tail!!!";

    underTest.setMessage(str);

    assertThat(underTest.getMessage()).isEqualTo(repeat("a", MAX_MESSAGE_SIZE - 3) + "...");
  }
}
