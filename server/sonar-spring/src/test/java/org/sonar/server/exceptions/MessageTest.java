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

class MessageTest {

  @Test
  void of_createsMessageWithText() {
    Message message = Message.of("Test message");

    assertThat(message.getMessage()).isEqualTo("Test message");
  }

  @Test
  void of_withFormatArguments_createsFormattedMessage() {
    Message message = Message.of("Error: %s at line %d", "syntax error", 42);

    assertThat(message.getMessage()).isEqualTo("Error: syntax error at line 42");
  }

  @Test
  void of_withEmptyMessage_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> Message.of(""))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void of_withNullMessage_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> Message.of(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void equals_withSameMessage_returnsTrue() {
    Message message1 = Message.of("Test");
    Message message2 = Message.of("Test");

    assertThat(message1).isEqualTo(message2);
  }

  @Test
  void equals_withDifferentMessage_returnsFalse() {
    Message message1 = Message.of("Test1");
    Message message2 = Message.of("Test2");

    assertThat(message1).isNotEqualTo(message2);
  }

  @Test
  void equals_withSameInstance_returnsTrue() {
    Message message = Message.of("Test");

    assertThat(message).isEqualTo(message);
  }

  @Test
  void equals_withNull_returnsFalse() {
    Message message = Message.of("Test");

    assertThat(message).isNotEqualTo(null);
  }

  @Test
  void equals_withDifferentClass_returnsFalse() {
    Message message = Message.of("Test");

    assertThat(message).isNotEqualTo("Test");
  }

  @Test
  void hashCode_withSameMessage_returnsSameHashCode() {
    Message message1 = Message.of("Test");
    Message message2 = Message.of("Test");

    assertThat(message1.hashCode()).isEqualTo(message2.hashCode());
  }

  @Test
  void toString_returnsMessage() {
    Message message = Message.of("Test message");

    assertThat(message.toString()).isEqualTo("Test message");
  }
}
