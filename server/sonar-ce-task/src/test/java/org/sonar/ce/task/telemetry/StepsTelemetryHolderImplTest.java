/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.telemetry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepsTelemetryHolderImplTest {

  private final StepsTelemetryHolderImpl stepsTelemetryHolderImpl = new StepsTelemetryHolderImpl();

  @Test
  void getTelemetryMetrics_shouldReturnEmptyMap() {
    assertThat(stepsTelemetryHolderImpl.getTelemetryMetrics()).isEmpty();
  }

  @Test
  void add_shouldAddValue() {
    stepsTelemetryHolderImpl.add("key", "value");
    assertThat(stepsTelemetryHolderImpl.getTelemetryMetrics()).containsEntry("key", "value");
  }

  @Test
  void add_shouldThrowNPEIfKeyIsNull() {
    assertThatThrownBy(() -> stepsTelemetryHolderImpl.add(null, "value"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Metric has null key");
  }

  @Test
  void add_shouldThrowNPEIfValueIsNull() {
    assertThatThrownBy(() -> stepsTelemetryHolderImpl.add("key", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Metric with key [key] has null value");
  }

  @Test
  void add_shouldThrowIAEIfKeyIsAlreadyPresent() {
    stepsTelemetryHolderImpl.add("key", "value");
    assertThatThrownBy(() -> stepsTelemetryHolderImpl.add("key", "value"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric with key [key] is already present");
  }
}
