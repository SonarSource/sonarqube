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
package org.sonar.api.impl.utils;

import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TestSystem2Test {

  private TestSystem2 underTest;

  @Before
  public void setup() {
    underTest = new TestSystem2();
  }

  @Test
  public void test_tick() {
    underTest.setNow(1000L);
    underTest.tick();
    assertThat(underTest.now()).isEqualTo(1001L);
  }

  @Test
  public void test_now() {
    underTest.setNow(1000L);
    assertThat(underTest.now()).isEqualTo(1000L);
  }

  @Test
  public void test_default_time_zone() {
    underTest.setDefaultTimeZone(TimeZone.getDefault());
    TimeZone result = underTest.getDefaultTimeZone();
    assertThat(result.getID()).isEqualTo(TimeZone.getDefault().getID());
  }

  @Test
  public void throw_ISE_if_now_equal_zero() {
    underTest.setNow(0);

    assertThatThrownBy(() -> underTest.now())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Method setNow() was not called by test");
  }

  @Test
  public void throw_ISE_if_now_lesser_than_zero() {
    underTest.setNow(-1);

    assertThatThrownBy(() -> underTest.now())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Method setNow() was not called by test");
  }

  @Test
  public void throw_ISE_if_now_equal_zero_and_try_to_tick() {
    underTest.setNow(0);

    assertThatThrownBy(() -> underTest.tick())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Method setNow() was not called by test");
  }

  @Test
  public void throw_ISE_if_now_lesser_than_zero_and_try_to_tick() {
    underTest.setNow(-1);

    assertThatThrownBy(() -> underTest.tick())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Method setNow() was not called by test");
  }
}
