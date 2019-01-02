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
package org.sonar.db.event;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.base.Strings.repeat;

public class EventValidatorTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Event name length (401) is longer than the maximum authorized (400).");

    EventValidator.checkEventName(repeat("a", 400 + 1));
  }

  @Test
  public void fail_if_category_longer_than_50() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Event category length (51) is longer than the maximum authorized (50). 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' was provided.");

    EventValidator.checkEventCategory(repeat("a", 50 + 1));
  }

  @Test
  public void fail_if_description_longer_than_4000() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Event description length (4001) is longer than the maximum authorized (4000).");

    EventValidator.checkEventDescription(repeat("a", 4000 + 1));
  }
}
