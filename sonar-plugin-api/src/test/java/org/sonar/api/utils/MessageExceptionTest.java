/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageExceptionTest {

  @Test
  public void should_create_exception() {
    String message = "the message";
    MessageException exception = MessageException.of(message);
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void should_create_exception_with_status_and_l10n_message_with_param(){
    MessageException exception = MessageException.ofL10n("key", new String[]{"value"});
    assertThat(exception.l10nKey()).isEqualTo("key");
    assertThat(exception.l10nParams()).containsOnly("value");
  }

  @Test
  public void should_create_exception_with_status_and_l10n_message_without_param(){
    MessageException exception = MessageException.ofL10n("key", null);
    assertThat(exception.l10nKey()).isEqualTo("key");
    assertThat(exception.l10nParams()).isEmpty();
  }
}
