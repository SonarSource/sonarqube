/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.base.Strings.repeat;

public class NewComponentTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fail_when_key_is_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key can't be null");

    NewComponent.create(null, "name");
  }

  @Test
  public void fail_when_key_is_longer_than_400_characters() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component key length (401) is longer than the maximum authorized (400)");

    NewComponent.create(repeat("a", 400 + 1), "name");
  }

  @Test
  public void fail_when_name_is_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name can't be null");

    NewComponent.create("key", null);
  }

  @Test
  public void fail_when_name_is_longer_than_2000_characters() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component name length (2001) is longer than the maximum authorized (2000)");

    NewComponent.create("key", repeat("a", 2001));
  }

  @Test
  public void fail_when_qualifier_is_longer_than_10_characters() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component qualifier length (11) is longer than the maximum authorized (10)");

    NewComponent.create("key", "name").setQualifier(repeat("a", 10 + 1));
  }
}
