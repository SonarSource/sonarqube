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
package org.sonar.server.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;

public class BooleanTypeValidationTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private BooleanTypeValidation underTest = new BooleanTypeValidation();

  @Test
  public void key() {
    assertThat(underTest.key()).isEqualTo("BOOLEAN");
  }

  @Test
  public void not_fail_on_valid_boolean() {
    underTest.validate("true", null);
    underTest.validate("True", null);
    underTest.validate("false", null);
    underTest.validate("FALSE", null);
  }

  @Test
  public void fail_on_invalid_boolean() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value 'abc' must be one of \"true\" or \"false\".");

    underTest.validate("abc", null);
  }

}
