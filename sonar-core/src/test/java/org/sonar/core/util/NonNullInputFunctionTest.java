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
package org.sonar.core.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class NonNullInputFunctionTest {

  NonNullInputFunction<String, Integer> underTest = new TestFunction();

  @Test
  public void fail_if_null_input() {
    try {
      underTest.apply(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Null inputs are not allowed in this function");
    }
  }

  @Test
  public void apply() {
    assertThat(underTest.apply("foo")).isEqualTo(3);
  }

  private static class TestFunction extends NonNullInputFunction<String, Integer> {
    @Override
    protected Integer doApply(String input) {
      return input.length();
    }
  }
}
