/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ws;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class RequestTest {
  @Test
  public void string_params() {
    Request request = new Request(ImmutableMap.of("foo", "bar"));
    assertThat(request.param("none")).isNull();
    assertThat(request.param("foo")).isEqualTo("bar");
  }

  @Test
  public void int_params() {
    Request request = new Request(ImmutableMap.of("foo", "123"));
    assertThat(request.intParam("none")).isNull();
    assertThat(request.intParam("foo")).isEqualTo(123);

    assertThat(request.intParam("none", 456)).isEqualTo(456);
    assertThat(request.intParam("foo", 456)).isEqualTo(123);
  }
}
