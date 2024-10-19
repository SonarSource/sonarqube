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
package org.sonar.api.batch.sensor.internal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

public class InMemorySensorStorageTest {

  InMemorySensorStorage underTest = new InMemorySensorStorage();

  @Test
  public void test_storeProperty() {
    assertThat(underTest.contextProperties).isEmpty();

    underTest.storeProperty("foo", "bar");
    assertThat(underTest.contextProperties).containsOnly(entry("foo", "bar"));
  }

  @Test
  public void storeProperty_throws_IAE_if_key_is_null() {
    assertThatThrownBy(() -> underTest.storeProperty(null, "bar"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Key of context property must not be null");
  }

  @Test
  public void storeProperty_throws_IAE_if_value_is_null() {
    assertThatThrownBy(() -> underTest.storeProperty("foo", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of context property must not be null");
  }
}
