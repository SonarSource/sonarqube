/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerExceptionTest {

  @Test
  void constructor_setsHttpCodeAndMessage() {
    ServerException ex = new ServerException(500, "Internal error");

    assertThat(ex.httpCode()).isEqualTo(500);
    assertThat(ex.getMessage()).isEqualTo("Internal error");
  }

  @Test
  void constructor_withNullMessage_throwsNullPointerException() {
    assertThatThrownBy(() -> new ServerException(400, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Error message cannot be null");
  }

  @Test
  void httpCode_returnsCorrectCode() {
    ServerException ex = new ServerException(404, "Not found");

    assertThat(ex.httpCode()).isEqualTo(404);
  }
}
