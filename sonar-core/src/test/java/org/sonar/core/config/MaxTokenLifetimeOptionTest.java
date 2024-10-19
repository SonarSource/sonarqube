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
package org.sonar.core.config;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sonar.core.config.MaxTokenLifetimeOption.NINETY_DAYS;
import static org.sonar.core.config.MaxTokenLifetimeOption.NO_EXPIRATION;
import static org.sonar.core.config.MaxTokenLifetimeOption.ONE_YEAR;
import static org.sonar.core.config.MaxTokenLifetimeOption.THIRTY_DAYS;

public class MaxTokenLifetimeOptionTest {

  @Test
  public void all_options_present() {
    assertThat(MaxTokenLifetimeOption.values()).hasSize(4);
  }

  @Test
  public void when_get_by_name_then_the_enum_value_is_returned() {
    assertThat(MaxTokenLifetimeOption.get("30 days")).isEqualTo(THIRTY_DAYS);
    assertThat(MaxTokenLifetimeOption.get("90 days")).isEqualTo(NINETY_DAYS);
    assertThat(MaxTokenLifetimeOption.get("1 year")).isEqualTo(ONE_YEAR);
    assertThat(MaxTokenLifetimeOption.get("No expiration")).isEqualTo(NO_EXPIRATION);
  }

  @Test
  public void when_get_by_name_nonexistant_then_exception_is_thrown() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> MaxTokenLifetimeOption.get("wrong lifetime"))
      .withMessage("No token expiration interval with name \"wrong lifetime\" found.");
  }

  @Test
  public void lifetime_options_days() {
    assertThat(THIRTY_DAYS.getDays()).hasValue(30);
    assertThat(NINETY_DAYS.getDays()).hasValue(90);
    assertThat(ONE_YEAR.getDays()).hasValue(365);
    assertThat(NO_EXPIRATION.getDays()).isEmpty();
  }
}
