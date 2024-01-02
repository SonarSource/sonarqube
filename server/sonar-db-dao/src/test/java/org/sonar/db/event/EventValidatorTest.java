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
package org.sonar.db.event;

import org.junit.Test;

import static com.google.common.base.Strings.repeat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EventValidatorTest {

  @Test
  public void valid_cases() {
    EventValidator.checkEventName(repeat("a", 400));
    EventValidator.checkEventName(null);
    EventValidator.checkEventCategory(repeat("a", 50));
    EventValidator.checkEventCategory(null);
    EventValidator.checkEventDescription(repeat("a", 4000));
    EventValidator.checkEventDescription(null);
  }

  @Test
  public void fail_if_name_longer_than_400() {
    assertThatThrownBy(() ->  EventValidator.checkEventName(repeat("a", 400 + 1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Event name length (401) is longer than the maximum authorized (400).");
  }

  @Test
  public void fail_if_category_longer_than_50() {
    assertThatThrownBy(() -> EventValidator.checkEventCategory(repeat("a", 50 + 1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Event category length (51) is longer than the maximum authorized (50). 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' was provided.");
  }

  @Test
  public void fail_if_description_longer_than_4000() {
    assertThatThrownBy(() ->  EventValidator.checkEventDescription(repeat("a", 4000 + 1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Event description length (4001) is longer than the maximum authorized (4000).");
  }
}
