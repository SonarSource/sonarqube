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

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotFoundExceptionTest {

  @Test
  void constructor_setsMessageAndHttpCode() {
    NotFoundException ex = new NotFoundException("Resource not found");

    assertThat(ex.httpCode()).isEqualTo(404);
    assertThat(ex.getMessage()).isEqualTo("Resource not found");
  }

  @Test
  void checkFound_whenValueNotNull_returnsValue() {
    String value = "test";

    String result = NotFoundException.checkFound(value, "Not found");

    assertThat(result).isEqualTo("test");
  }

  @Test
  void checkFound_whenValueNull_throwsNotFoundException() {
    assertThatThrownBy(() -> NotFoundException.checkFound(null, "Resource %s not found", "ABC"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Resource ABC not found");
  }

  @Test
  void checkFoundWithOptional_whenValuePresent_returnsValue() {
    Optional<String> value = Optional.of("test");

    String result = NotFoundException.checkFoundWithOptional(value, "Not found");

    assertThat(result).isEqualTo("test");
  }

  @Test
  void checkFoundWithOptional_whenValueEmpty_throwsNotFoundException() {
    Optional<String> value = Optional.empty();

    assertThatThrownBy(() -> NotFoundException.checkFoundWithOptional(value, "Resource %s not found", "XYZ"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Resource XYZ not found");
  }
}
