/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.github.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTokenTest {

  @Test
  public void test_value() {
    AppToken underTest = new AppToken("foo");

    assertThat(underTest.toString())
      .isEqualTo(underTest.getValue())
      .isEqualTo("foo");

    assertThat(underTest.getAuthorizationHeaderPrefix()).isEqualTo("Bearer");
  }

  @Test
  public void test_equals_hashCode() {
    AppToken foo = new AppToken("foo");

    assertThat(foo)
      .isEqualTo(foo)
      .isEqualTo(new AppToken("foo"))
      .isNotEqualTo(new AppToken("bar"))
      .hasSameHashCodeAs(foo)
      .hasSameHashCodeAs(new AppToken("foo"));
  }
}
