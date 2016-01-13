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

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class StringListTypeValidationTest {

  StringListTypeValidation validation;

  @Before
  public void setUp() {
    validation = new StringListTypeValidation();
  }

  @Test
  public void key() {
    assertThat(validation.key()).isEqualTo("SINGLE_SELECT_LIST");
  }

  @Test
  public void not_fail_on_valid_option() {
    validation.validate("a", newArrayList("a", "b", "c"));
    validation.validate("a", null);
  }

  @Test
  public void fail_on_invalid_option() {
    try {
      validation.validate("abc", newArrayList("a", "b", "c"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      BadRequestException badRequestException = (BadRequestException) e;
      assertThat(badRequestException.firstError().getParams()[0]).isEqualTo("abc");
      assertThat(badRequestException.firstError().getParams()[1]).isEqualTo("a, b, c");
    }
  }

}
