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
package org.sonar.scanner.repository;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ContextPropertiesCacheTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ContextPropertiesCache underTest = new ContextPropertiesCache();

  @Test
  public void put_property() {
    assertThat(underTest.getAll()).isEmpty();

    underTest.put("foo", "bar");
    assertThat(underTest.getAll()).containsOnly(entry("foo", "bar"));
  }

  @Test
  public void put_overrides_existing_value() {
    underTest.put("foo", "bar");
    underTest.put("foo", "baz");
    assertThat(underTest.getAll()).containsOnly(entry("foo", "baz"));
  }

  @Test
  public void put_throws_IAE_if_key_is_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key of context property must not be null");

    underTest.put(null, "bar");
  }

  @Test
  public void put_throws_IAE_if_value_is_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of context property must not be null");

    underTest.put("foo", null);
  }
}
