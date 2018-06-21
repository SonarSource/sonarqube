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
package org.sonar.server.computation.task.projectanalysis.measure;

import org.junit.Test;

import static org.assertj.guava.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class MeasureLevelTest {
  @Test
  public void toLevel_return_absent_for_null_arg() {
    assertThat(Measure.Level.toLevel(null)).isAbsent();
  }

  @Test
  public void verify_toLevel_supports_all_Level_values() {
    for (Measure.Level level : Measure.Level.values()) {
      assertThat(Measure.Level.toLevel(level.name()).get()).isEqualTo(level);
    }
  }

  @Test
  public void toLevel_is_case_sensitive_and_returns_absent() {
    for (Measure.Level level : Measure.Level.values()) {
      assertThat(Measure.Level.toLevel(level.name().toLowerCase())).isAbsent();
    }
  }

  @Test
  public void toLevel_returns_absent_when_value_is_unknown() {
    assertThat(Measure.Level.toLevel("trololo")).isAbsent();
  }
}
