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
package org.sonar.ce.task.log;

import org.junit.Test;
import org.sonar.ce.task.log.CeTaskMessages.Message;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CeTaskMessagesMessageTest {

  @Test
  public void constructor_throws_IAE_if_text_is_null() {
    assertThatThrownBy(() -> new Message(null, 12L))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Text can't be null");
  }

  @Test
  public void constructor_throws_IAE_if_text_is_empty() {
    assertThatThrownBy(() -> new Message("", 12L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Text can't be empty");
  }

  @Test
  public void constructor_throws_IAE_if_timestamp_is_less_than_0() {
    assertThatThrownBy(() -> new Message("bar", -1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Timestamp can't be less than 0");
  }

  @Test
  public void equals_is_based_on_text_and_timestamp() {
    long timestamp = 10_000_000_000L;
    String text = secure().nextAlphabetic(23);
    Message underTest = new Message(text, timestamp);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new Message(text, timestamp))
      .isNotEqualTo(new Message(text + "ç", timestamp))
      .isNotEqualTo(new Message(text, timestamp + 10_999L))
      .isNotNull()
      .isNotEqualTo(new Object());
  }

  @Test
  public void hashsode_is_based_on_text_and_timestamp() {
    long timestamp = 10_000_000_000L;
    String text = secure().nextAlphabetic(23);
    Message underTest = new Message(text, timestamp);

    assertThat(underTest.hashCode())
      .isEqualTo(underTest.hashCode())
      .isEqualTo(new Message(text, timestamp).hashCode())
      .isNotEqualTo(new Message(text + "ç", timestamp).hashCode())
      .isNotEqualTo(new Message(text, timestamp + 10_999L).hashCode())
      .isNotEqualTo(new Object().hashCode());
  }
}
