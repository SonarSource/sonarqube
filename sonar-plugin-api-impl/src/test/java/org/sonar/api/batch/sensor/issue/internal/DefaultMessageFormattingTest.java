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
package org.sonar.api.batch.sensor.issue.internal;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.api.batch.sensor.issue.MessageFormatting;

public class DefaultMessageFormattingTest {

  @Test
  public void negative_start_should_throw_exception() {
    DefaultMessageFormatting format = new DefaultMessageFormatting().start(-1).end(1).type(MessageFormatting.Type.CODE);
    Assertions.assertThatThrownBy(() -> format.validate("message"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Message formatting start must be greater or equals to 0");
  }

  @Test
  public void missing_type_should_throw_exception() {
    DefaultMessageFormatting format = new DefaultMessageFormatting().start(0).end(1);
    Assertions.assertThatThrownBy(() -> format.validate("message"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Message formatting type can't be null");
  }

  @Test
  public void end_lesser_than_start_should_throw_exception() {
    DefaultMessageFormatting format = new DefaultMessageFormatting().start(3).end(2).type(MessageFormatting.Type.CODE);
    Assertions.assertThatThrownBy(() -> format.validate("message"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Message formatting end must be greater than start");
  }

  @Test
  public void end_greater_or_equals_to_message_size_throw_exception() {
    DefaultMessageFormatting format = new DefaultMessageFormatting().start(0).end(8).type(MessageFormatting.Type.CODE);
    Assertions.assertThatThrownBy(() -> format.validate("message"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Message formatting end must be lesser or equal than message size");
  }

  @Test
  public void full_range_on_message_should_work() {
    DefaultMessageFormatting format = new DefaultMessageFormatting().start(0).end(6).type(MessageFormatting.Type.CODE);
    Assertions.assertThatCode(() -> format.validate("message")).doesNotThrowAnyException();
  }
}
