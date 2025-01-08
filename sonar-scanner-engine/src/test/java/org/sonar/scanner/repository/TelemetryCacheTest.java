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
package org.sonar.scanner.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

class TelemetryCacheTest {

  TelemetryCache underTest = new TelemetryCache();

  @Test
  void put_EntryIsAddedToCache() {
    assertThat(underTest.getAll()).isEmpty();

    underTest.put("key", "value");
    assertThat(underTest.getAll()).containsOnly(entry("key", "value"));
  }

  @Test
  void put_whenKeyIsAlreadyThere_EntryOverridesPreviousValue() {
    underTest.put("key", "value");
    underTest.put("key", "newValue");
    assertThat(underTest.getAll()).containsOnly(entry("key", "newValue"));
  }

  @Test
  void put_whenCacheIsAlreadyFull_newEntryIsNotAdded() {
    for (int i = 0; i < 1000; i++) {
      underTest.put("key" + i, "value" + i);
    }
    underTest.put("key", "value");
    assertThat(underTest.getAll()).hasSize(1000);
    assertThat(underTest.getAll()).doesNotContain(entry("key", "value"));
  }

  @Test
  void put_whenCacheIsAlreadyFull_newEntryIsAddedIfKeyAlreadyThere() {
    for (int i = 0; i < 1000; i++) {
      underTest.put("key" + i, "value" + i);
    }
    underTest.put("key1", "newValue");
    underTest.put("key", "newValue");

    assertThat(underTest.getAll()).hasSize(1000);
    assertThat(underTest.getAll()).contains(entry("key1", "newValue"));
  }

  @Test
  void put_whenKeyIsNull_IAEIsThrown() {
    assertThatThrownBy(() -> underTest.put(null, "value"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Key of the telemetry entry must not be null");
  }

  @Test
  void put_whenValueIsNull_IAEIsThrown() {
    assertThatThrownBy(() -> underTest.put("key", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of the telemetry entry must not be null");
  }

}
