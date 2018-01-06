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
import org.sonar.api.PropertyType;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;

public class LongTypeValidationTest {

  LongTypeValidation underTest = new LongTypeValidation();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void key_is_long_type_name() {
    assertThat(underTest.key()).isEqualTo(PropertyType.LONG.name());
  }

  @Test
  public void do_not_fail_with_long_values() {
    underTest.validate("1984", null);
    underTest.validate("-1984", null);
  }

  @Test
  public void fail_when_float() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value '3.14' must be a long.");

    underTest.validate("3.14", null);
  }

  @Test
  public void fail_when_string() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value 'original string' must be a long.");

    underTest.validate("original string", null);
  }
}
