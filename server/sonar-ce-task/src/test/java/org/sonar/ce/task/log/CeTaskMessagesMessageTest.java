/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Random;
import java.util.stream.LongStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.log.CeTaskMessages.Message;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskMessagesMessageTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_throws_IAE_if_text_is_null() {
    expectTextCantBeNullNorEmptyIAE();

    new Message(null, 12L);
  }

  @Test
  public void constructor_throws_IAE_if_text_is_empty() {
    expectTextCantBeNullNorEmptyIAE();

    new Message("", 12L);
  }

  private void expectTextCantBeNullNorEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Text can't be null nor empty");
  }

  @Test
  public void constructor_throws_IAE_if_timestamp_is_less_than_0() {
    LongStream.of(0, 1 + new Random().nextInt(12))
      .forEach(timestamp -> assertThat(new Message("foo", timestamp).getTimestamp()).isEqualTo(timestamp));

    long lessThanZero = -1 - new Random().nextInt(33);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Text can't be less than 0");

    new Message("bar", lessThanZero);
  }

  @Test
  public void equals_is_based_on_text_and_timestamp() {
    long timestamp = new Random().nextInt(10_999);
    String text = randomAlphabetic(23);
    Message underTest = new Message(text, timestamp);

    assertThat(underTest).isEqualTo(underTest);
    assertThat(underTest).isEqualTo(new Message(text, timestamp));
    assertThat(underTest).isNotEqualTo(new Message(text + "ç", timestamp));
    assertThat(underTest).isNotEqualTo(new Message(text, timestamp + 10_999L));
    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Object());
  }

  @Test
  public void hashsode_is_based_on_text_and_timestamp() {
    long timestamp = new Random().nextInt(10_999);
    String text = randomAlphabetic(23);
    Message underTest = new Message(text, timestamp);

    assertThat(underTest.hashCode()).isEqualTo(underTest.hashCode());
    assertThat(underTest.hashCode()).isEqualTo(new Message(text, timestamp).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Message(text + "ç", timestamp).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Message(text, timestamp + 10_999L).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode());
  }
}
