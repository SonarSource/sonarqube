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
package org.sonar.server.exceptions;

import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class BadRequestExceptionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void text_error() {
    BadRequestException exception = BadRequestException.create("error");
    assertThat(exception.getMessage()).isEqualTo("error");
  }

  @Test
  public void create_exception_from_list() {
    BadRequestException underTest = BadRequestException.create(asList("error1", "error2"));

    assertThat(underTest.errors()).containsOnly("error1", "error2");
  }

  @Test
  public void create_exception_from_var_args() {
    BadRequestException underTest = BadRequestException.create("error1", "error2");

    assertThat(underTest.errors()).containsOnly("error1", "error2");
  }

  @Test
  public void getMessage_return_first_error() {
    BadRequestException underTest = BadRequestException.create(asList("error1", "error2"));

    assertThat(underTest.getMessage()).isEqualTo("error1");
  }

  @Test
  public void fail_when_creating_exception_with_empty_list() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At least one error message is required");

    BadRequestException.create(Collections.emptyList());
  }

  @Test
  public void fail_when_creating_exception_with_one_empty_element() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Message cannot be empty");

    BadRequestException.create(asList("error", ""));
  }

  @Test
  public void fail_when_creating_exception_with_one_null_element() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Message cannot be empty");

    BadRequestException.create(asList("error", null));
  }
}
