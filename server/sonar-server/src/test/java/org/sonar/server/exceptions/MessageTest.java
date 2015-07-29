/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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

public class MessageTest {

  @Test
  public void create_message() {
    Message message = Message.of("key1", "param1");
    assertThat(message.getKey()).isEqualTo("key1");
    assertThat(message.getParams()).containsOnly("param1");
  }

  @Test
  public void create_message_without_params() {
    Message message = Message.of("key1");
    assertThat(message.getKey()).isEqualTo("key1");
    assertThat(message.getParams()).isEmpty();
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    Message message1 = Message.of("key1", "param1");
    Message message2 = Message.of("key2", "param2");
    Message message3 = Message.of("key1");
    Message message4 = Message.of("key1", "param2");
    Message sameAsMessage1 = Message.of("key1", "param1");

    assertThat(message1).isEqualTo(message1);
    assertThat(message1).isNotEqualTo(message2);
    assertThat(message1).isNotEqualTo(message3);
    assertThat(message1).isNotEqualTo(message4);
    assertThat(message1).isEqualTo(sameAsMessage1);
    assertThat(message1).isNotEqualTo(null);
    assertThat(message1).isNotEqualTo(new Object());

    assertThat(message1.hashCode()).isEqualTo(message1.hashCode());
    assertThat(message1.hashCode()).isNotEqualTo(message2.hashCode());
    assertThat(message1.hashCode()).isNotEqualTo(message3.hashCode());
    assertThat(message1.hashCode()).isNotEqualTo(message4.hashCode());
    assertThat(message1.hashCode()).isEqualTo(sameAsMessage1.hashCode());
  }

  @Test
  public void to_string() {
    assertThat(Message.of("key1", "param1").toString()).isEqualTo("Message{key=key1, params=[param1]}");
    assertThat(Message.of("key1").toString()).isEqualTo("Message{key=key1, params=[]}");
    assertThat(Message.of("key1", null).toString()).isEqualTo("Message{key=key1, params=null}");
  }
}
