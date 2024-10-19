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
package org.sonar.server.util;

import org.junit.Test;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IntegerTypeValidationTest {
  private IntegerTypeValidation validation = new IntegerTypeValidation();

  @Test
  public void key() {
    assertThat(validation.key()).isEqualTo("INTEGER");
  }

  @Test
  public void not_fail_on_valid_integer() {
    validation.validate("10", null);
    validation.validate("-10", null);
  }

  @Test
  public void fail_on_string() {
    assertThatThrownBy(() -> validation.validate("abc", null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Value 'abc' must be an integer.");
  }

  @Test
  public void fail_on_float() {

    assertThatThrownBy(() -> validation.validate("10.1", null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Value '10.1' must be an integer.");
  }

}
