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

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BadRequestExceptionTest {

  @Test
  void create_withSingleMessage_returnsException() {
    BadRequestException ex = BadRequestException.create("Error message");

    assertThat(ex.httpCode()).isEqualTo(400);
    assertThat(ex.getMessage()).isEqualTo("Error message");
    assertThat(ex.errors()).containsExactly("Error message");
    assertThat(ex.getRelatedField()).isEmpty();
  }

  @Test
  void create_withMultipleMessages_returnsException() {
    BadRequestException ex = BadRequestException.create("Error 1", "Error 2", "Error 3");

    assertThat(ex.httpCode()).isEqualTo(400);
    assertThat(ex.getMessage()).isEqualTo("Error 1");
    assertThat(ex.errors()).containsExactly("Error 1", "Error 2", "Error 3");
  }

  @Test
  void create_withList_returnsException() {
    BadRequestException ex = BadRequestException.create(List.of("Error A", "Error B"));

    assertThat(ex.httpCode()).isEqualTo(400);
    assertThat(ex.getMessage()).isEqualTo("Error A");
    assertThat(ex.errors()).containsExactly("Error A", "Error B");
  }

  @Test
  void create_withEmptyList_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> BadRequestException.create(List.of()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("At least one error message is required");
  }

  @Test
  void create_withNullMessage_throwsIllegalArgumentException() {
    List<String> messages = java.util.Arrays.asList("Valid", null);
    assertThatThrownBy(() -> BadRequestException.create(messages))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Message cannot be empty");
  }

  @Test
  void create_withEmptyMessage_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> BadRequestException.create(List.of("Valid", "")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Message cannot be empty");
  }

  @Test
  void createWithRelatedField_returnsExceptionWithField() {
    BadRequestException ex = BadRequestException.createWithRelatedField("Invalid value", "fieldName");

    assertThat(ex.httpCode()).isEqualTo(400);
    assertThat(ex.getMessage()).isEqualTo("Invalid value");
    assertThat(ex.errors()).containsExactly("Invalid value");
    assertThat(ex.getRelatedField()).hasValue("fieldName");
  }

  @Test
  void createWithRelatedField_withEmptyMessage_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> BadRequestException.createWithRelatedField("", "field"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Message cannot be empty");
  }

  @Test
  void createWithRelatedField_withEmptyField_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> BadRequestException.createWithRelatedField("message", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Related field cannot be empty");
  }

  @Test
  void checkRequest_whenExpressionTrue_doesNotThrow() {
    BadRequestException.checkRequest(true, "Should not throw");
  }

  @Test
  void checkRequest_whenExpressionFalse_throwsBadRequestException() {
    assertThatThrownBy(() -> BadRequestException.checkRequest(false, "Error: %s", "details"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Error: details");
  }

  @Test
  void checkRequest_withList_whenExpressionFalse_throwsBadRequestException() {
    assertThatThrownBy(() -> BadRequestException.checkRequest(false, List.of("Error 1", "Error 2")))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Error 1");
  }

  @Test
  void checkRequest_withList_whenExpressionTrue_doesNotThrow() {
    BadRequestException.checkRequest(true, List.of("Should not throw"));
  }

  @Test
  void throwBadRequestException_throwsWithFormattedMessage() {
    assertThatThrownBy(() -> BadRequestException.throwBadRequestException("Error: %s %d", "test", 123))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Error: test 123");
  }

  @Test
  void toString_returnsFormattedString() {
    BadRequestException ex = BadRequestException.create("Error message");

    String result = ex.toString();

    assertThat(result).contains("errors=[Error message]");
  }

  @Test
  void toString_withRelatedField_includesField() {
    BadRequestException ex = BadRequestException.createWithRelatedField("Error", "field");

    String result = ex.toString();

    assertThat(result).contains("errors=[Error]");
    assertThat(result).contains("relatedField=field");
  }
}
