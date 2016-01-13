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
package org.sonar.server.util;

import org.junit.Before;
import org.junit.Test;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class IntegerTypeValidationTest {

  IntegerTypeValidation validation;

  @Before
  public void setUp() {
    validation = new IntegerTypeValidation();
  }

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
    try {
      validation.validate("abc", null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      BadRequestException badRequestException = (BadRequestException) e;
      assertThat(badRequestException.firstError().getParams()[0]).isEqualTo("abc");
    }
  }

  @Test
  public void fail_on_float() {
    try {
      validation.validate("10.1", null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      BadRequestException badRequestException = (BadRequestException) e;
      assertThat(badRequestException.firstError().getParams()[0]).isEqualTo("10.1");
    }
  }

}
