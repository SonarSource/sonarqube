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
import org.junit.Test;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class NotFoundExceptionTest {
  @Test
  public void http_code_is_404() {
    NotFoundException underTest = new NotFoundException(secure().nextAlphabetic(12));

    assertThat(underTest.httpCode()).isEqualTo(404);
  }
  @Test
  public void message_is_constructor_argument() {
    String message = secure().nextAlphabetic(12);
    NotFoundException underTest = new NotFoundException(message);

    assertThat(underTest.getMessage()).isEqualTo(message);
  }

  @Test
  public void checkFound_type_throws_NotFoundException_if_parameter_is_null() {
    String message = secure().nextAlphabetic(12);
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> checkFound(null, message))
      .withMessage(message);
  }

  @Test
  public void checkFound_type_throws_NotFoundException_if_parameter_is_null_and_formats_message() {
    String message = "foo %s";
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> checkFound(null, message, "bar"))
      .withMessage("foo bar");
  }

  @Test
  public void checkFound_return_parameter_if_parameter_is_not_null() {
    String message = secure().nextAlphabetic(12);
    Object o = new Object();

    assertThat(checkFound(o, message)).isSameAs(o);
  }

  @Test
  public void checkFoundWithOptional_throws_NotFoundException_if_empty() {
    String message = secure().nextAlphabetic(12);
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> checkFoundWithOptional(Optional.empty(), message))
      .withMessage(message);
  }

  @Test
  public void checkFoundWithOptional_throws_NotFoundException_if_empty_and_formats_message() {
    String message = "foo %s";
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> checkFoundWithOptional(Optional.empty(), message, "bar"))
      .withMessage("foo bar");
  }

  @Test
  public void checkFoundWithOptional_return_content_of_if_not_empty() {
    String message = secure().nextAlphabetic(12);
    Object o = new Object();

    assertThat(checkFoundWithOptional(Optional.of(o), message)).isSameAs(o);
  }

}
