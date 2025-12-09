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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MessageTest {
  @Test
  public void create_message() {
    Message message = Message.of("key1 %s", "param1");
    assertThat(message.getMessage()).isEqualTo("key1 param1");
  }

  @Test
  public void create_message_without_params() {
    Message message = Message.of("key1");
    assertThat(message.getMessage()).isEqualTo("key1");
  }

  @Test
  public void fail_when_message_is_null() {
    assertThatThrownBy(() -> Message.of(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_message_is_empty() {
    assertThatThrownBy(() -> Message.of(""))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void test_equals_and_hashcode() {
    Message message1 = Message.of("key1%s", "param1");
    Message message2 = Message.of("key2%s", "param2");
    Message message3 = Message.of("key1");
    Message message4 = Message.of("key1%s", "param2");
    Message sameAsMessage1 = Message.of("key1%s", "param1");

    assertThat(message1)
      .isEqualTo(message1)
      .isNotEqualTo(message2)
      .isNotEqualTo(message3)
      .isNotEqualTo(message4)
      .isEqualTo(sameAsMessage1)
      .isNotNull()
      .isNotEqualTo(new Object())
      .hasSameHashCodeAs(message1)
      .hasSameHashCodeAs(sameAsMessage1);

    assertThat(message1.hashCode())
      .isNotEqualTo(message2.hashCode())
      .isNotEqualTo(message3.hashCode())
      .isNotEqualTo(message4.hashCode());
  }

  @Test
  public void to_string() {
    assertThat(Message.of("key1 %s", "param1")).hasToString("key1 param1");
    assertThat(Message.of("key1")).hasToString("key1");
    assertThat(Message.of("key1", (Object[])null)).hasToString("key1");
  }
}
